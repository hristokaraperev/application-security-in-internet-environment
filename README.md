# Сигурност на приложения в интернет среда

## Задача

Малък демонстрационнен проект, който показва реализация на сигурна автентикация и защита на комуникацията между услуги

## Изисквания

### Минимум три отделни сървиса

* например authentication service
* API service
* frontend или gateway service

### Автентикация чрез JWT (JSON Web Token)

* генериране на токен при login
* валидиране на токена при достъп до защитени ресурси
* комуникация между сървиси с използване на токена

### Демонстрация на комуникация между сървиси

* защитени API заявки
* валидиране на идентичността на клиента или услугата

### Проектът трябва да включва кратко обяснение

* какви типични заплахи са взети предвид (например: injection, XSS, brute force, token theft, insecure configuration и др.)
* какви мерки за защита са приложени

## Представяне на проекта

Всички проекти ще бъдат представени в деня на изпита . По време на представянето студентът трябва да демонстрира:
* работеща система
* процеса на автентикация
* комуникацията между услугите
* реализираните мерки за сигурност

---

## Реализация: Сигурен дигитален портфейл (e-wallet)

Микросървизна система от **три услуги** със собствени бази, JWT автентикация (RS256) и
service-to-service автентикация.

```
Browser ──▶ Gateway (:8080) ──▶ Auth service   (:8081)  ── PostgreSQL (auth-db)
            frontend + рутиране └▶ Wallet service (:8082)  ── PostgreSQL (wallet-db)
```

| Услуга | Роля |
|--------|------|
| **gateway-service** | единствена публична входна точка; сервира frontend; рутира `/auth/**` и `/api/**`; добавя service token; rate limiting; security headers |
| **auth-service** | регистрация/login/refresh/logout; издава RS256 JWT; публикува JWKS |
| **wallet-service** | защитен resource server: баланс, преводи, транзакции; валидира JWT и service token |

### Стартиране (Docker)

```powershell
copy .env.example .env        # по желание — compose има и defaults
docker-compose up --build
```

- Frontend: **http://localhost:8080**
- Демо потребители: `alice` / `bob`, парола `Password123`

### Тестване

- **Frontend** — нагледен потребителски поток.
- **Postman** — `docs/postman/` (папки: Auth, Wallet happy path, Attacks).
- **Runbook за изпита** — [docs/DEMO.md](docs/DEMO.md): стъпка-по-стъпка за всяка защита.

### Сигурност

Пълен threat model с препратки към кода: [docs/SECURITY.md](docs/SECURITY.md).

