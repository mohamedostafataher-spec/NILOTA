package com.example.ui.passport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassportLevelTest {

    @Test
    fun zeroStampsIsNewcomer() {
        assertEquals(PassportLevel.NEWCOMER, PassportLevel.forStampCount(0))
    }

    @Test
    fun oneToThreeStampsIsExplorer() {
        assertEquals(PassportLevel.EXPLORER, PassportLevel.forStampCount(1))
        assertEquals(PassportLevel.EXPLORER, PassportLevel.forStampCount(3))
    }

    @Test
    fun fourStampsIsAdventurer() {
        assertEquals(PassportLevel.ADVENTURER, PassportLevel.forStampCount(4))
    }

    @Test
    fun eightStampsIsNileMaster() {
        assertEquals(PassportLevel.NILE_MASTER, PassportLevel.forStampCount(8))
    }

    @Test
    fun twelveStampsIsGlobalSoul() {
        assertEquals(PassportLevel.GLOBAL_SOUL, PassportLevel.forStampCount(12))
    }

    @Test
    fun globalSoulHasNoNextLevel() {
        assertNull(PassportLevel.next(PassportLevel.GLOBAL_SOUL))
    }

    @Test
    fun egyptPassportHasTwelveCities() {
        assertEquals(EgyptPassport.TOTAL_STAMPS, EgyptPassport.cities.size)
    }

    @Test
    fun cityIdsAreUnique() {
        val ids = EgyptPassport.cities.map { it.cityId }
        assertEquals(ids.size, ids.toSet().size)
    }
}
