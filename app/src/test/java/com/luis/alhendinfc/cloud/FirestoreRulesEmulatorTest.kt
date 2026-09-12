package com.luis.alhendinfc.cloud

import org.junit.Ignore
import org.junit.Test

/**
 * Casos de reglas Firestore. Requiere Emulator Suite en marcha
 * (`firebase emulators:start` desde la raíz del repo).
 * No se ejecutan en CI/unitarias: no fingir que pasaron.
 */
@Ignore("Requiere Firebase Emulator Suite (manual)")
class FirestoreRulesEmulatorTest {

    @Test
    fun unsignedCannotReadWorkspace() {
        // Manual: GET workspaces/alhendin-dev sin auth → deny
    }

    @Test
    fun nonMemberCannotReadSports() {
        // Manual: auth uid sin documents/workspaces/alhendin-dev/members/{uid} → deny
    }

    @Test
    fun memberCanReadWriteSportsCollections() {
        // Manual: miembro puede leer/escribir teams, players, matches, boards, …
    }

    @Test
    fun memberCannotWriteMembers() {
        // Manual: allow write: false en members
    }

    @Test
    fun userCanOnlyWriteOwnPreferences() {
        // Manual: users/{uid}/preferences/homeLayout solo el propio uid
    }
}
