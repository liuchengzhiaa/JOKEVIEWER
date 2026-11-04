# Changelog

## 0.1.0 — 2026-11-05 (import/diagnostic preview)
- Independent Android project and public JOKEVIEWER repository.
- Read-only WPK/nested LPK import and observed STM_1_0/STD2_0 transforms.
- Config/model/motion/resource inspection; static cover preview.
- Archive bounds, unsafe-path rejection and cancellable import.
- No Spine/Live2D rendering SDK or animation playback in this version.
- Verification: 19 JVM tests passed; Lint 0 errors / 4 warnings; APK compiled and signed.
- Local real-package probe under a 256 MiB heap: 4 WPK and 2 STD2_0 LPK read successfully; 5 standalone STM_1_0 packages require outer WPK metadata and fail with an explicit message. All original sample hashes unchanged.
- No phone/emulator UI test performed. No dynamic model playback.
