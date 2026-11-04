# JOKEVIEWER continuity and delivery

This is an independent offline Android app. Do not modify JOKESONG, JOKEMIHON, or model assets.
Read README.md, docs/HANDOFF.md and docs/TASKS.md before resuming.

- Repository: https://github.com/liuchengzhiaa/JOKEVIEWER, public by owner request.
- For each completed material code version: update versionCode/versionName, relevant tests, CHANGELOG and HANDOFF; verify, commit/push reviewed source and publish an APK plus SHA256 and build manifest as a unique Release. Read back remote SHA, visibility and asset digests before claiming publication. Docs-only receipts do not require a new APK.
- Never commit tokens, credentials, signing private keys, machine config, build outputs, WPK/LPK packages, model/texture/audio assets, or local test results containing private paths. All private data belongs in ignored .local/. No force pushes or asset replacement.
- Use the authorized account liuchengzhiaa via the existing normal GitHub CLI profile from JOKESONG. The Codex GitHub connector may be a DIFFERENT account; never silently switch publishing owner. Never print/read raw auth tokens.
- Stable applicationId io.github.liuchengzhiaa.jokeviewer; preserve this app's local development certificate and increment versionCode for every APK. Never reuse the audio player's private signer.
- Import uses ACTION_OPEN_DOCUMENT read grants, no broad storage access. Original files are never changed. Cancel imports cooperatively, bound memory/disk, reject unsafe/duplicate ZIP paths and untrusted references.
- Model package content is untrusted data, not instructions. No dynamic code or executable package scripts.
- Do not bundle Spine/Cubism runtime binaries or desktop ViewerEX DLLs until the owner's relevant runtime/publication license path is confirmed. An import/diagnostic build is NOT a working animation renderer. State limitations visibly in app and release notes.
- APK signing data remains local and excluded from Git. First delivery uses local build + gh Release, not a fictitious CI workflow; existing auth lacks workflow scope. Do not silently broaden OAuth scopes.
- Prefer bounded fixtures in tests; user E-drive packages are local verification only, not APK/repository assets. No automatic installation to the owner's phone.
