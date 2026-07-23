# Contributing to AnyWebView

Thanks for your interest! This document covers how to report bugs, help with
device testing, and contribute code.

## Reporting bugs

Use the [bug report form](https://github.com/neoblackxt/AnyWebView/issues/new?template=bug_report.yml).
A good report includes:

- **Module version** (e.g. 1.4)
- **Android version and ROM** (stock, LineageOS, MIUI, ...)
- **Xposed framework and version** (LSPosed, Vector, EdXposed, ...)
- **The WebView provider** you are trying to use (package name + where it came from)
- **The framework's module log** captured after a reboot (lines tagged `anywebview`)

Always reboot after activating or updating the module before reporting - the
hook lives in `system_server` and only loads at boot.

## Device testing wanted

The v1.4 injection rework was verified on Android 16. The fallback path for
Android 7-14 (API 24-34) is derived from AOSP sources but has had little
real-device testing.

If you run a pre-Android-15 device, your report - success or failure - is
genuinely useful:

1. Install the module, activate it in your framework, reboot.
2. Check developer options -> WebView implementation: is your provider listed?
   Selectable? Do apps actually use it?
3. Open an issue with your Android version, framework, and the result.

## Code contributions

- **Small fixes:** open a PR directly.
- **Anything bigger:** open an issue first so the approach can be agreed on
  before you spend time on it.

Guidelines:

- **Build:** JDK 17, `./gradlew build` must pass (that includes lint, and CI
  runs it on every push).
- **Compatibility:** `minSdk` is 24. Newer Android APIs may only be used behind
  version checks or reflection. Both Xposed APIs (`de.robv`, libxposed) must
  stay `compileOnly` - the frameworks provide them at runtime.
- **Design:** shared, framework-agnostic logic lives in
  `WebViewProviderInjector`; `AnyWebView` (de.robv) and `AnyWebViewModule`
  (libxposed) are thin adapters. Keep it that way: no Xposed imports in the
  injector, no duplicated logic in the adapters.
- **Dependencies:** avoid new ones. The module currently ships zero runtime
  dependencies.
- **Scope:** AnyWebView makes WebView providers selectable - it does not modify
  WebView behavior. Out-of-scope requests (ad-blocking, filter lists, provider
  settings UIs) will be declined.
- **PR description:** say what you tested and, just as important, what you did
  *not* test. Honest "untested" notes are respected here, not punished.

## Releasing (maintainer)

1. Bump `versionCode` / `versionName` in `app/build.gradle`.
2. `git tag vX.Y && git push origin vX.Y` (or trigger the Release workflow
   manually with a tag name).
3. CI builds the signed APK, verifies the signature, and creates the GitHub
   Release.

## License

By contributing you agree that your contributions are licensed under the
[MIT License](LICENSE).
