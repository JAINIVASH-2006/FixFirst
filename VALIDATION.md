# Validation report

Validated on 26 September 2026 with Java 17 and Spring Boot 4.1.1.

- Maven compilation, unit tests and executable JAR packaging: passed.
- Four scoring test methods: passed, no failures or errors.
- Six Maven unit tests: priority rules plus HIGH/MEDIUM/LOW deadline policy and legacy null-deadline handling passed.
- HTTP integration checks against the packaged app with H2 in MySQL compatibility mode: passed.
- Checks: registration, duplicate email, session login/logout, CSRF rejection, student/admin access rules, input validation, duplicate support prevention, point calculations, override/reset, allowed status transitions, reporter-only verification, reopening and closure, normalized recurrence counting, technician creation and assignment, technician task visibility, technician-only status access, deadline preservation on reopen, notification ownership, before/after image uploads, asset creation and analytics.
- Headless Chromium checks: login, dashboard rendering, issue detail dialog, matching-report suggestion, search empty state and logout passed, with no JavaScript page errors.
- Responsive inspection: desktop 1440px and mobile 390px screenshots inspected. No horizontal document overflow at 390px.

Limits: MySQL 8.4 connectivity and DDL were not run against a live MySQL server in this test environment. The Windows launcher was reviewed but not executed on Windows. Node.js is not installed, so standalone `node --check` was unavailable; the browser JavaScript was exercised by the HTTP/static application workflow. These are the next environment checks to perform on your PC. The included source package requires internet for first-run Maven downloads.

Preview screenshots use disposable test reports and accounts. They are not preloaded into your real database.
