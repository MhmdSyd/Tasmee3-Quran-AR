package com.tasmee3.app

import com.tasmee3.app.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun routesAreUniqueAndComplete() {
        assertEquals(5, Routes.all.size)
        assertEquals("recitation/604", Routes.recitation(604))
        assertEquals(Routes.all.size, Routes.all.toSet().size)
    }
}
