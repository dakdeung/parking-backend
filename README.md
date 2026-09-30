# Parking Backend – Parking Lot Reservation & Billing

Java 17 · Spring Boot 3.5 · Spring Data JPA · H2 (local/tests) · Oracle (Oracle Cloud Free) · Swagger UI

## Build, run, test

```bash
# Build + run all tests
./mvnw clean verify            # Windows: mvnw.cmd clean verify

# Run (in-memory H2, demo data seeded) -> http://localhost:8080/swagger-ui.html
./mvnw spring-boot:run

# Tests only (reports in target/surefire-reports)
./mvnw test
```

### Run against Oracle Cloud Free (Autonomous Database)

1. In the OCI console download the **Instance Wallet** of your database and unzip it (e.g. `C:/oracle/Wallet_parkdb`).
2. Set environment variables and start with the `oracle` profile:

```powershell
$env:ORACLE_DB_URL="jdbc:oracle:thin:@parkdb_high?TNS_ADMIN=C:/oracle/Wallet_parkdb"
$env:ORACLE_DB_USERNAME="ADMIN"
$env:ORACLE_DB_PASSWORD="your-password"
./mvnw spring-boot:run "-Dspring-boot.run.profiles=oracle"
```

Tables are created automatically (`DDL_AUTO=update` by default). To create them yourself, run
`docs/oracle-schema.sql` in SQL Developer / Database Actions and start with `DDL_AUTO=none`.

## Demo data (seeded on start-up)

| Lot | Hours | Slots |
|-----|-------|-------|
| `LOT-JKT01` | 24h | MOTORCYCLE M-01..M-04 (floor 0) · CAR A-01..A-03 (floor 0), B-01..B-03 (floor 1) · TRUCK T-01, T-02 (floor 0) |
| `LOT-BDG01` | 06:00–22:00 | CAR A-01, A-02 · MOTORCYCLE M-01 |

Rate cards: MOTORCYCLE 3,000/h (cap 20,000, overnight 10,000) · CAR 5,000/h (cap 50,000, overnight 20,000) ·
TRUCK 15,000/h (cap 120,000, overnight 30,000) · 15 min grace. Disable seeding with `parking.seed.enabled=false`.

## API

| Method | Path | Result |
|--------|------|--------|
| POST | `/api/v1/lots/{lotId}/availability` | `{ "vehicleType": "CAR", "startTime": "...", "endTime": "..." }` → available slots, best first (400 if a field is missing or `vehicleType` is not MOTORCYCLE / CAR / TRUCK) |
| POST | `/api/v1/reservations` | `{ lotId, vehicleType, licensePlate, customerId, plannedStartTime, plannedEndTime, slotId? }` → 200 + reservation (409 if no slot) |
| GET | `/api/v1/reservations/{id}` | reservation |
| POST | `/api/v1/reservations/{id}/check-in` | `{ "actualStartTime": "..." }` |
| POST | `/api/v1/reservations/{id}/check-out` | `{ "actualEndTime": "..." }` → reservation + invoice |
| DELETE | `/api/v1/reservations/{id}` | optional body `{ "reason": "..." }` → cancelled + `lateCancellation` |
| PUT | `/api/v1/reservations/{id}/extend` | `{ "newEndTime": "..." }` (409 on conflict) |
| GET | `/api/v1/invoices/{reservationId}` | invoice |

Status codes: 200, 400 (validation – body lists `violations[{field, reason, rejectedValue}]`), 404, 409
(double-booking / concurrent modification), 422 (business rule, e.g. wrong status), 500.
Dates are ISO-8601 `yyyy-MM-ddTHH:mm:ss`; money fields are JSON strings (`"17500.00"`).

## Project layout

```
com.demo.parking
├── constant/        String constants (VehicleType, SlotStatus, ReservationStatus), lengths, parameters, URLs
├── entity/          JPA entities with Lombok @Getter/@Setter only – no business logic
├── converter/       JPA AttributeConverters
├── repository/      Spring Data JPA repositories (JpaSpecificationExecutor; native SELECT … FOR UPDATE row locks)
├── specification/   JPA Specifications for all other queries (slots, reservations, invoices, rate cards)
├── service/         business logic called by controllers: ReservationService, BillingService, InvoiceService
│   └── seeder/      demo data (DataSeederService, ParkingLotSeederService, RateCardSeederService) – independent of service/
├── model/
│   ├── request/     request bodies (Lombok classes + Bean Validation)
│   ├── response/    API responses (entity → response mapping is done in services)
│   └── object/      shared value objects (FieldViolation)
├── controller/      thin REST controllers (only call a service and return its response)
├── exception/       custom exceptions + GlobalExceptionHandler
├── configuration/   security (permit-all), Jackson, OpenAPI, Clock, DataSeeder runner
└── utils/           Money helpers (scale 2, HALF_UP)
```

## Design notes

* **Thread safety / no double-booking** – services are stateless. `createReservation` locks every candidate slot
  of the lot + vehicle type with `SELECT … FOR UPDATE` (always in the same order → no deadlocks), then re-checks
  overlaps and inserts inside the same transaction. Extend / check-in / check-out / cancel lock the one slot they
  touch. Because the lock lives in the database, this also holds across several app instances. `@Version`
  columns add optimistic locking (→ 409). Covered by `ConcurrentReservationIntegrationTest`.
* **Billing** – `BigDecimal` only, HALF_UP, 2 decimals. Grace period is deducted from the start of the stay; the
  rest is split into 30-minute blocks (partial → round up) of `hourlyRate / 2`, starting at the end of the grace
  period. Each block is charged to the calendar day it starts on; each day is capped at `dailyCap`;
  `overnightSurcharge` is added once per midnight crossed. All three reference cases (A, B, C) pass.
* Entities are plain Lombok data holders; every rule (slot status changes, check-in window, cancellation,
  extension, invoice building, lot/slot validation) lives in the service classes.
* Time-dependent rules use an injected `Clock`, so tests are deterministic.

## Assumptions

* Overlap uses half-open windows `[start, end)`: a booking ending at 13:00 does not clash with one starting at 13:00.
* Availability is based on PENDING/ACTIVE reservations for the window, not on the slot's *current* status; slots in
  MAINTENANCE are never offered. Slot status reflects the present: RESERVED after booking, OCCUPIED after check-in,
  back to AVAILABLE (or RESERVED if other bookings remain) after check-out / cancel / no-show.
* Check-in is allowed from 30 min before to 30 min after `plannedStartTime`. Earlier → 422; later → reservation
  becomes NO_SHOW (returned with HTTP 200) and the slot is released.
* Late cancellation = cancelled at or after `plannedStartTime − 30 min` (by server clock). Cancelling an ACTIVE
  reservation is therefore always late.
* Extension is allowed for PENDING and ACTIVE reservations; only the extra window is checked for conflicts.
* Billing uses actual check-in/out times, grace applies once per stay; leaving exactly at 00:00 is not "past
  midnight". A stay fully inside the grace period costs 0, including surcharges.
* `billedDurationMinutes` = stay minus grace, in minutes (rounded up to whole minutes).
* `discountAmount` is always 0 – no discount rules are defined in Tasks 1–4.
* `slotId` in the reservation request is optional; when given, that exact slot is booked or 409 is returned.
* Lots with limited hours must have `open < close`; reservations there must start and end on the same day.
* No authentication is required by the spec, so Spring Security is configured as permit-all.

## Known limitations / trade-offs

* Row locking serialises bookings per lot + vehicle type — simple and correct, but a very busy lot would
  benefit from finer-grained locking or a DB exclusion constraint.
* A vehicle that overstays its `plannedEndTime` while ACTIVE is not automatically detected.
* Rate cards are global per vehicle type (not per lot) and operating hours are the same every day.
* Task 5 (dynamic pricing & reports) is not implemented.
