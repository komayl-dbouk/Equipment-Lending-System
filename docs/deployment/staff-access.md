# Staff Access and Transport Boundary

Every `/api/**` operation requires one staff HTTP Basic credential. Supply it only through the
private process environment:

```powershell
$env:ELS_STAFF_USERNAME = '<private-staff-name>'
$env:ELS_STAFF_PASSWORD = '<private-staff-secret>'
```

The application has no production default and fails startup when either value is missing or
blank. It hashes the configured password for in-memory comparison and does not provide a login,
registration, password-reset, student-account or role-management endpoint. Changing the
credential requires updating the private environment and restarting the process.

Missing and invalid credentials receive the same `401 UNAUTHORIZED` JSON response and Basic
challenge. Authentication is stateless: there is no application session or authentication
cookie. The current CLI/API delivery disables CSRF and accepts JSON writes; statelessness alone
does not make browser Basic authentication immune to CSRF. Browsers may cache Basic credentials.
Before browser delivery, reassess CSRF and origin policy with the frontend authentication design;
do not broaden JSON content types or CORS without that review. See [Spring's CSRF guidance](https://docs.spring.io/spring-security/reference/6.5/features/exploits/csrf.html).
`/actuator/health` is deliberately public and returns only
aggregate status; every other route is denied unless explicitly protected and authorized.

HTTP Basic encodes but does not encrypt credentials. Repository demonstrations bind to loopback
HTTP. Any remote deployment must terminate and verify HTTPS before forwarding requests to this
application. This code does not configure certificates, a reverse proxy, rate limiting, account
lockout, multi-user authorization or automated secret rotation.

Compose forwards `ELS_STAFF_USERNAME` and `ELS_STAFF_PASSWORD` from private configuration;
the guarded demo seed verifies the running configuration before using the credential. The exact
public `/actuator/health` path is used for readiness. No credential belongs in an image, seed
fixture, command line, URL, committed `.env`, example value or generated evidence file. Production
Hibernate SQL-exception detail logging is disabled because database errors can include student
emails; the application error handler retains sanitized exception-class diagnostics. Do not enable
SQL/bind/HTTP-header debug logging with real records or credentials.
