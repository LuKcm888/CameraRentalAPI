# Camera Rental API

A production-style REST API for a camera rental business, built with Spring Boot 4, Spring Security (JWT), and PostgreSQL. This project goes beyond basic CRUD — it implements real-world business rules, lifecycle management, N+1 query prevention, and a layered testing strategy.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Clients                            │
│              (Postman, React, Mobile)                    │
└──────────────────────┬──────────────────────────────────┘
                       │  HTTP/JSON
┌──────────────────────▼──────────────────────────────────┐
│  Security Filter Chain                                  │
│  ┌──────────────┐ ┌──────────────┐ ┌─────────────────┐ │
│  │OriginCheck   │→│ RateLimit    │→│ AuthTokenFilter  │ │
│  │Filter        │ │ (Bucket4j)   │ │ (JWT validation) │ │
│  └──────────────┘ └──────────────┘ └─────────────────┘ │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│  Controllers          @PreAuthorize role checks         │
│  ┌────────┐ ┌───────────┐ ┌───────┐ ┌───────────────┐  │
│  │Camera  │ │Inventory  │ │Units  │ │BusinessHours  │  │
│  └───┬────┘ └─────┬─────┘ └───┬───┘ └───────┬───────┘  │
└──────┼────────────┼───────────┼──────────────┼──────────┘
       │            │           │              │
┌──────▼────────────▼───────────▼──────────────▼──────────┐
│  Service Layer        Business rules, DTO mapping       │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│  Spring Data JPA      @EntityGraph, JPQL aggregates     │
│  ┌────────────┐ ┌───────────────┐ ┌──────────────────┐  │
│  │Camera Repo │ │InventoryItem  │ │PhysicalUnit Repo │  │
│  │            │ │Repo           │ │(batch counts)    │  │
│  └────────────┘ └───────────────┘ └──────────────────┘  │
└──────────────────────┬──────────────────────────────────┘
                       │
            ┌──────────▼──────────┐
            │  PostgreSQL + Redis │
            └─────────────────────┘
```

## Domain Model

The inventory system uses a three-tier design that separates catalog data from business data from physical assets:

```
Camera (catalog)          InventoryItem (pricing)       PhysicalUnit (asset)
┌──────────────────┐      ┌─────────────────────┐      ┌────────────────────┐
│ brand            │      │ dailyRentalPrice    │      │ serialNumber       │
│ modelName        │ 1:1  │ replacementValue    │ 1:N  │ condition (enum)   │
│ category         │◄────►│ camera (FK)         │◄────►│ status (enum)      │
│ sensorFormat     │      │                     │      │ acquiredDate       │
│ isActive         │      │                     │      │ notes              │
│ resolution, etc. │      │                     │      │                    │
└──────────────────┘      └─────────────────────┘      └────────────────────┘

Camera = what it is       InventoryItem = how much      PhysicalUnit = which one
                          it costs                      is on the shelf
```

**Why three tiers?** A rental business needs to know not just *what cameras they offer* (catalog), but *how much to charge* (pricing) and *exactly which serial-numbered unit* a customer walked out with (asset tracking). This separation also means you can deactivate a catalog entry without losing pricing history or asset records.

## Features

### Authentication & Authorization
- JWT-based auth with sign-in, sign-up, and sign-out (token blacklisting via Redis)
- Three roles: **ADMIN**, **VENDOR**, **CUSTOMER** with `@PreAuthorize` enforcement
- Login rate limiting powered by Bucket4j (Redis-backed, in-memory fallback)
- Origin-check filter for CSRF protection on mutation endpoints

### Camera Catalog
- Full CRUD with paginated, searchable listings
- Case-insensitive search across brand and model name
- Sort-field whitelisting to prevent arbitrary column access
- **Soft-delete** via `PATCH /deactivate` — hides from customer listings, preserves history
- **Hard-delete** gated by business rules — rejected with `409` if physical units still exist
- `includeInactive` query param for admin visibility into deactivated cameras

### Inventory & Physical Units
- One inventory item per camera (rental pricing)
- Individual physical units tracked by serial number, condition, and status
- Real-time `totalUnits` and `availableUnits` computed from unit status
- Unit lifecycle: `AVAILABLE` → `RENTED` → `MAINTENANCE` → `RETIRED`
- Unit condition tracking: `NEW` → `EXCELLENT` → `GOOD` → `FAIR` → `POOR`

### Business Hours
- Weekly schedule management keyed by day of week
- Case-insensitive day lookup (`/monday`, `/MONDAY`, `/Monday`)
- Public read access, admin-only writes

### Error Handling
- Global exception handler with structured JSON responses
- `400` validation errors, `403` authorization, `404` not found, `409` conflict, `500` unexpected
- Specific handlers for `DataIntegrityViolationException`, `MethodArgumentTypeMismatchException`, malformed JSON, and more

### Performance
- **N+1 query prevention** via `@EntityGraph` (eager camera loading) and batched JPQL aggregates (unit counts in one query)
- Manual DTO mapping on hot paths to avoid ModelMapper lazy-proxy traversal
- Query plans documented in service-layer Javadoc

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4, Spring Security 6 |
| Database | PostgreSQL 16, Spring Data JPA / Hibernate 6 |
| Caching / Rate Limiting | Redis 7, Redisson, Bucket4j |
| Auth | JWT (jjwt 0.12), BCrypt |
| Build | Maven |
| Testing | JUnit 5, Mockito, Testcontainers, AssertJ |
| Infrastructure | Docker Compose |

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL (port `5434`) and Redis (port `6379`).

### 2. Configure the application

Copy the example properties file and fill in your values:

```bash
cp src/main/resources/application-local-pg.properties.example \
   src/main/resources/application-local-pg.properties
```

At minimum, set your JWT secret (64+ characters for HS512):

```properties
spring.app.jwtSecret=your-secret-key-here-must-be-at-least-64-characters-long-for-hs512
```

### 3. Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-pg
```

The app starts on `http://localhost:8080` with seed data:

| User | Password | Role |
|---|---|---|
| `admin` | `password123` | ADMIN |
| `vendor` | `password123` | VENDOR |
| `customer` | `password123` | CUSTOMER |

Five cameras, inventory items, and physical units are seeded automatically along with a full weekly business hours schedule.

### 4. Get a token

```bash
curl -X POST http://localhost:8080/api/v1/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

Use the returned `accessToken` as `Authorization: Bearer <token>` on subsequent requests.

## API Endpoints

### Auth
| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/signup` | Public | Register a new customer |
| `POST` | `/api/v1/auth/signin` | Public | Sign in, get JWT |
| `POST` | `/api/v1/auth/signout` | Bearer | Blacklist current token |
| `GET` | `/api/v1/auth/user` | Bearer | Get current user info |

### Cameras
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/cameras` | Bearer | List active cameras (paginated, searchable) |
| `GET` | `/api/v1/cameras?includeInactive=true` | Bearer | Include deactivated cameras |
| `GET` | `/api/v1/cameras/{id}` | Bearer | Get camera by ID |
| `POST` | `/api/v1/cameras` | Admin | Create a camera |
| `PUT` | `/api/v1/cameras/{id}` | Admin | Update a camera |
| `PATCH` | `/api/v1/cameras/{id}/deactivate` | Admin | Soft-delete (recommended) |
| `DELETE` | `/api/v1/cameras/{id}` | Admin | Hard-delete (data cleanup only) |

### Inventory
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/inventory` | Bearer | List inventory with unit counts |
| `GET` | `/api/v1/inventory/{id}` | Bearer | Get single item with counts |
| `POST` | `/api/v1/inventory` | Admin | Create inventory for a camera |
| `PUT` | `/api/v1/inventory/{id}` | Admin | Update pricing |
| `DELETE` | `/api/v1/inventory/{id}` | Admin | Delete inventory item |

### Physical Units
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/units/inventory/{inventoryId}` | Bearer | List units for an inventory item |
| `GET` | `/api/v1/units/{id}` | Bearer | Get unit by ID |
| `POST` | `/api/v1/units` | Admin | Register a new physical unit |
| `PUT` | `/api/v1/units/{id}` | Admin | Update unit details |
| `DELETE` | `/api/v1/units/{id}` | Admin | Remove a physical unit |

### Business Hours
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/business-hours` | Public | Get weekly schedule |
| `GET` | `/api/v1/business-hours/{day}` | Public | Get single day (case-insensitive) |
| `POST` | `/api/v1/business-hours` | Admin | Create entry for a day |
| `PUT` | `/api/v1/business-hours/{day}` | Admin | Update a day's hours |
| `DELETE` | `/api/v1/business-hours/{day}` | Admin | Remove a day's entry |

## Testing

### Run all tests

```bash
./mvnw test
```

**Requirements:** Docker must be running (Testcontainers spins up a PostgreSQL container for integration tests).

### Test pyramid

| Layer | What it tests | Framework |
|---|---|---|
| **Unit** | Service business logic, utilities, security components | JUnit 5 + Mockito |
| **Integration** | Full HTTP request/response through all controllers | MockMvc + Testcontainers (PostgreSQL) |

## Project Structure

```
src/main/java/com/camerarental/backend/
├── config/              # App config, seed data, constants
├── controller/          # REST controllers
├── exceptions/          # Custom exceptions + global handler
├── model/
│   ├── base/            # AuditableEntity (createdAt, updatedAt, createdBy)
│   └── entity/          # JPA entities + enums
├── payload/             # DTOs and response wrappers
├── repository/          # Spring Data JPA repositories
├── security/
│   ├── filters/         # Rate limiting, request logging
│   ├── jwt/             # JWT utils, token filter, blacklist service
│   ├── request/         # Login/signup request DTOs
│   ├── response/        # Auth response DTOs
│   └── services/        # UserDetailsService implementation
├── service/             # Business logic (interfaces + implementations)
└── util/                # PaginationHelper
```

## Configuration Profiles

| Profile | Database | Use case |
|---|---|---|
| `local-pg` | PostgreSQL (Docker) | Local development with Postman |
| `local-h2` | H2 in-memory | Quick local testing without Docker |
| `test-pg` | PostgreSQL (Testcontainers) | Automated integration tests |
| `prod` | PostgreSQL | Production deployment |
| `stage` | PostgreSQL | Staging environment |

Example property files (`.example`) are provided for each profile. Copy and fill in your secrets.

## Roadmap

- [ ] Rental/reservation system with state machine (PENDING → ACTIVE → RETURNED → OVERDUE)
- [ ] Availability query endpoint
- [ ] Pricing rules (late fees, weekly discounts, weekend surcharges)
- [ ] Admin endpoint for user role management
- [ ] Swagger UI integration
- [ ] Damage reporting and insurance tracking

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
