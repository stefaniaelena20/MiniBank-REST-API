# MiniBank

REST API pentru un sistem bancar simplificat, implementat ca parte a unui coding challenge axonsoft.

## Tehnologii
- Java 25
- Spring Boot 4.0.3
- Spring Data JPA + Hibernate 7
- H2 in-memory database
- Lombok
- Apache Commons Validator (validare IBAN)

## Arhitectură
Proiectul urmează o arhitectură clasică pe straturi:
- **Controller** — expune endpoint-urile REST
- **Service** — conține logica de business
- **Repository** — acces la baza de date prin Spring Data JPA
- **Model** — entități JPA și DTO-uri

## Funcționalități implementate

### Conturi bancare
- Creare conturi cu validare IBAN (format + checksum)
- Tipuri de conturi: CHECKING și SAVINGS
- Monede suportate: RON, EUR, USD, GBP
- Listare paginată

### Transferuri
- Transferuri între orice două conturi SEPA
- Conversie valutară automată cu rate fixe
- Idempotență prin `idempotencyKey` — transferurile duplicate nu se execută de două ori
- Protecție la acces concurent prin pessimistic locking (ordine după ID pentru a evita deadlock)

### Tranzacții
- Istoric complet per cont în ordine cronologică
- Tipuri: DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
- Balanță după fiecare tranzacție (`balanceAfter`)

### Reguli de business
- Contul bancă sistem (ID=1) are fonduri nelimitate și nu are ledger propriu
- Conturi SAVINGS: limită zilnică de 5000 EUR echivalent pentru transferuri outgoing
- Balanța nu poate deveni negativă
- Doar transferuri între țări membre SEPA sunt permise
- IBAN-urile trebuie să fie valide (checksum internațional)

### Rate de schimb
- Rate fixe încărcate din `exchange-rates.yml`
- EUR: 4.97 RON, USD: 4.56 RON, GBP: 5.73 RON
- Conversie: `amount * (sourceToRON / targetToRON)`
- Rotunjire HALF_EVEN (banker's rounding) la 2 zecimale

## Endpoint-uri API

| Method | Endpoint | Descriere |
|--------|----------|-----------|
| POST | `/api/accounts` | Creare cont nou |
| GET | `/api/accounts/{id}` | Detalii cont |
| GET | `/api/accounts` | Listă conturi (paginat) |
| GET | `/api/accounts/{id}/transactions` | Istoric tranzacții |
| POST | `/api/transfers` | Creare transfer |
| GET | `/api/transfers/{id}` | Detalii transfer |
| GET | `/api/transfers` | Listă transferuri (filtrat + paginat) |
| GET | `/api/exchange-rates` | Rate de schimb curente |
| GET | `/actuator/health` | Status aplicație |

## Rulare locală

### Cerințe
- Java 25
- Maven 3.9+

### Start
```bash
mvn spring-boot:run
```

### Verificare
```bash
curl http://localhost:8080/actuator/health
# Răspuns: {"status":"UP"}
```

### H2 Console (development)
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:minibank
Username: sa
Password: (gol)
```

## Exemple de utilizare

### Creare cont
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "ownerName": "Alice Popescu",
    "iban": "RO86BCDE2A02345678901234",
    "currency": "EUR",
    "accountType": "CHECKING"
  }'
```

### Depunere fonduri (din contul bancă sistem)
```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceIban": "RO49AAAA1B31007593840000",
    "targetIban": "RO86BCDE2A02345678901234",
    "amount": 1000.00
  }'
```

### Transfer între conturi
```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceIban": "RO86BCDE2A02345678901234",
    "targetIban": "RO49AAAA1B31007593840001",
    "amount": 250.00,
    "idempotencyKey": "transfer-001"
  }'
```
