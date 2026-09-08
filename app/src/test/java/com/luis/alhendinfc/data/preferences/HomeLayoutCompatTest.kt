package com.luis.alhendinfc.data.preferences

import com.luis.alhendinfc.domain.model.HomeModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutCompatTest {

    @Test
    fun oldLayoutWithoutTasks_stillLoadsAndAppendsNewModule() {
        val raw = "team:1,matches:1,calendar:1,pizarra:1,statistics:1,settings:1,next_match:1,live_match:1"
        val config = HomePreferencesRepository.decode(raw)
        val ids = config.modules.map { it.module.id }
        assertEquals(
            listOf(
                "team", "matches", "calendar", "pizarra",
                "statistics", "settings", "next_match", "live_match", "tasks"
            ),
            ids
        )
        val tasks = config.modules.last()
        assertEquals(HomeModule.TASKS, tasks.module)
        assertTrue(tasks.enabled)
        assertEquals(HomeModule.TEAM, config.modules[0].module)
        assertEquals(HomeModule.CALENDAR, config.modules[2].module)
        assertEquals(HomeModule.PIZARRA, config.modules[3].module)
        assertTrue(config.visibleOrdered.contains(HomeModule.TASKS))
    }

    @Test
    fun oldDisabledModules_arePreservedWhenTasksAreAppended() {
        val raw = "team:1,matches:0,calendar:1,pizarra:1,statistics:0,settings:1,next_match:1,live_match:1"
        val config = HomePreferencesRepository.decode(raw)
        val matches = config.modules.first { it.module == HomeModule.MATCHES }
        val stats = config.modules.first { it.module == HomeModule.STATISTICS }
        assertFalse(matches.enabled)
        assertFalse(stats.enabled)
        assertTrue(config.modules.first { it.module == HomeModule.TASKS }.enabled)
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
        assertEquals(calendarIndex + 1, tasksIndex)
    }
}
