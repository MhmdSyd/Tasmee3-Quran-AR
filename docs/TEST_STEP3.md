# Step 3 on-device test (needs a real phone)

Before the build: run Actions -> **Export Tarteel Whisper model** with `publish` ticked (once). Then push any commit so CI builds an APK that contains the model.

1. Install the APK. Open the reader and tap the **menu icon** (top right). The test screen opens.
2. The status box must say the model is ready and show the load time. If it says the files are missing, the release was not published. If it says failed, send me the message.
3. Tap start recording and allow the microphone.
4. Recite Al-Fatiha slowly, pausing briefly between verses (about one second). Each pause produces one result card with: the text, the audio length and the decode time.
5. Write down for me:
   - the decode time against the audio length on your phone model (for example 6 s audio, 3 s decode);
   - whether the text has diacritics (the little vowel marks) or not;
   - 3-5 result texts, including at least one wrong one;
   - whether the screen froze or the app crashed.
6. Repeat once in a noisy room and once reciting without pauses for about 20 seconds.
