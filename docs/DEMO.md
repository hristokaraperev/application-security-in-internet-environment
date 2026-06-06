# DEMO runbook — за изпитната защита

Цел: бързо и уверено да покажеш всяка защитна мярка. Всяка стъпка има **какво да направиш**,
**очакван резултат** и **какво доказва**.

## 0. Стартиране

```powershell
copy .env.example .env          # по желание — compose има и defaults
docker-compose up --build       # вдига 3 услуги + 2 бази
```

Изчакай услугите да станат `healthy` (`docker-compose ps`). После отвори:

- **Frontend:** http://localhost:8080
- **Postman:** импортирай `docs/postman/wallet-demo.postman_collection.json` и
  `...environment.json`, избери средата „Wallet Demo".

Демо потребители: `alice` / `bob`, парола `Password123`. Начални баланси: alice 1000, bob 500.

> Съвет: дръж отворен и `docker-compose logs -f wallet-service` — там се вижда audit log-ът
> и отхвърлянията на заявки в реално време.

> **Нулиране до чисто състояние** (напр. след репетиция, в която си заключил акаунт или
> сменил баланси): `docker-compose down -v && docker-compose up -d`. Флагът `-v` трие
> базите, така че seed-данните (alice 1000, bob 500) се възстановяват.

---

## 1. Автентикация (happy path)

**Frontend:** влез с `alice` / `Password123` → виж баланс 1000 → превод 100 към `bob` →
балансът става 900, появява се транзакция.

**Postman:** папка `1. Auth → Login (alice)`, после `2. Wallet → Balance`, `Transfer`.

**Доказва:** JWT login, защитен достъп с токен, комуникация Gateway → Auth и Gateway → Wallet.

---

## 2. Заявка без токен → 401

**Postman:** `3. Attacks → Balance without token`.
**curl:**
```powershell
curl.exe -i http://localhost:8080/api/wallet/balance
```
**Очаквано:** `401 Unauthorized`.
**Доказва:** защитените ресурси изискват валиден JWT (мярка #2, #8).

---

## 3. Подправен токен → 401

**Postman:** `3. Attacks → Tampered token`.
**Очаквано:** `401` — RS256 подписът не съвпада.
**Доказва:** токените не могат да се фалшифицират без частния ключ (мярка #8).

---

## 4. Brute force / account lockout

**Postman:** `1. Auth → Login WRONG password` — изпълни **6 пъти** подред.
**curl:**
```powershell
1..6 | ForEach-Object {
  curl.exe -s -o NUL -w "%{http_code}`n" -X POST http://localhost:8080/auth/login `
    -H "Content-Type: application/json" `
    -d '{\"username\":\"alice\",\"password\":\"wrong\"}'
}
```
**Очаквано:** първите опити → `401`, след 5-ия → `429 Too Many Requests` (заключен акаунт).
**Доказва:** brute force защита чрез lockout + bcrypt (мярка #1).

> Забележка: това заключва `alice` за 15 мин. За да продължиш веднага, използвай `bob`,
> или рестартирай (`docker-compose restart auth-service auth-db` нулира при чиста база),
> или просто изчакай.

---

## 5. IDOR — опит за достъп до чужд баланс

Важното тук е, че **няма как** да поискаш чужд баланс — endpoint-ът няма параметър за
потребител; акаунтът идва от токена.

**Демонстрация:** влез като `bob` и поискай баланс → виждаш само баланса на `bob` (500),
никога на `alice`. Дори да подадеш `?username=alice`, той се игнорира.
```powershell
curl.exe -i "http://localhost:8080/api/wallet/balance?username=alice" -H "Authorization: Bearer <BOB_TOKEN>"
```
**Очаквано:** връща баланса на `bob`, не на `alice`.
**Доказва:** broken access control / IDOR е невъзможен по дизайн (мярка #5).

---

## 6. Директно извикване на Wallet (заобикаляне на Gateway) → 401

Wallet е изложен на `:8082`, но отхвърля заявки без валиден `X-Gateway-Auth`.

**Postman:** `3. Attacks → Direct call to wallet (bypass gateway)`.
**curl:**
```powershell
curl.exe -i http://localhost:8082/api/wallet/balance -H "Authorization: Bearer <VALID_TOKEN>"
```
**Очаквано:** `401` с `{"error":"forbidden_caller"}` — дори с валиден потребителски токен.
**Доказва:** service-to-service автентикация (мярка #7). Сравни с проксито през `:8080`,
което **минава**.

---

## 7. Манипулация на сумата → 400

**Postman:** `3. Attacks → Negative amount transfer`.
**curl:**
```powershell
curl.exe -i -X POST http://localhost:8080/api/wallet/transfer `
  -H "Authorization: Bearer <ALICE_TOKEN>" -H "Content-Type: application/json" `
  -d '{\"toUsername\":\"bob\",\"amount\":-100}'
```
**Очаквано:** `400 validation_error` (amount must be positive). Опитай и сума над баланса →
`422 insufficient_funds`.
**Доказва:** server-side валидация на парични операции (мярка #9).

---

## 8. Изтекъл токен → refresh

Access токенът живее 15 мин. За бърза демонстрация на потока:

**Frontend:** натисни „Поднови токен" → в статус панела се появява нов access токен;
старият refresh е ротиран (single-use).
**Postman:** `1. Auth → Refresh` → токените в средата се обновяват автоматично.
**Доказва:** ротация на refresh токени (мярка #2).

---

## 9. Logout → revocation (вкл. на access токена)

**Postman:** `1. Auth → Logout`, после:
- `3. Attacks → Balance AFTER logout, same token` → **401** (access токенът е блокиран веднага),
- `1. Auth → Refresh` със същия refresh токен → **401 invalid_refresh**.

**Очаквано:** и двете дават `401`.
**Доказва:** logout анулира **както refresh, така и access** токена. Access токенът се добавя в
blocklist в `auth-service`, а `wallet-service` го проверява чрез token introspection преди да
изпълни заявка (мярка #2). Това е отговорът на въпроса „ами ако токенът е откраднат / наистина ли
ме изхвърля logout".

---

## Кратка таблица за самопроверка

| Стъпка | Очакван код |
|--------|-------------|
| Login | 200 |
| Balance / Transfer (валидни) | 200 |
| Без токен | 401 |
| Подправен токен | 401 |
| 6-ти грешен login | 429 |
| Директно към :8082 | 401 (forbidden_caller) |
| Отрицателна сума | 400 |
| Превод над баланса | 422 |
| Refresh след logout | 401 |
