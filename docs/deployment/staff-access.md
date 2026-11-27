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
cookie. CSRF is disabled because every protected request authenticates through its header rather
than ambient browser credentials. `/actuator/health` is deliberately public and returns only
aggregate status; every other route is denied unless explicitly protected and authorized.

HTTP Basic encodes but does not encrypt credentials. Repository demonstrations bind to loopback
HTTP. Any remote deployment must terminate and verify HTTPS before forwarding requests to this
application. This code does not configure certificates, a reverse proxy, rate limiting, account
lockout, multi-user authorization or automated secret rotation.

The ELS-45 packaging/runbook owner should provision `ELS_STAFF_USERNAME` and
`ELS_STAFF_PASSWORD` through the selected private runtime mechanism and use the exact public
`/actuator/health` path for health checking. No credential belongs in an image, seed, command
line, URL, committed `.env`, example or generated evidence file.
