# Release process

Every completed code version must have a unique versionName/tag, increased versionCode, verified source commit and an APK the owner can download without signing into GitHub. The target is public liuchengzhiaa/JOKEVIEWER; do not upload private packages, signing keys or credentials.

This project initially reuses the owner's normal GitHub CLI + LOCAL build/Release process, NOT GitHub Actions. The current authorized CLI profile lacks workflow scope. No account scopes are changed automatically. Do not claim a CI pipeline exists.

## Local setup
- JDK17, Android SDK37/build-tools, Gradle9.6.1; set sdk.dir in ignored local.properties or ANDROID_HOME.
- Set GH_CONFIG_DIR to the owner's existing secure CLI profile if needed. Never print tokens or copy auth caches into this repository.
- The generated development signing key resides in ignored .local/android; preserve it for update compatibility. The public certificate fingerprint is pinned in tools/release-signing-certificate.sha256. Never reuse another app's private signer.
- The diagnostic APK is development-signed, not a Play Store production build.

## Reviewed delivery
1. Update version and CHANGELOG/HANDOFF/TASKS. Review exact source/test/docs files. Exclude .local, user assets and APKs. Commit source; the publisher rejects dirty checkouts.
2. Run tools/publish-release.ps1 with optional -GhCli and -GradleCli executable paths. -PlanOnly performs local planning only.
3. The publisher checks the authenticated owner/destination, public visibility, uniqueness, incremented versionCode, unit tests, Lint, committed source, APK identity and pinned signer. It pushes without rewriting history, reads back the branch SHA, tags that exact commit, creates a draft prerelease, uploads three assets and verifies their remote SHA256/size before publication.
4. It publishes as a prerelease, not latest/stable, then reads assets back through the anonymous public API. Share the version-specific browser_download_url, not a guessed latest URL.
5. A documentation-only follow-up commit records the receipt without rebuilding or moving the code tag. If anything fails, preserve the current remote state, inspect missing steps and never overwrite assets or force-push.

0.1.0 has no Spine/Cubism SDK and no model animation playback. Its first round of owner acceptance is SAF import, package diagnostics, correct metadata-required warnings, read-only behavior and cancellation. Host parser tests are not phone/device UI tests. Runtime license confirmation is pending.
