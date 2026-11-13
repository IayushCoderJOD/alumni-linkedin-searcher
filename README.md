# Alumni LinkedIn Profile Searcher

Spring Boot API that searches LinkedIn alumni through PhantomBuster and stores them in PostgreSQL.

## Run

```bash
docker compose up -d          # Postgres on localhost:5433
cp .env.example .env          # fill in the PhantomBuster values
mvn spring-boot:run           # API on http://localhost:8090
```

Without PhantomBuster credentials, start with sample data instead:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock
```

`PHANTOMBUSTER_AGENT_ID` is the id of a "LinkedIn Search Export" phantom in your PhantomBuster account, and `LINKEDIN_SESSION_COOKIE` is the `li_at` cookie of a logged-in LinkedIn session.

## API

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/alumni/search` | `{"university": "...", "designation": "...", "passoutYear": 2020}` (`passoutYear` optional) |
| GET | `/api/alumni/all` | |

Swagger UI is at http://localhost:8090/swagger-ui.html while the app is running. A Postman collection is in `postman/`.

## Tests

```bash
mvn test
```
