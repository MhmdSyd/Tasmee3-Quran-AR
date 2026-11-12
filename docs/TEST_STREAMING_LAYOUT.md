# On-device test: streaming reveal and the new mushaf layout

1. Open the reader. It should look like the reference app: light page, black text, black banner with the surah name,
   ringed verse numbers, a grey info card at the top (surah name, "صفحة | جزء | surah:verse"), a big mint button at the
   bottom right and the page buttons at the bottom left. Page 132 must break its lines exactly like the printed
   1405 Madani mushaf (line 7 ends with verse 39 then "قل", line 12 starts with verse 42).
2. Tap the mint button, allow the microphone, and recite WITHOUT stopping between words: words must appear one by one
   (the newest in blue) while you are still speaking, not after the end of the ayah.
3. Recite a whole ayah in one breath. Words should keep appearing during the ayah. After your pause the picture is
   confirmed; nothing should disappear or flicker.
4. Say a wrong word: it is flagged only after your pause (red outline and pink panel).
5. Hidden words must show as thin grey lines (one continuous line per row) with the verse numbers visible.
6. Settings > "آية كاملة": a verse appears only when all of its words were heard.

Please tell me: how many words behind your voice the screen is (none / one / several), whether the last word flickers,
and whether the phone gets hot or slow on long ayahs (a 350 ms refresh of a long phrase costs battery).
