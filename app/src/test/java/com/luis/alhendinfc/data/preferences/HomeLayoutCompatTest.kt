package com.luis.alhendinfc.data.preferences

import com.luis.alhendinfc.domain.model.HomeModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutCompatTest {

    @Test
    fun oldLayoutWithoutTasks_insertsTasksAndRivalsNearCalendar() {
        val raw = "team:1,matches:1,calendar:1,pizarra:1,statistics:1,settings:1,next_match:1,live_match:1"
        val config = HomePreferencesRepository.decode(raw)
        val ids = config.modules.map { it.module.id }
        assertEquals(
            listOf(
                "team", "matches", "calendar", "tasks", "rivals", "pizarra",
                "statistics", "settings", "next_match", "live_match"
            ),
            ids
        )
        assertEquals(HomeModule.TASKS, config.modules[3].module)
        assertEquals(HomeModule.RIVALS, config.modules[4].module)
        assertTrue(config.modules[3].enabled)
        assertTrue(config.modules[4].enabled)
        assertEquals(HomeModule.TEAM, config.modules[0].module)
        assertEquals(HomeModule.CALENDAR, config.modules[2].module)
        assertEquals(HomeModule.PIZARRA, config.modules[5].module)
        assertTrue(config.visibleOrdered.contains(HomeModule.TASKS))
        assertTrue(config.visibleOrdered.contains(HomeModule.RIVALS))
    }

    @Test
    fun oldDisabledModules_arePreservedWhenTasksAreAppended() {
        val raw = "team:1,matches:0,calendar:1,pizarra:1,statistics:0,settings:1,next_match:1,live_match:1"
        val config = HomePreferencesRepository.decode(raw)
        val matches = config.modules.first { it.module == HomeModule.MATCHES }
        val stats = config.modules.first { it.module == HomeModule.STATISTICS }
        assertFalse(matches.enabled)
        assertFalse(stats.enabled)
        assertTrue(config.modules.any { it.module == HomeModule.TASKS && it.enabled })
        assertTrue(config.modules.any { it.module == HomeModule.RIVALS && it.enabled })
        assertFalse(config.visibleOrdered.contains(HomeModule.MATCHES))
    }

    @Test
    fun unknownTokensDoNotBreakParser() {
        val raw = "team:1,unknown_mod:1,matches:1"
        val config = HomePreferencesRepository.decode(raw)
        assertEquals(HomeModule.TEAM, config.modules[0].module)
        assertEquals(HomeModule.MATCHES, config.modules[1].module)
        assertTrue(config.modules.any { it.module == HomeModule.TASKS && it.enabled })
    }

    @Test
    fun blankLayoutUsesDefaultsIncludingTasks() {
        val config = HomePreferencesRepository.decode(null)
        assertEquals(HomeModule.entries.size, config.modules.size)
        assertTrue(config.modules.any { it.module == HomeModule.TASKS && it.enabled })
        assertEquals(
            HomeModule.TASKS,
            config.modules.first { it.module == HomeModule.TASKS }.module
        )
        val calendarIndex = config.modules.indexOfFirst { it.module == HomeModule.CALENDAR }
        val tasksIndex = config.modules.indexOfFirst { it.module == HomeModule.TASKS }
        val rivalsIndex = config.modules.indexOfFirst { it.module == HomeModule.RIVALS }
        assertEquals(calendarIndex + 1, tasksIndex)
        assertEquals(tasksIndex + 1, rivalsIndex)
    }

    @Test
    fun settingsCannotBeDisabledInDecode() {
        val raw = "team:1,matches:1,calendar:1,tasks:1,rivals:1,pizarra:1,statistics:1,settings:0,next_match:1,live_match:1"
        val config = HomePreferencesRepository.decode(raw)
        val settings = config.modules.first { it.module == HomeModule.SETTINGS }
        assertTrue(settings.enabled)
        assertTrue(config.visibleOrdered.contains(HomeModule.SETTINGS))
    }
}
