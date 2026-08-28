# WorkforceOS API Specification

**Status:** Draft | **Version:** `v1`

## 1. Conventions

Base path: `/api/v1`

- JSON request and response bodies use `Content-Type: application/json`.
- Timestamps are ISO 8601 with timezone information and are returned in UTC.
- Collection endpoints support pagination; exact parameter names and maximum page size are TBD.
- Protected routes require `Authorization: Bearer <access-token>`.
- Mutating retry-sensitive commands accept `Idempotency-Key`.
- Resource IDs are opaque to clients.

## 2. Authentication

```http
POST /api/v1/auth/login
Content-Type: application/json

{"usernameOrEmail":"user@example.com","password":"..."}
```

Successful login returns an access token and expiry metadata. Invalid credentials return `401 Unauthorized` without revealing whether the identity exists.

## 3. Resource Endpoints

| Area | Endpoints |
| --- | --- |
| Employees | `GET/POST /employees`, `GET/PATCH /employees/{id}` |
| Departments and teams | `GET/POST /departments`, `GET/POST /teams` |
| Shifts | `GET/POST /shifts`, `GET/PATCH /shifts/{id}` |
| Rosters | `GET/POST /rosters`, `POST /rosters/{id}/publish` |
| Attendance | `POST /attendance/clock-in`, `POST /attendance/clock-out`, `GET /attendance/me` |
| Leave | `POST /leave-requests`, `GET /leave-requests`, approval actions by ID |
| Overtime | `POST /overtime-requests`, `GET /overtime-requests`, approval actions by ID |
| Handovers | `POST /handovers`, submit and acknowledge actions by ID |
| Reports | Authorized report endpoints under `/reports` |

Additional shift swap and attendance correction endpoints are P1.

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
- `429 Too Many Requests`: rate limit exceeded when enabled.
- `500/503`: unexpected or unavailable service failure.

## 5. Error Contract

```json
{
  "timestamp": "2026-08-28T08:30:00Z",
  "status": 409,
  "code": "ATTENDANCE_ALREADY_ACTIVE",
  "message": "Employee already has an active attendance session",
  "path": "/api/v1/attendance/clock-in",
  "traceId": "trace-id"
}
```

Messages must be safe for clients and must not expose credentials, secrets, SQL, or sensitive internal details.

## 6. Authorization Expectations

Employees may access their own schedules, attendance, and requests. Supervisors are limited to authorized teams. Managers and HR receive organization-scoped administrative access. Administrators manage system access. Every endpoint must document role and organization-scope checks.

## 7. Contract Governance

The implementation should publish an OpenAPI document generated or maintained from the API contract. Breaking changes require a new version or a compatibility decision. Examples, validation rules, pagination, sorting, filtering, and rate limits must be added before production release.
