# Secure REST API — лабораторная работа 1

Защищённый REST API на **Java 21 + Spring Boot 4.1** (Spring Security, Spring Data JPA/Hibernate, H2, JJWT)
с автоматической проверкой безопасности в **GitHub Actions** (SAST: SpotBugs + Find Security Bugs, SCA: OWASP Dependency-Check).

## Эндпоинты

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| `POST` | `/auth/register` | публичный | Регистрация пользователя (`username`, `password`) |
| `POST` | `/auth/login` | публичный | Аутентификация, возвращает JWT |
| `GET` | `/api/data?q=` | JWT | Список постов (опционально — поиск по подстроке `q`) |
| `POST` | `/api/data` | JWT | Создание поста (`title`, `content`) — **третий метод** |

## Реализованные меры защиты

| Угроза (OWASP Top 10) | Мера | Где в коде |
|---|---|---|
| **A03 Injection (SQLi)** | Только Spring Data JPA / Hibernate с именованными параметрами (`:username`, `:query`) → PreparedStatement. Конкатенации строк в SQL нет. | `UserRepository`, `PostRepository` |
| **A03 Injection (XSS)** | Все пользовательские строки экранируются `HtmlUtils.htmlEscape` (встроенная функция Spring) перед отдачей; заголовок `Content-Security-Policy: default-src 'none'`; `X-Content-Type-Options: nosniff` | `HtmlSanitizer`, `PostResponse`, `UserResponse`, `SecurityConfig` |
| **A07 Broken Authentication** | JWT (HS256, срок жизни 15 мин, проверка подписи/`exp`/`iss`, отклонение `alg=none`); фильтр-middleware `JwtAuthenticationFilter` на всех `/api/**`; пароли — **bcrypt (cost 12)**; блокировка после 5 неудачных попыток входа на 5 мин; одинаковый ответ и время ответа для «нет пользователя» и «неверный пароль» | `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthService`, `LoginAttemptService` |
| **A01 Broken Access Control** | По умолчанию `denyAll`, разрешены только явно перечисленные пути; роль берётся из БД, а не из токена | `SecurityConfig` |
| **A04/A05 Misconfiguration** | Валидация входных данных (Bean Validation), без стек-трейсов и внутренних сообщений в ответах, H2-консоль выключена, секрет JWT — из переменной окружения | `GlobalExceptionHandler`, `application.yml` |
| **A09 Logging** | В логи не пишутся данные из токена (защита от CRLF-инъекции в логи), `toString()` DTO маскирует пароль | `JwtService`, `LoginRequest` |

## Запуск

Требуется JDK 21 (Maven ставить не нужно — используется Maven Wrapper).

```bash
./mvnw verify
```

```bash
JWT_SECRET=$(openssl rand -base64 32) ./mvnw spring-boot:run
```

Если `JWT_SECRET` не задан, ключ генерируется случайно при старте (токены перестают быть валидными после перезапуска).

## Проверка через curl

```bash
# 1. Регистрация
curl -s -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"Str0ng-Passw0rd"}'
# {"id":1,"username":"alice","role":"USER"}

# 2. Логин с неверным паролем -> 401
curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"wrong"}'
# {"status":401,"error":"Invalid username or password",...}

# 3. Логин -> JWT
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"Str0ng-Passw0rd"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')

# 4. Доступ без токена -> 401
curl -i localhost:8080/api/data
# HTTP/1.1 401 / WWW-Authenticate: Bearer / {"status":401,"error":"Unauthorized"}

# 5. Создание поста с XSS-нагрузкой -> данные экранированы
curl -s -X POST localhost:8080/api/data -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"<script>alert(1)</script>","content":"hello"}'
# {"id":1,"title":"&lt;script&gt;alert(1)&lt;/script&gt;",...}

# 6. Получение данных с токеном -> 200
curl -s localhost:8080/api/data -H "Authorization: Bearer $TOKEN"

# 7. Попытка SQL-инъекции в поиске -> воспринимается как обычный текст
curl -s -G localhost:8080/api/data --data-urlencode "q=' OR 1=1 --" -H "Authorization: Bearer $TOKEN"
# []
```

## Проверка через Postman

Импортируйте коллекцию [`postman/secure-api.postman_collection.json`](postman/secure-api.postman_collection.json)
(*Import → File*). В ней 10 запросов с автотестами (18 проверок): регистрация, неверный пароль, SQLi в логине,
успешный логин (токен автоматически сохраняется в переменную `{{token}}`), доступ без токена и с поддельным токеном,
XSS-нагрузка, получение данных, SQLi в поиске. Запустите всю коллекцию через *Run collection* — все тесты должны быть зелёными.

Из консоли то же самое можно сделать через newman:

```bash
npx newman run postman/secure-api.postman_collection.json
```

Те же сценарии автоматизированы в интеграционных тестах: `src/test/java/ru/itmo/secureapi/SecurityIntegrationTest.java`.

## CI/CD (`.github/workflows/ci.yml`)

Запускается на каждый `push` и `pull_request`. Три job'а:

1. **Build & tests** — `./mvnw verify` (компиляция + 11 интеграционных тестов безопасности).
2. **SAST** — `./mvnw compile spotbugs:check` со SpotBugs + плагином **Find Security Bugs**
   (порог `Low`, сборка падает при любой находке). Отчёт `spotbugs.html` сохраняется как artifact,
   SARIF публикуется в *Security → Code scanning*.
3. **SCA** — **OWASP Dependency-Check** (`dependency-check-maven:check`), сборка падает при CVSS ≥ 7.
   HTML/JSON/SARIF-отчёт сохраняется как artifact `dependency-check-report`.

> Для SCA нужен бесплатный ключ NVD API (https://nvd.nist.gov/developers/request-an-api-key) —
> добавьте его в *Settings → Secrets and variables → Actions* как `NVD_API_KEY`. Без ключа первая загрузка базы NVD идёт очень долго.
> База NVD кэшируется между запусками.

### Найденные и исправленные SAST-замечания

Первый прогон Find Security Bugs нашёл 9 замечаний; исправлено:

| Замечание | Исправление |
|---|---|
| `CRLF_INJECTION_LOGS` — сообщение исключения JWT (с данными из токена) писалось в лог | В лог пишется только тип исключения |
| `XSS_SERVLET` — запись строки в `PrintWriter` в обработчике 401/403 | Тело ответа — заранее подготовленная константа (`byte[]`), `Content-Type: application/json` |
| `CT_CONSTRUCTOR_THROW` — исключение в конструкторе `JwtService` (Finalizer attack) | Класс сделан `final` |
| Сгенерированный Spring пароль `inMemoryUserDetailsManager` в логах | Свой `UserDetailsService` на основе БД |

Оставшиеся информационные маркеры (`SPRING_ENDPOINT`, `SERVLET_HEADER`, `SPRING_CSRF_PROTECTION_DISABLED` для stateless JWT-API)
исключены с обоснованием в `spotbugs-exclude.xml`.
