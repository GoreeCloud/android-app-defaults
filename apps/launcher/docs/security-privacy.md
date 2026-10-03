# Security and privacy

The launcher is high-trust software because it becomes the HOME surface and can enumerate launchable apps. Its default architecture therefore keeps app inventory, layout, folders, local search data and usage-derived suggestions on-device.

No advertising, attribution, sponsorship or engagement SDK is included. Milestone 0 requested no `INTERNET` permission. The current PR #248 Development candidate now declares `INTERNET` only for explicitly enabled connected Search adapters such as the authorized Google Drive metadata provider. Core Home, Apps, settings, and local Search remain offline-capable; cleartext traffic is disabled and provider opt-in/authorization remains the transmission boundary.

Do not add Accessibility Service or device-admin privileges merely to imitate privileged launcher behavior. Imported themes/backups are untrusted input.

Signing keys and passwords remain outside source control. The Development build can consume a complete externally supplied `GOREECLOUD_DEV_KEYSTORE_*` environment configuration and fails closed on partial configuration. Ordinary PR CI intentionally continues to use the CI debug key and labels its artifact as installability-only; a persistent Development key must not be injected into untrusted PR execution. Protected key provisioning, trusted distribution workflow protection, and representative-device update-in-place verification remain separate security/acceptance gates.

## Launcher App Lock

App Lock protects only launch attempts that pass through GoreeCloud Launcher. It is intentionally not presented as Android-wide package enforcement. Other launchers, notifications, links, recents, Settings, and system surfaces retain their own Android authority.

The current Development design uses one local credential for all Launcher-locked apps. A valid PIN is 4–6 digits; a valid pattern contains 4–9 distinct dots. Raw credential material is not stored. Launcher persists only a random salt, a PBKDF2-HMAC-SHA256 verifier, profile-qualified app keys, failed-attempt count, and cooldown deadline in the app-private `launcher_app_lock` Preferences DataStore.

Five failed verification attempts trigger a 30-second persisted cooldown. Unlocking an app, changing the credential, or disabling App Lock requires successful verification. Locking a new app after App Lock is configured does not require re-entering the credential.

Launcher’s Android backup rules explicitly include only shared preferences and databases; the Preferences DataStore file lives outside that allowlist. App Lock state is also deliberately absent from the portable Launcher preference/restore model. This avoids copying a local credential verifier or stale lock identities to another installation without a separately designed encrypted recovery flow.

App Lock does not add `AccessibilityService`, device-admin, overlay, usage-access, biometric, account, network, or background execution authority.

