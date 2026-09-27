# FixFirst — Community Issue Prioritization System

A beginner-friendly Java full stack campus maintenance project. Students report issues,
support existing reports and verify repairs. Administrators assign technicians, manage
assets and deadlines, see maintenance analytics, and override a score with a reason.

## 1. What you need

- JDK 17 (both `java -version` and `javac -version` should work).
- MySQL Server 8.4 running locally on port 3306.
- VS Code. Extension Pack for Java and Spring Boot Extension Pack are useful, but the terminal commands work without them.
- Internet for the first Maven run to download dependencies. The Maven Wrapper is included; no separate Maven installation is required.

This project uses Spring Boot 4.1.1, Spring Security, Spring Data JPA, MySQL, and plain HTML/CSS/JavaScript. No Node.js, frontend build or paid API is needed.

## 2. Extract and open

Extract the ZIP. Open the `fixfirst` folder (the folder containing `pom.xml`) in VS Code.
For example: `D:\new project\fixfirst`.

## 3. Create the MySQL database and application account

In PowerShell, connect using your existing MySQL root password:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p
```

At `mysql>`, execute the following. Replace `CHANGE_TO_YOUR_OWN_DB_PASSWORD` with a strong password you choose before running it. Use a password without single quotes or backslashes to avoid SQL escaping confusion. Keep it private.

```sql
CREATE DATABASE IF NOT EXISTS fixfirst CHARACTER SET utf8mb4;
CREATE USER 'fixfirst_app'@'localhost' IDENTIFIED BY 'CHANGE_TO_YOUR_OWN_DB_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON fixfirst.* TO 'fixfirst_app'@'localhost';
exit;
```

This is a local development account with schema creation permissions. It does not receive global privileges or access to unrelated databases. If the account already exists, do not repeatedly run `CREATE USER`; use the password you previously set.

## 4. Run on Windows

In the VS Code PowerShell terminal, from the folder containing `pom.xml`:

```powershell
.\START-FIXFIRST.cmd
```

The launcher asks for:
1. The `fixfirst_app` database password you set in step 3.
2. A new FixFirst admin password (at least 12 characters, at most 72 UTF-8 bytes).

The admin login email defaults to `admin@fixfirst.local`. This is an application login, different from the MySQL root/database accounts. Password input is masked. No passwords are stored in project files. The launcher sets credentials only for its process and child Java process.

Wait for `Started FixFirstApplication`, then open **http://localhost:8080**.
Keep the terminal open. Stop the app with Ctrl+C. The first run can take several minutes.

Use the same admin password on later starts. The startup initializer creates the admin only if absent; it does not reset an existing password. To choose an email before first start, set `$env:ADMIN_EMAIL='your-admin@example.com'`.

The CMD launcher uses PowerShell's process-only execution-policy option to run the included local startup script. It does not change your saved Windows policy. If your institution blocks scripts, use the manual method below.

### Manual alternative

```powershell
$env:DB_USER='fixfirst_app'
$secret=Read-Host 'Database password' -AsSecureString
$env:DB_PASSWORD=[System.Net.NetworkCredential]::new('', $secret).Password
$secret=Read-Host 'FixFirst admin password (12+ characters)' -AsSecureString
$env:ADMIN_PASSWORD=[System.Net.NetworkCredential]::new('', $secret).Password
.\mvnw.cmd spring-boot:run
```

After stopping, remove variables with `Remove-Item Env:DB_PASSWORD,Env:ADMIN_PASSWORD`.

### Optional temporary demo without MySQL

```powershell
.\START-FIXFIRST.cmd -Demo
```

This uses an in-memory H2 database. **All demo data disappears when the app stops.** It is separate from MySQL data. You still set an admin password; student accounts start empty. The demo is for trying the workflow, not for preserving your work.

## 5. Try the three-role workflow

1. Click **Create a student account**, register and sign in.
2. Report “Water purifier not working”, category Water, location “Block A”, equipment “Purifier A-01”. The reporter counts as supporter 1: score 40 + 2 + 0 = 42.
3. In a private/incognito window, register a second student and support the report. Score becomes 44. A second vote by the same account is rejected.
4. Sign in as `admin@fixfirst.local` with the admin password you chose. Use **Create technician** to create a TECHNICIAN account; public registration always creates STUDENT accounts.
5. As admin, open an active issue, assign or reassign the technician, optionally set an asset and deadline, and use the notification bell.
6. Sign in as the technician. The board becomes **My assigned tasks**. Add a work note, optional repair cost, and after photo before starting or resolving work.
7. As the original reporter, open the issue under **All statuses** or **Resolved**. Confirm the fix or reopen it with a note. Reopening preserves assignment and deadline.
8. Use the notification bell to mark individual notifications or all notifications read. Polling runs every 30 seconds only while signed in.
9. Admin can open **Maintenance analytics** for date-bounded counts, first-resolution time, reopen rate, overdue active issues, repair cost and replacement review suggestions.

## 6. Priority rules

| Factor | Points |
|---|---|
| Water or Electrical category | 40 |
| Sanitation or Accessibility category | 30 |
| Equipment category | 20 |
| Other category | 10 |
| Supporting accounts, including reporter | 2 each, capped at 30 |
| Full days since report | 3 per day, capped at 30 |

Calculated score = category + support + age, maximum 100.
High: 70–100; Medium: 40–69; Low: 0–39. Ties show older reports first.
A staff override changes the effective score, keeping the calculated breakdown visible.
Age freezes at resolution and resumes from the original report date on reopening.
Scores are recalculated when the board is fetched/refreshed; there is no background notification service.
These weights are teaching assumptions that require campus validation, not proven emergency-triage rules.

## 7. Deadlines, photos, assets and analytics

New issues receive a deadline from their initial effective score: HIGH 24 hours, MEDIUM
72 hours, and LOW 7 days. Later votes and score changes do not recalculate it. Existing
rows may have a null deadline and are shown as having no deadline until an administrator
sets one with a reason. Timestamps are stored as UTC `Instant` values and formatted in
the browser's local timezone. Active issues whose deadline is strictly in the past are
overdue; resolved and closed issues are never overdue.

Before photos are optional for reporters. After photos are optional for the assigned
technician or an administrator. Files are checked by content signature, limited to JPEG,
PNG or WebP, limited to 5 MB each and three per before/after kind, renamed with UUIDs,
and stored in `FIXFIRST_PHOTO_DIR` (default `./fixfirst-photos`), outside the static web
folder. Back up that directory together with the database; deleting an issue does not
currently delete files automatically, so periodically remove orphaned files after a
database backup.

Assets use a stable identifier, location, category and optional INR replacement estimate.
Repair cost uses `BigDecimal` and is recorded when repair work is resolved. Replacement
review is only a human recommendation: the default trigger is at least 3 resolved repairs
in the last 183 days or recorded cost at least 60% of the estimate. Configure these with
`FIXFIRST_REPAIR_COUNT_THRESHOLD` and `FIXFIRST_REPAIR_COST_RATIO`.

Analytics uses report `createdAt` for date filtering and category/status counts, the first
non-null `firstResolvedAt` for average report-to-first-resolution, any positive
`reopenCount` for reopen rate, active status plus deadline for overdue count, and recorded
`repairCost` for total cost. Empty sets return zero values rather than dividing by zero.

## 8. Project map — read in this order

| File | Responsibility |
|---|---|
| `src/main/resources/static/index.html` | Forms, dashboard and dialogs |
| `src/main/resources/static/style.css` | Responsive appearance |
| `src/main/resources/static/app.js` | Calls APIs and updates the page |
| `src/main/java/com/fixfirst/IssueController.java` | Receives API requests and validates input |
| `src/main/java/com/fixfirst/IssueService.java` | Reporting, support, transitions, verification and history |
| `src/main/java/com/fixfirst/PriorityCalculator.java` | Simple scoring rules |
| `src/main/java/com/fixfirst/Issue.java` | Issue data mapped to database tables |
| `src/main/java/com/fixfirst/IssueRepository.java` | Database access and write locking |
| `src/main/java/com/fixfirst/NotificationService.java` | Owned notifications and duplicate event keys |
| `src/main/java/com/fixfirst/PhotoController.java` | Validated authenticated photo storage and serving |
| `src/main/java/com/fixfirst/CampusAsset.java` | Asset and replacement-cost metadata |
| `src/main/java/com/fixfirst/AccountController.java` | Registration and current-user details |
| `src/main/java/com/fixfirst/SecurityConfig.java` | Login, role permissions and CSRF protection |
| `src/main/java/com/fixfirst/AdminBootstrap.java` | Creates the first admin account |
| `src/main/resources/application.properties` | MySQL/environment settings |

Request flow: Browser → Controller → Service → Repository → MySQL.
The response returns as JSON; JavaScript renders it on the page.
Tables: `app_users`, `issues`, `issue_supports`, `issue_history`, `notifications`, `issue_photos`, and `campus_assets`.
We use explicit Java fields and constructors, without Lombok, to keep the example readable.
Database entities are not returned directly: response records keep password hashes and private account data out of API results.

## 9. API routes

All writes require the CSRF token returned by `GET /api/csrf` in its named header.

| Method | Route | Access |
|---|---|---|
| POST | `/api/register` | Public; always creates STUDENT |
| POST | `/api/login` | Form fields `username`, `password` |
| POST | `/api/logout` | Logged-in session |
| GET | `/api/me` | Logged-in session |
| GET / POST | `/api/issues` | Logged-in session |
| POST | `/api/issues/{id}/support` | Logged-in session, one vote per account |
| POST | `/api/admin/issues/{id}/status` | ADMIN |
| POST | `/api/admin/issues/{id}/priority` | ADMIN |
| POST | `/api/issues/{id}/feedback` | Original reporter only |
| GET | `/api/notifications` | Current account only |
| POST | `/api/notifications/{id}/read`, `/api/notifications/read-all` | Current account only |
| POST | `/api/admin/technicians` | ADMIN; creates TECHNICIAN |
| POST | `/api/admin/issues/{id}/assignment` | ADMIN |
| POST | `/api/issues/{id}/status` | Assigned TECHNICIAN or ADMIN |
| POST | `/api/issues/{id}/photos` | Reporter before; assigned technician/admin after |
| GET | `/api/admin/analytics` | ADMIN |
| GET / POST / PUT | `/api/admin/assets` | ADMIN |

Workflow: OPEN → IN_PROGRESS → RESOLVED → CLOSED.
Only the assigned technician or an admin can move OPEN to IN_PROGRESS or IN_PROGRESS to
RESOLVED, and every such transition requires a work note. Only the reporter can verify or
reopen a resolved issue. Existing MySQL data is preserved: `spring.jpa.hibernate.ddl-auto=update`
adds nullable columns and new tables. Take a database backup before starting the updated
application and review generated DDL in a staging copy before production use. No drop/reset
migration is included. H2 demo uses `create-drop` and is disposable.

## 10. Tests

```powershell
.\mvnw.cmd test
```

Unit tests cover point caps, full-day calculation, future timestamps, resolved ageing,
overrides and priority boundaries. `tests/workflow_check.py` is a separate HTTP integration
check for a fresh demo server; it uses disposable accounts and issues. Run it only against
a demo instance, with matching `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables:

```powershell
python tests/workflow_check.py
```

See `VALIDATION.md` for the checks actually run during delivery.

## 11. Troubleshooting

- **Access denied for fixfirst_app:** Check the database password entered in the launcher and whether the account exists.
- **Unknown database fixfirst:** Run `CREATE DATABASE IF NOT EXISTS fixfirst;` as root.
- **Communications link failure:** Run `Get-Service MySQL84`; start it from Windows Services if stopped. Confirm port 3306.
- **Port 8080 already in use:** Stop the earlier FixFirst terminal, or set `$env:SERVER_PORT='8081'` before starting and open port 8081.
- **JAVA_HOME error:** Set JAVA_HOME to your JDK directory, not its `bin` directory; reopen VS Code afterward.
- **Maven download/network error:** Verify internet access and retry. Corporate proxies may require your organization's Maven settings.
- **Whitelabel error / opening HTML directly:** Use http://localhost:8080, not Live Server or a file URL.
- **You cannot sign in as admin after changing startup password:** Existing admin passwords are not overwritten at restart. Use the original one.
- **An issue disappears after marking resolved:** Change the default Active filter to All statuses or Resolved.

## 12. Scope and next improvements

This is a local learning project, not a production campus service. It binds to 127.0.0.1.
It includes password hashing, session login, CSRF protection, server-side role checks,
validation, one-vote constraints, per-issue write locks, deadline tracking, notification
ownership, and authenticated media access.
Student emails are not verified, so one account is not proof of one real student.
Matching uses normalized exact category/location/equipment fields; it is a suggestion,
not AI text matching or an enforced duplicate ban. Recurrence counts previous reports,
not independently confirmed failures. Email alerts, password recovery, verified college
identities, rate limiting, pagination and managed schema migrations are future work. This
first version is sized for a small demonstration; recurrence and analytics currently load
issue rows in memory and should use aggregate repository queries for a large deployment.

Emergency faults must be reported through real campus emergency channels.
