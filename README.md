# Awful

Awful is an __unofficial__ viewer for the [SomethingAwful forums][forums] on Android platforms. Come check out the [Awful forum thread][forum-thread]!

<a href='https://play.google.com/store/apps/details?id=com.ferg.awfulapp'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='80px'/></a>

## Terms and Conditions for usage of this app

If you are a Google Play review-bot (or actual flesh automaton), you can find the forum rules (or "terms of user/user policy") [at this address on the interwebs][forum-rules]. I'm not sure why you would look for them here, but you do you.

## Contributing

Want to help? Come join us in the [development and beta testing thread][dev-thread], and [join the beta testing program][join-beta].

Or take a look at [our issues][issues] and set up your own fork to quash some bugs!

### Build using Android Studio (recommended)

1. Download and install [Android Studio][android-studio].
   * __[After install, make sure to accept licenses for SDKs.][accept-licensing]__ From the welcome screen, choose the three-dot menu in the upper right-hand corner -> `SDK Manager` -> `SDK Tools` -> check `Google Play Licensing Library` -> click `Apply` -> click `OK`.
2. [Fork Awful.apk on Github][github-fork-howto].
3. Download your fork's Git repository and open the project in Android Studio.
   * Quick way: from the welcome screen, choose `Get from VCS` and enter the Github URL for your new fork.
4. __(Optional)__ If you'd like to be able to upload images to Imgur:
   1. [Register a new application for the Imgur API][imgur-api-docs].
   2. Create the file [`secrets.xml`][secrets-example] in `/Awful.apk/src/main/res/values/`.
   3. Place the client ID in `secrets.xml`, like so:
```
<?xml version=1.0 encoding="utf-8"?>
<resources>
    <string name="imgur_api_client_id">YOUR_CLIENT_ID</string>
</resources>
```
6. `Build > Make Project` should run without any issues!

Further questions or problems? Please let us know in the [dev thread][dev-thread].

[forums]: https://forums.somethingawful.com
[forum-thread]: https://forums.somethingawful.com/showthread.php?threadid=3571717
[dev-thread]: https://forums.somethingawful.com/showthread.php?threadid=3743815
[issues]: https://github.com/Awful/Awful.apk/issues?state=open
[join-beta]: https://play.google.com/apps/testing/com.ferg.awfulapp
[forum-rules]: https://www.somethingawful.com/forum-rules/forum-rules/

[android-studio]: https://developer.android.com/studio
[accept-licensing]: https://stackoverflow.com/a/69897480
[github-fork-howto]: https://docs.github.com/en/get-started/quickstart/fork-a-repo
[firebase-console]: https://console.firebase.google.com/
[imgur-api-docs]: https://apidocs.imgur.com
[secrets-example]: https://forums.somethingawful.com/showthread.php?threadid=3743815&userid=0&perpage=40&pagenumber=17#post505621360