# Building Tasmee3 without Android Studio

You only need a web browser and a GitHub account.

## 0. Updating from a previous zip
Copy the new files over your project folder (keep your own `.git` folder), then in the terminal: `git add .`, `git commit -m "Step 2: Mushaf reader"`, `git push`.

## 1. Put the code on GitHub
1. Unzip the project. It already contains a `.git` folder with the commits.
2. In a terminal inside the folder run:
   ```
   git remote -v
   git push -u origin main
   ```
   (`remote -v` should show `https://github.com/MhmdSyd/Tasmee3-Quran-AR`. GitHub will ask you to sign in.)

## 2. Watch the build
1. Open the repository on GitHub and click the **Actions** tab.
2. Click the newest run named **Android CI**. A green check means success, a red cross means failure.

## 3. Download the APK
1. Open the finished run and scroll to **Artifacts** at the bottom.
2. Download **tasmee3-debug-apk** (a zip). Unzip it to get `app-debug.apk`.

## 4. Install on the phone
1. Copy `app-debug.apk` to the phone (USB, cloud drive, or message it to yourself).
2. Open the file on the phone. When asked, allow **Install unknown apps** for the app you opened it from.
3. Tap **Install**, then open **تسميع**.

## If the build fails
1. In the failed run click the red **build** job.
2. Expand the red step (usually **Unit tests and debug APK**). Scroll up to the first line starting with `e:` or `FAILURE:` .
3. Copy those lines (or screenshot them) and send them to the developer.
4. For test failures, download the **test-reports** artifact and open `tests/testDebugUnitTest/index.html`.
