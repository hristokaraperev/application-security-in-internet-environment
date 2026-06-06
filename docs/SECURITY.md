# Сигурност — заплахи и приложени мерки

Документът описва заплахите, които системата взема предвид, и конкретните мерки за
всяка от тях, с препратки към кода. Архитектурата е микросървизна: три независими
услуги със собствени бази, JWT автентикация и service-to-service автентикация.

## Модел на доверие (накратко)

- **Gateway** (`:8080`) е единствената публична входна точка. Сервира frontend-а и
  рутира `/auth/**` и `/api/**`.
- **Auth service** (`:8081`) издава и управлява JWT (RS256). Държи частния ключ.
- **Wallet service** (`:8082`) е resource server — приема само валидни токени и само
  заявки, дошли през Gateway.
- Всяка услуга има **собствена PostgreSQL база** (децентрализирани данни).

---

## Заплахи → мерки

| # | Заплаха | Мярка | Къде в кода |
|---|---------|-------|-------------|
| 1 | **Brute force** на пароли | Account lockout след 5 неуспешни опита (15 мин) + bcrypt (cost 12, бавен хеш) + edge rate limiting | [`AuthService`](../auth-service/src/main/java/com/example/wallet/auth/service/AuthService.java), [`SecurityConfig`](../auth-service/src/main/java/com/example/wallet/auth/config/SecurityConfig.java), [`RateLimitFilter`](../gateway-service/src/main/java/com/example/wallet/gateway/RateLimitFilter.java) |
| 2 | **Token theft / replay** | Кратък access токен (15 мин) + refresh токен с **ротация** (single-use) и **revocation** при logout. При logout се записва per-user „cutoff" момент (`SessionRevocationService`): **всеки** access токен на потребителя, издаден преди logout, става невалиден — без значение дали е подаден при logout. Wallet проверява всеки токен чрез **token introspection** (sub + iat) → анулиран токен спира да работи преди да изтече. Токените се пазят само в паметта на клиента | [`JwtIssuer`](../auth-service/src/main/java/com/example/wallet/auth/jwt/JwtIssuer.java), [`SessionRevocationService`](../auth-service/src/main/java/com/example/wallet/auth/service/SessionRevocationService.java), [`IntrospectionController`](../auth-service/src/main/java/com/example/wallet/auth/web/IntrospectionController.java), [`RevocationValidator`](../wallet-service/src/main/java/com/example/wallet/wallet/config/RevocationValidator.java) |
| 3 | **SQL injection** | Spring Data JPA / параметризирани заявки навсякъде; никаква конкатенация на SQL | [`*Repository`](../wallet-service/src/main/java/com/example/wallet/wallet/domain) |
| 4 | **XSS** | Content-Security-Policy + `X-Content-Type-Options: nosniff`; frontend рендира данни през `textContent`, не `innerHTML` | [gateway `application.yml`](../gateway-service/src/main/resources/application.yml), [`app.js`](../gateway-service/src/main/resources/static/app.js) |
| 5 | **IDOR / broken access control** | Акаунтът се извежда **само** от `sub` claim на проверения токен — никога от параметър на заявката. Няма account id в път/тяло | [`WalletController`](../wallet-service/src/main/java/com/example/wallet/wallet/web/WalletController.java), [`WalletService`](../wallet-service/src/main/java/com/example/wallet/wallet/service/WalletService.java) |
| 6 | **Insecure configuration** | Тайни през env vars (не в кода); изключени stack traces/съобщения в грешки; secure defaults; асиметричен RS256 (частният ключ не напуска auth) | [auth `application.yml`](../auth-service/src/main/resources/application.yml), [`.env.example`](../.env.example), [`JwtKeyConfig`](../auth-service/src/main/java/com/example/wallet/auth/config/JwtKeyConfig.java) |
| 7 | **Service spoofing** (директно извикване на Wallet) | Gateway подписва всяка заявка с `X-Gateway-Auth` (HMAC-SHA256 + timestamp). Wallet отхвърля заявки без валиден токен → защита и срещу replay | [`GatewayToken`](../common-security/src/main/java/com/example/wallet/common/GatewayToken.java), [`ServiceTokenFilter`](../common-security/src/main/java/com/example/wallet/common/ServiceTokenFilter.java), [`ServiceTokenInjectionFilter`](../gateway-service/src/main/java/com/example/wallet/gateway/ServiceTokenInjectionFilter.java) |
| 8 | **Token forgery / tampering** | RS256 подпис; Wallet валидира токена срещу публичния ключ от JWKS. Подправен токен → 401 | [`JwksController`](../auth-service/src/main/java/com/example/wallet/auth/web/JwksController.java), wallet `oauth2ResourceServer` в [`SecurityConfig`](../wallet-service/src/main/java/com/example/wallet/wallet/config/SecurityConfig.java) |
| 9 | **Amount manipulation** | `BigDecimal` + bean-validation (`@DecimalMin 0.01`, `@Digits`) + повторна server-side проверка + проверка на баланса в транзакция; optimistic lock срещу race | [`TransferRequest`](../common-security/src/main/java/com/example/wallet/common/dto/TransferRequest.java), [`WalletService.transfer`](../wallet-service/src/main/java/com/example/wallet/wallet/service/WalletService.java), [`WalletAccount`](../wallet-service/src/main/java/com/example/wallet/wallet/domain/WalletAccount.java) |
| 10 | **Sensitive data exposure** | Паролите се пазят само като bcrypt хеш; audit log записва само метаданни (без токени/пароли); унифицирани грешки без вътрешни детайли | [`UserAccount`](../auth-service/src/main/java/com/example/wallet/auth/user/UserAccount.java), [`AuditService`](../wallet-service/src/main/java/com/example/wallet/wallet/service/AuditService.java), [`ApiError`](../common-security/src/main/java/com/example/wallet/common/dto/ApiError.java) |
| 11 | **Input abuse** (oversized/малформиран вход) | Bean validation на всички входни DTO-та (`@Size`, `@Pattern`, `@NotBlank`) | [`common-security/dto`](../common-security/src/main/java/com/example/wallet/common/dto) |
| 12 | **Clickjacking** | `X-Frame-Options: DENY` + `frame-ancestors 'none'` | gateway/services `SecurityConfig` и `application.yml` |

---

## Защо тези проектни решения

- **RS256 (асиметричен), а не споделен HMAC за JWT** — само auth притежава частния ключ;
  компрометиране на Wallet не позволява издаване на токени. Wallet валидира с публичен ключ
  от JWKS.
- **Opaque refresh токени в БД**, а не self-contained JWT — позволяват незабавна revocation
  и ротация, което самостоятелните JWT не дават лесно.
- **Service token + JWT (два независими слоя)** на Wallet — дори валиден потребителски токен
  не стига, ако заявката не е минала през Gateway.

## Известни опростявания (демо обхват)

- RSA ключът се генерира при старт (за production — managed secret store + ротация).
- TLS се прекратява извън приложението (reverse proxy) — в демото комуникацията е по HTTP в
  рамките на Docker мрежата.
- Refresh токенът се пази в паметта на SPA-то; в production се препоръчва HttpOnly + Secure
  + SameSite cookie.
