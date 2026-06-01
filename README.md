# Adaptive Multi-Factor Authentication Security Gateway for CPS Operator Workstations

A university **Cyber Security in Cyber-Physical Systems** demo. It implements an MFA *security
gateway* that brokers operator access to a simulated **HMI/SCADA workstation**. Operators never
reach the process dashboard directly — they must pass credential verification, a **risk-based MFA**
challenge, session creation, and (for critical control actions) **step-up MFA**. Every security
event is audited, anomalies raise alerts and incidents, and the trail can be exported in
SIEM-friendly JSON/CSV.

> ⚠️ **Demo scope.** Email/SMS delivery and the WebAuthn/FIDO2 cryptographic ceremony are *mocked*
> but structurally real (clearly marked `MOCK` in code and isolated behind services so a real
> provider drops in cleanly). This is built to teach CPS access-control concepts, not for production.

---

## 1. Architecture

```
 React SPA  ──HTTP + Bearer token──▶  Spring Boot MFA Gateway  ──JPA──▶  PostgreSQL
 (Vite)                               ┌───────────────────────────────┐
 login → risk → MFA → HMI/Admin       │ auth · risk · mfa · session    │
                                      │ rbac · hmi/cps · stepup        │
                                      │ audit · incident · siem · notify│
                                      └───────────────────────────────┘
```

* **Server-side opaque session tokens** (not JWT). The raw token is sent as
  `Authorization: Bearer …`; only its SHA-256 hash is stored, so sessions can be tracked,
  idle/absolute-timed and force-terminated.
* **Login is a state machine.** `POST /auth/login` checks the password, runs the risk engine, and
  issues a `PENDING_MFA` session. A `PENDING_MFA` token can *only* reach MFA endpoints. Successful
  MFA promotes it to `ACTIVE`.
* **Step-up MFA** stamps a short validity window on the session; critical HMI endpoints return
  `requiresStepUp: true` until it is satisfied.

### Tech stack
| Layer | Tech |
|------|------|
| Frontend | React 18, React Router 6, Axios, Recharts, hand-rolled CSS (SCADA theme) |
| Backend | Spring Boot 3.2, Spring Security 6, JPA/Hibernate, BCrypt |
| DB | PostgreSQL 16 (or H2 in-memory for a zero-infra run) |

---

## 2. Prerequisites

* **JDK 17 or 21 (LTS recommended).** Use a supported LTS — Lombok's annotation processor does not
  yet run on JDK 23/24, which will cause "cannot find symbol" compile errors. JDK 21 is verified.
* **Maven** is *optional* — the repo ships the **Maven Wrapper** (`./mvnw` / `mvnw.cmd`), which
  downloads Maven 3.9.9 automatically. (System Maven 3.9+ also works.)
* **Node.js 18+** and npm
* **PostgreSQL** — easiest via Docker (`docker compose up -d`). Or skip it and use the `h2` profile.

---

## 3. Running the backend

### Option A — PostgreSQL (default)
```bash
# from the repo root
docker compose up -d            # starts PostgreSQL on :5432 (db/user/pass all 'cps_mfa')

cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run  (or use system 'mvn')
```

### Option B — H2 in-memory (no database to install)
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
# H2 console: http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:cps_mfa, user: sa)
```

The backend starts on **http://localhost:8080**, auto-creates the schema and **seeds demo data**
on first boot. Environment variables (all optional) are documented in
[`backend/.env.example`](backend/.env.example).

> 💡 **Where do the OTPs appear?** Email/SMS are mocked — the generated 6-digit code is printed to
> the **backend console**, e.g. `>>> [DEMO] EMAIL OTP for operator1 = 481920`.

## 4. Running the frontend
```bash
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173**. The Vite dev server proxies `/api` to the backend on `:8080`.

---

## 5. Demo credentials

All demo users share the password **`Password123!`**.

| Username | Role | Can do |
|----------|------|--------|
| `viewer1` | VIEWER | View HMI process values only |
| `operator1` | OPERATOR | View HMI, start/stop pump, acknowledge alarms |
| `engineer1` | ENGINEER | Operator + change motor speed / pressure / temperature (step-up MFA) |
| `admin1` | ADMIN | Manage users, sessions, view audit & incidents (always strong MFA) |
| `security1` | SECURITY_OFFICER | View audit, investigate incidents, export SIEM logs |

---

## 6. Risk engine

Each login is scored (0–100). The band selects the required MFA method:

| Score | Level | Required MFA |
|------:|-------|--------------|
| 0–30 | LOW | password + **Email OTP** |
| 31–60 | MEDIUM | password + **SMS OTP** |
| 61–80 | HIGH | password + **WebAuthn/FIDO2** |
| 81–100 | CRITICAL | **blocked** + admin notified + incident created |

Contributing rules: new device `+25`, unknown IP `+25`, off-hours `+15`, ADMIN role `+20`,
ENGINEER role `+10`, prior failed password `+20`, prior failed MFA `+30`, concurrent sessions `+20`.
**ADMIN always requires WebAuthn**, regardless of score.

> The login screen has **demo risk toggles** (new device / unknown IP / outside hours) so you can
> deliberately push the score into HIGH or CRITICAL during a presentation.

---

## 7. Guided demo flows

**Flow 1 — Normal operator login (LOW risk → Email OTP)**
Log in as `operator1` with no toggles → risk is LOW → Email OTP requested → read the code from the
backend console → HMI dashboard opens. `AUTH_LOGIN_SUCCESS` + `MFA_SUCCESS` + `SESSION_CREATED` are
audited.

**Flow 2 — High-risk engineer login (→ WebAuthn)**
Log in as `engineer1` and tick **New device** + **Unknown IP** (and/or off-hours) → score climbs
into HIGH → WebAuthn is required → complete the (mock) ceremony. A `HIGH_RISK_LOGIN` alert appears
on the admin dashboard.

**Flow 3 — Failed MFA → incident**
Log in as any user, enter the **wrong OTP 3 times**. MFA is blocked, the pending session is
terminated, and a `HIGH` incident *"Repeated MFA failures"* is auto-created (visible to admin1 /
security1).

**Flow 4 — Critical CPS action with step-up MFA**
As `engineer1`, go to **HMI → Engineering Setpoints** and change the motor speed. The gateway
returns `requiresStepUp`, routing you to the step-up page. Confirm with the strong authenticator;
the action then executes. Audit trail:
`CRITICAL_ACTION_REQUESTED → STEP_UP_MFA_REQUIRED → STEP_UP_MFA_SUCCESS → CPS_ACTION_EXECUTED`.

**Flow 5 — Unauthorized action attempt**
As `operator1`, the engineering setpoints are not shown — but if a setpoint request is issued
(e.g. via the API), RBAC denies it, logs `UNAUTHORIZED_ACTION_ATTEMPT`, raises an alert, and after
3 attempts opens an incident.

**Flow 6 — Session timeout**
Stay idle on the HMI for the idle window (default 5 min; background polling does **not** reset the
timer). The session monitor expires it, the next request returns `SESSION_EXPIRED`, and you are
logged out automatically. `SESSION_EXPIRED` is audited.

**Flow 7 — SIEM export**
As `security1`, open **Audit & SIEM** and download **JSON** or **CSV**. The export contains the
structured security events. `LOG_EXPORTED` is recorded.

---

## 8. Key API endpoints

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| MFA | `/api/mfa/email/{send,verify}`, `/api/mfa/sms/{send,verify}`, `/api/mfa/webauthn/{register,authenticate}/{start,finish}`, `/api/mfa/recovery/{verify,regenerate}` |
| HMI/CPS | `GET /api/hmi/status`, `POST /api/hmi/{start-pump,stop-pump,acknowledge-alarm,change-motor-speed,change-pressure-setpoint,change-temperature-setpoint,reset-emergency-stop}` |
| Step-up | `POST /api/step-up/{request,verify,execute-action}` |
| Admin | `GET /api/admin/dashboard`, `/api/admin/users` (GET/POST), `/api/admin/users/{id}/{lock,unlock}`, `/api/admin/sessions`, `/api/admin/sessions/{id}/terminate` |
| Audit | `GET /api/audit/logs`, `GET /api/audit/logs/{id}` |
| Incidents | `GET/POST /api/incidents`, `GET /api/incidents/{id}`, `PUT /api/incidents/{id}/{assign,status}` |
| SIEM | `GET /api/siem/export/{json,csv}` |

---

## 9. Project layout

```
backend/   Spring Boot gateway
  src/main/java/com/cps/mfa/
    auth/ mfa/ risk/ rbac/ session/ hmi/ stepup/
    notification/ audit/ incident/ siem/ user/ admin/ config/ common/
frontend/  React SPA (Vite)
  src/ pages/ components/ services/ context/ utils/ api/
docker-compose.yml   PostgreSQL for the demo
```

## 10. Security notes & mocked parts (read before grading)

* **Passwords, OTPs and recovery codes** are stored only as BCrypt hashes. OTPs are 6-digit,
  single-use, 5-min TTL, max 3 attempts, with a resend cooldown.
* **Session tokens** are stored only as SHA-256 hashes.
* **Mocked (clearly marked `MOCK` in code):** email/SMS delivery (printed to console) and the
  WebAuthn attestation/assertion verification. The WebAuthn challenge handling, credential storage
  and signature counters are real in shape — swap in Yubico `java-webauthn-server` without changing
  the endpoints or schema. Biometric login is the WebAuthn platform-authenticator path.
* This is a teaching demo: `ddl-auto` is `update`/`create-drop`, CSRF is disabled (token auth),
  and the H2 console is enabled under the `h2` profile. Do not deploy as-is.
