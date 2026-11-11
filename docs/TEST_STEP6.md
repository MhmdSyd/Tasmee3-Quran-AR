# Step 6 on-device test (needs a real phone, airplane mode on)

1. Open the reader and tap the menu icon (top right): the settings screen opens.
   - "الحروف فقط" is selected; "مع التشكيل" is greyed out on purpose (the model cannot judge vowel marks).
   - The model card shows the real size of the model inside the app.
   - Switch "آية كاملة" on, go back, recite a page: words must stay hidden until a whole verse is finished.
   - Switch "تشغيل صوت عند الخطأ" on and say a wrong word: a short beep plays.
   - Switch "تلميح تلقائي" off: no word is revealed by itself after 5 seconds of silence.
   - Close the app completely, reopen it: the switches keep their values.
2. Finish a recitation (or tap the square): the summary shows the accuracy ring, three count cards and review cards.
   Check that a wrong word shows "سمعنا: ..." and that "أعد التسميع" and "الصفحة التالية" work.
3. Check the app icon on the home screen (arch with microphone) and, on Android 13 or newer, with "Themed icons" on.
4. Data is stored only on the phone (database `tasmee3.db`). Nothing needs checking here except that the summary still opens normally.
