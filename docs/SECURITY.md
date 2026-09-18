# WorkforceOS Security

**Status:** Draft security baseline | **Implementation:** authenticated bearer-token API with role enforcement; hashing, CORS, input validation, a per-request tenant context, and token-bucket API rate limiting in place

## 1. Identity and Access

- Authenticate users with password credentials and issue short-lived access tokens.
- Hash passwords with a modern adaptive password hashing algorithm (BCrypt); never store plaintext passwords.
- Enforce RBAC for `ADMIN`, `HR`, `MANAGER`, `SUPERVISOR`, and `EMPLOYEE`.
- Enforce organization and resource ownership checks in addition to role checks.
- Deny by default and reject self-approval.
- Deactivate users and revoke or expire access according to account policy.

Tenant isolation: shared-schema multi-tenancy with an `organization_id` discriminator on every tenant-owned table. `TenantContextFilter` resolves the authenticated user's organization into a per-request `TenantContext` that persistence-layer reads and writes are scoped to; the MVP resolves every account to the seeded default organization. See [DATABASE-DESIGN.md §4](DATABASE-DESIGN.md).

Implemented: the access token is a bearer token signed with HMAC-SHA256 over `base64url(username:expiry)` using `WORKFORCE_AUTH_SIGNING_KEY`, valid for 1 hour, re-validated against the stored (BCrypt) account on every request. Refresh tokens, rotation, and expulsion are not yet implemented. JWT adoption, key storage, issuer, and exact lifetimes remain decisions to be documented before production.

## 2. API Protection

- Require HTTPS outside local development.
- Validate request bodies, query parameters, path variables, content types, and size limits.
- Use secure response headers and a restrictive CORS policy.
- Return generic authentication errors and standardized safe error responses.
- Tune and validate rate limiting for authentication and public-facing endpoints; per-client-IP limits are enforced by the `RateLimitFilter` token bucket (`workforce.api-rate-limit.*`), returning HTTP 429 with `Retry-After`, and should be layered behind an edge proxy in production.
- Treat integration adapters as untrusted boundaries: outbound adapters are disabled until credentials are configured, and the inbound biometric clock-event endpoint stays disabled (`workforce.integrations.biometric.enabled=false`) and requires authentication and optional geofence validation.
- Use idempotency keys for attendance and other retry-sensitive commands.

## 3. Data Protection

- Encrypt traffic in transit and protect database backups at rest.
- Keep secrets in a secret manager or deployment secret store, never in source control.
- Minimize personal data in logs and audit payloads.
- Restrict database credentials by environment and least privilege.
- Define retention, anonymization, and access procedures for personal, attendance, and audit data.

## 4. Audit and Monitoring

Audit actor, action, resource, resource ID, timestamp, source address where appropriate, and before/after values where policy permits. Audit records are append-only. Alert on repeated authentication failures, privilege changes, unusual approval activity, and access-control failures.

## 5. Secure Engineering

Dependency scanning, secret scanning, static analysis, migration review, code review, and security regression tests are required in CI. Threat modeling must cover roster manipulation, duplicate attendance, unauthorized approval, tenant escape, token theft, event replay, and sensitive data disclosure.

## 6. AI and Risk Analysis

- LLM provider credentials (`workforce.risk.ai-*` base URL, model, and API key) are server-side configuration only and are never exposed to the frontend or in responses.
- The optional LLM call is an outbound adapter behind `RiskAdvisorPort`; it is invoked server-side with a timebound HTTP client (timeout enforced, default 15 s) and falls back to the offline heuristic advisor on any failure or when unconfigured (`ai-provider` defaults to `none`).
- Risk analysis uses replayed snapshots of roster, attendance, leave, overtime, and swap data; feed nothing more than the operational slices required by the rules and placeholders rather than full personal profiles.
- Do not log prompt payloads or AI responses verbatim; mask names and identifiers in analysis text as required by data-handling policy.

## 7. Incident Response

Define owners and runbooks for credential compromise, data exposure, unauthorized access, database failure, and event backlog. Preserve relevant logs, contain access, rotate credentials, assess scope, communicate according to policy, and record corrective actions.

## 8. Security Acceptance Gate

No release is production-ready until authentication, authorization, tenant isolation, input validation, secret handling, dependency vulnerabilities, audit coverage, and failure responses have documented test evidence.
