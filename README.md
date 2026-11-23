# Darslab TV — Android app

A small shell app for Android phones, tablets, Android TV / Google TV and Fire TV. It opens https://darslab.com/tv/ full screen;
lessons, sign-in and everything else live on the site. No ads, no tracking, internet permission only.

Build: GitHub → Actions → "Darslab TV Android app" → Run workflow → download the `darslab-tv-android` artifact.
Signed store builds need the repository secrets `DL_KEYSTORE_B64`, `DL_KEYSTORE_PASS`, `DL_KEY_ALIAS`, `DL_KEY_PASS`. Never commit the key file.
