# AnyWebView

Any WebView is OK!

<img src=".github/webviews.jpg" width="720"/>

Android Framework should be selected in LSPosed.

A webview app must be installed for all users (or in all spaces, so-called Dual app, Second space, etc.) to be selectable. Maybe deleting redundant users is alternative.
adb command:

Get USER_ID list:

`adb shell pm list users`

Each user entry is as follow: UserInfo{USER_ID:USERNAME:INT} , USER_ID 0 is the main user.

Install apk for specific USER_ID:

`adb install --user USER_ID PATH/TO/APK/ON/COMPUTER`

or

`adb shell pm install-existing --user USER_ID PACKAGE.NAME.OF.APK` (for apks already installed for one user)

or

`adb push PATH/TO/APK/ON/COMPUTER PATH/TO/APK/ON/PHONE` (copy an apk file to phone from computer)

`adb shell pm install --user USER_ID PATH/TO/APK/ON/PHONE`

Delete a user (NOT RECOMENDED, be careful, you may lose important data):

`adb shell pm remove-user USER_ID`

All the `adb shell pm ...` commands above can be run in an Android terminal simulator(root access granted) as `pm ...`

Reboot to take effect.

### FAQ

Can I set Chrome as the system webview implementation?

Only supported on Android 8-9, not supported on Android 10+. It is Google's policy that Google Chrome app is no longer the WebView provider in Android 10. Even though it is listed in the WebView implementation, it does not work on Android 10+. Related discussion: [AnyWebView#12](https://github.com/neoblackxt/AnyWebView/issues/12#issuecomment-1644258502)

Is Bromite/Mulch/Vanadium etc. system webview supported?

Only Bromite system webview is supported on current branch. All of them are supported on the [experimental branch](https://github.com/neoblackxt/AnyWebView/tree/experimental), you can download the apk from [pre-release](https://github.com/neoblackxt/AnyWebView/releases/tag/v1.2).

