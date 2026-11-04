# JOKEVIEWER

Independent offline Android model-package viewer project.

**0.1.0 is an import/diagnostic preview, NOT a Spine or Live2D animation player.**

## Current capabilities

- Read a SAF-selected WPK or LPK without changing the original.
- WPK nested LPK discovery; observed STM_1_0 and STD2_0 package byte transforms.
- List model/costume configs, identify Spine / recognizable Live2D configs, list configured motion names and check Spine resource references.
- Static WPK cover preview, sampled image decoding, cooperative cancellation.
- Bounded ZIP entries, resource sizes and copy operations; traversal/duplicate-path rejection.
- No internet permission, analytics, cloud upload, desktop DLL or model assets.

Not implemented: dynamic rendering, physics, expressions, model hit areas/menus, audio, wallpaper mode, generic versions of every package format. A standalone STM_1_0 LPK requires its containing WPK metadata.

## Target and build

Android 10+ (API29), target35; owner's recorded test device is Redmi / Android15 / ARM64.
Current diagnostic Java-only APK has no native ABI restriction. JDK17, Android SDK37, Gradle9.6.1, AGP9.4.0.

Run ./gradlew testDebugUnitTest lintDebug assembleDebug (gradlew.bat on Windows).
Set sdk.dir locally; do not commit local.properties. Build output is app/build/outputs/apk/debug/app-debug.apk.

## Publication

[Public repository](https://github.com/liuchengzhiaa/JOKEVIEWER). Each completed code version gets reviewed source, a unique release tag, signed development APK, SHA256 and build manifest. Releases are public so phone downloads do not require GitHub login. See docs/RELEASING.md.

## Runtime licensing and roadmap

Phase 1: package import/diagnostic foundation. Phase 2: Spine runtimes selected by actual skeleton version and robust multi-page/PMA rendering. Phase 3: motion, skin, input and ViewerEX-compatible command scheduling. Phase 4: Live2D engine and package compatibility after license confirmation. These are plans, not current capabilities.

Spine runtime integration/public redistribution is subject to its [runtime license](https://github.com/EsotericSoftware/spine-runtimes#licensing). Cubism Core is proprietary and is [not published on GitHub](https://docs.live2d.com/en/cubism-sdk-manual/cubism-core/). A viewer accepting arbitrary Live2D models appears to meet the official [Expandable Application definition](https://www.live2d.com/en/sdk/license/expandable/); approval/publication terms must be resolved before publishing that engine. No such runtime is bundled in this preview.

Original code is MIT-licensed. Generated Gradle wrapper files retain their original notices. Model packages remain the property of their respective creators and are never included.
