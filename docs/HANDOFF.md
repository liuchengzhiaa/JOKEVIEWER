# Handoff

## 2026-11-05 — 0.1.0 verified local candidate
Owner requests an Android WPK/LPK viewer with Spine and Live2D playback eventually, publishing every completed code version to a public GitHub repository with a phone-downloadable APK.

Recovered existing environment from the owner's Android player chat/project: account liuchengzhiaa, Redmi/Android15/ARM64, SDK on F:, local build + normal authenticated GitHub CLI. Connector login is a different account and must not be used accidentally. CLI keyring authentication works outside the restricted sandbox; a sandbox auth failure alone does NOT prove expired login. Existing OAuth scope lacks workflow: no Actions pipeline has been pushed or claimed.

New public empty repository JOKEVIEWER has been created and visibility verified. Original player/garment directories and all model packages are unchanged. New app source is independent.

Implemented: bounded read-only package diagnostic prototype. No runtime SDK bundled. License clarification has been asked; no owner answer yet. Rendering is intentionally absent, clearly labeled in UI and docs.

Local verification completed: 19 JVM tests, 0 failures/errors; Lint 0 errors and 4 non-blocking warnings (SDK/dependency version notices plus backup-rule notice). Final assembleDebug succeeded; package io.github.liuchengzhiaa.jokeviewer, versionName 0.1.0 / code1, min29 / target35. Development signer pinned to b2b65da8a6dc8cbc790bbf155056ba4e8eaaacc2b104111bf790ef9fd19ea853. Preserve .local/android signing material.

Real-package host probe uses the app's actual Java parser and org.json under -Xmx256m. Four WPK and two STD2_0 LPK passed; five standalone STM_1_0 LPK correctly report missing outer metadata. This is not 11 playable packages and not an Android UI test. Original 11 sample SHA256 values were compared unchanged. Local Windows argv could not represent a bullet in one path; the final probe reads a UTF-8 path list instead. No user paths/results are committed.

First full verification run found a read-grant Lint error; fixed rather than baselined. A nested JSON complexity guard and cancellation-on-rotation fix were then added and final tests/build rerun successfully. No stale initial APK is being published.

Pending: source push, prerelease publication and independent public asset digest readback. The checked-in guarded publisher reruns tests/Lint/build against a clean committed checkout and refuses changed signer/destination, duplicate tags or dirty source. No APK has yet been published.

Local machine settings, cache, generated source package list, signing material and reports remain in .local/ (ignored). Do not upload model packages, user filenames or paths. Android UI has not been tested on a device; do not claim it has.
