# WorkforceOS API Specification

**Status:** In progress — reflects the implemented `v1` surface | **Version:** `v1`

## 1. Conventions

Base path: `/api/v1`

- JSON request and response bodies use `Content-Type: application/json`.
- Timestamps are ISO 8601 with timezone information and are returned in UTC.
- Collection endpoints currently return full lists; pagination parameter names and maximum page size are TBD.
- Protected routes require `Authorization: Bearer <access-token>`.
- Mutating retry-sensitive commands accept `Idempotency-Key` (enforced by the consumers of the command; the contract is TBD).
- Resource IDs are opaque to clients (UUIDs).

## 2. Authentication

```http
POST /api/v1/auth/login
Content-Type: application/json

{"username":"admin","password":"admin"}
```

Login accepts `username` and `password`. Successful login returns:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer"
}
```

The `accessToken` is a signed bearer token (HMAC-SHA256 over `base64url(username:expiry)` using `workforce.auth.signing-key`), valid for 1 hour. It is not a JWT; JWT adoption is a pending decision. Invalid credentials return `401 Unauthorized` without revealing whether the identity exists. A default `admin` account (roles `ADMIN`) is bootstrapped when the identity store is empty, and a `manager` account (roles `MANAGER`) is always ensured.

### 2.1 Organization administration

```http
POST /api/v1/admin/accounts
Authorization: Bearer <admin-token>
Content-Type: application/json

{"username":"operator","password":"secret","organizationId":"00000000-0000-0000-0000-000000000002","roles":["MANAGER"]}
```

`ADMIN`-only. Provisions an account owned by the given organization; the account authenticates against that organization only. Returns `201 Created` with the account record, or `400 Bad Request` when required fields are missing. `409 Conflict` when the username already exists.

## 3. Resource Endpoints

Implemented endpoints:

| Area | Endpoints |
| --- | --- |
| Auth | `POST /auth/login` |
| Health | `GET /health` |
| Organizations | `GET /organizations`, `POST /organizations`, `GET /organizations/{id}`, `PATCH /organizations/{id}` (all `ADMIN`-only), `GET /organizations/current` (any authenticated user; resolves the caller's tenant) |
| Admin | `POST /admin/accounts` (`ADMIN`-only; provisions a user account scoped to an organization) |
| Dashboard | `GET /dashboard/summary`, `GET /dashboard/operations` |
| Employees | `GET/POST /employees`, `GET/PUT/DELETE /employees/{id}` |
| Departments | `GET/POST /departments`, `GET/DELETE /departments/{id}` |
| Teams | `GET/POST /teams`, `GET/DELETE /teams/{id}` |
| Shifts | `GET/POST /shifts`, `GET/PATCH /shifts/{id}` |
| Rosters | `GET/POST /rosters`, `GET /rosters/conflicts`, `GET /rosters/{id}`, `GET /rosters/{id}/auto-schedule`, `POST /rosters/{id}/publish`, `GET/POST /rosters/{id}/assignments` |
| Attendance | `POST /attendance/clock-in`, `POST /attendance/clock-out`, `GET /attendance/me?employeeId=<uuid>` |
| Attendance corrections | `GET/POST /attendance-corrections`, `POST /attendance-corrections/{id}/approve`, `POST /attendance-corrections/{id}/reject` |
| Leave | `GET/POST /leave-requests`, `POST /leave-requests/{id}/approve` |
| Overtime | `GET/POST /overtime-requests`, `POST /overtime-requests/{id}/approve`, `POST /overtime-requests/{id}/reject` |
| Shift swaps | `GET/POST /shift-swap-requests`, `POST /shift-swap-requests/{id}/approve`, `POST /shift-swap-requests/{id}/reject` |
| Handovers | `GET/POST /handovers`, `POST /handovers/{id}/submit`, `POST /handovers/{id}/acknowledge` |
| Approvals | `GET /approvals/history`, `GET /approvals/{requestId}` |
| Notifications | `GET/POST /notifications`, `GET /notifications/{id}`, `GET /notifications/recipient/{recipientId}`, `POST /notifications/{id}/mark-as-read` |
| Audit | `GET/POST /audit-logs`, `GET /audit-logs/{id}` |
| Reports | `GET /reports/leave-requests`, `GET /reports/overtime-requests`, `GET /reports/attendance`, `GET /reports/audit-summary`, `GET /reports/attendance-by-employee`, `GET /reports/overtime-by-employee`, `GET /reports/department-staffing` |
| Risk intelligence | `POST /risk/analyze`, `GET /risk/summary`, `GET /risk/assessments`, `GET /risk/assessments/{id}/recommendations`, `GET /risk/trend`, `GET /risk/alerts`, `POST /risk/alerts/{id}/resolve` |
| Assistant | `POST /assistant/ask` |
| Analytics | `GET /analytics/absenteeism?windowDays=<n>`, `GET /analytics/forecast?horizonDays=<n>` |
| Integrations | `GET /integrations`, `POST /integrations/biometric/clock-events` (config-gated) |
| Events | `GET /events/outbox`, `GET /events/received` |

Shift swap and attendance corrections were planned as P1 and are now implemented (in-memory). Break events and `GET/PATCH` on several resources are not yet exposed by the API.

## 4. Response Semantics

- `200 OK`: successful read or action returning a representation.
- `201 Created`: successful resource creation, with `Location` where applicable.
- `204 No Content`: successful update with no body.
- `400 Bad Request`: malformed or invalid input.
- `401 Unauthorized`: missing or invalid authentication.
- `403 Forbidden`: authenticated but not authorized.
- `404 Not Found`: resource is unavailable to the caller.
- `409 Conflict`: business invariant or idempotency conflict.
- `422 Unprocessable Entity`: semantically invalid request, if adopted by the implementation.
- `429 Too Many Requests`: API rate limit exceeded. The `RateLimitFilter` token-bucket limiter returns a `Retry-After` header and exempts `/health` and `/actuator/**`, and is enabled via `workforce.api-rate-limit.enabled`.
- `500/503`: unexpected or unavailable service failure.

## 5. Error Contract

Errors are returned through a single `ApiExceptionHandler` with this shape:

```json
{
  "timestamp": "2026-08-28T08:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Employee already has an active attendance session",
  "path": "/api/v1/attendance/clock-in",
  "traceId": "4f6c9a11e2b3456f",
  "requestId": "b8d2f0a1-6b2e-4c3a-9d1f-1a2b3c4d5e6f"
}
```

`traceId` and `requestId` correlate the response with the structured log line. Validation and unreadable-body failures return `400` with `"Malformed or missing request fields"`; unknown failures return `500` with `"Internal error"`. Messages must be safe for clients and must not expose credentials, secrets, SQL, or sensitive internal details.

## 6. Authorization Expectations

Employees may access their own schedules, attendance, and requests. Supervisors are limited to authorized teams. Managers and HR receive organization-scoped administrative access. System administrators (`ADMIN`) manage system access and organizations. Cross-tenant resource access (by organization) returns `404` to avoid disclosing existence. Every endpoint must document role and organization-scope checks.

## 7. Contract Governance

The API base path and response/error conventions are implemented across all controllers. A generated or maintained OpenAPI document is not yet published. Breaking changes require a new version or a compatibility decision. Examples, validation rules, pagination, sorting, filtering, and rate limits must be added before production release.
