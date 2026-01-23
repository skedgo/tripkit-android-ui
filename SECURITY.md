# Security Policy

## Supported Versions

We release patches for security vulnerabilities in the following versions:

| Version | Supported |
| ------- | ---------- |
| 2.x (current AndroidX/Jetpack releases from `main`) | :white_check_mark: |
| 1.x (legacy support branch) | :warning: Best-effort fixes only |
| < 1.0 | :x: |

> **Note:** TripKit Android UI is distributed through JitPack. Update this table whenever a new minor stream becomes the actively supported release line.

---

## Reporting a Vulnerability

We take the security of our software seriously. If you believe you have found a security vulnerability, please report it to us as described below.

### How to Report

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please email us at **[security@skedgo.com](mailto:security@skedgo.com)**.

You should receive a response within 48 hours. If for some reason you do not, please follow up via email to ensure we received your original message.

### What to Include

Please include the following information in your report:

- Type of issue (e.g., buffer overflow, SQL injection, cross-site scripting, etc.)
- Full paths of source file(s) related to the manifestation of the issue
- The location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit it

This information will help us triage your report more quickly.

### What to Expect

After you submit a report, we will:

1. **Acknowledge** your email within 48 hours
2. **Investigate** the issue and confirm the vulnerability
3. **Keep you informed** of our progress toward a fix
4. **Release** a security patch as appropriate
5. **Credit** you in our release notes (if you wish to be named)

---

## Security Best Practices for Contributors

If you're contributing to this project, please follow these security guidelines:

### Code Review
- All code changes must go through Pull Requests
- PRs require approval from at least one maintainer before merging
- No direct commits to `main` or protected branches

### Dependencies
- Keep Gradle dependencies up to date (Android Gradle Plugin, Kotlin, Jetpack Compose/AndroidX, Google Maps, etc.)
- Review dependency changes for known vulnerabilities (GitHub Dependabot alerts are enabled)
- Maintain alignment between `minSdkVersion`, `targetSdkVersion`, and Google Play requirements
- Use automated tools like Dependabot and Android Lint reports in CI to monitor security issues

### Secrets Management
- **Never** commit TripKit API keys, Google Maps keys, keystore files, or other credentials
- Sample apps should use placeholder keys or local `gradle.properties`, not checked-in secrets
- Use CI/CD secrets (GitHub Actions, Bitrise, etc.) or secure vaults (AWS SSM, GitHub Secrets) for publishing credentials
- Review commits for accidentally included secrets before pushing

### Secure Coding
- Follow [OWASP Top 10](https://owasp.org/www-project-top-ten/) best practices
- Validate and sanitize all inputs exposed by TripKit UI components (deep links, intents, form fields)
- Avoid storing sensitive data in UI state; rely on encrypted storage provided by host apps
- Implement proper authentication and authorization flows when embedding TripKit services (OAuth tokens, API keys)
- Use HTTPS/TLS ≥ 1.2 for all network communications and honor Android `networkSecurityConfig`

### Android UI Library Considerations
- Respect Android permission scopes; UI components must not request permissions directly unless required
- Keep theming resources compatible with Material You / dynamic color and support dark mode
- Harden WebView usage (disable JavaScript/file access unless explicitly required)
- Ensure obfuscation (R8/ProGuard) rules cover TripKit UI modules when shipping AARs
- Verify multi-module consumers cannot access debug-only tooling from release artifacts

---

## Security Features

This project includes the following security measures:

- **Dependabot alerts** enabled for vulnerable dependencies
- **Secret scanning** enabled to prevent credential leaks
- **GitHub Actions `CI` workflow** (android_ci.yml) runs unit tests on PRs targeting `develop`/`feature/**`
- **Code review** required for all changes
- **Branch protection** rules enforced on `main/develop`

---

## Disclosure Policy

We follow a **coordinated disclosure** approach:

1. Security issues are privately investigated and patched
2. A security advisory is prepared but not published
3. We notify relevant parties (e.g., major users, downstream projects)
4. A patch release is made available
5. The security advisory is published after users have had time to update

We aim to complete this process within 90 days of the initial report, though complex issues may take longer.

---

## Security Update Policy

Security updates are released as:
- **Patch versions** (x.x.X) for currently supported versions
- **Security advisories** published on our GitHub Security Advisories page
- **Release notes** clearly marking security-related changes

---

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE Top 25 Most Dangerous Software Weaknesses](https://cwe.mitre.org/top25/)
- [GitHub Security Best Practices](https://docs.github.com/en/code-security)

---

## Contact

For general security questions or concerns, please contact:
- **Email:** [security@skedgo.com](mailto:security@skedgo.com)

---

> **Last Updated:** 2025-11-14  
> **Version:** 1.1  
> 
> This security policy is maintained by the repository maintainers and reviewed regularly.
