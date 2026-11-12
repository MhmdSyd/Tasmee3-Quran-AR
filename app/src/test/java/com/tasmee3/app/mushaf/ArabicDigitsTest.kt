package com.tasmee3.app.mushaf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArabicDigitsTest {
    @Test fun formatsArabicIndic() = assertEquals("٦٠٤", ArabicDigits.fromInt(604))
    @Test fun parsesLatin() = assertEquals(42, ArabicDigits.toInt("42"))
    @Test fun parsesArabicIndic() = assertEquals(604, ArabicDigits.toInt(" ٦٠٤ "))
    @Test fun parsesPersian() = assertEquals(12, ArabicDigits.toInt("۱۲"))
    @Test fun rejectsText() = assertNull(ArabicDigits.toInt("abc"))
}

class ArabicDigitsConvertTest {
    @Test fun convertsDigitsInsideText() = assertEquals("٠٠:١٣", ArabicDigits.convert("00:13"))
    @Test fun keepsOtherCharacters() = assertEquals("ص ١٢٨ | ٦:١", ArabicDigits.convert("ص 128 | 6:1"))
}
