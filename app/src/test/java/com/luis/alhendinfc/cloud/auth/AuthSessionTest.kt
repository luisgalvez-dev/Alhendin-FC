package com.luis.alhendinfc.cloud.auth

import com.luis.alhendinfc.cloud.CloudStore
import com.luis.alhendinfc.cloud.MemoryCloudStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionTest {

    @Test
    fun loggedOut_whenNoUser() = runTest {
        val store = MemoryCloudStore()
        val auth = AuthRepository(FakeAuthBackend(null), store, "alhendin-dev")
        assertEquals(AuthSession.LoggedOut, auth.resolveCurrent())
    }

    @Test
    fun loggedInMember_isReady() = runTest {
        val store = MemoryCloudStore()
        store.addMember("uid-migue")
        val user = AuthUser("uid-migue", "migue@example.test", "Migue")
        val auth = AuthRepository(FakeAuthBackend(user), store, "alhendin-dev")
        val session = auth.resolveCurrent()
        assertTrue(session is AuthSession.Ready)
        session as AuthSession.Ready
        assertEquals("uid-migue", session.user.uid)
        assertEquals("alhendin-dev", session.workspaceId)
    }

    @Test
    fun signedInNonMember_doesNotEnterSports() = runTest {
        val store = MemoryCloudStore()
        val user = AuthUser("uid-other", "otro@example.test", "Otro")
        val auth = AuthRepository(FakeAuthBackend(user), store, "alhendin-dev")
        val session = auth.resolveCurrent()
        assertTrue(session is AuthSession.NonMember)
    }

    @Test
    fun signInMember_becomesReady() = runTest {
        val store = MemoryCloudStore()
        val backend = FakeAuthBackend()
        val auth = AuthRepository(backend, store, "alhendin-dev")
        val signed = backend.signIn("analista@example.test", "secret")
        store.addMember(signed.uid)
        val session = auth.resolveCurrent()
        assertTrue(session is AuthSession.Ready)
    }

    @Test
    fun cachedMember_offlineDoesNotBlockReady() = runTest {
        val cache = MemoryMembershipCache()
        cache.seed("uid-migue", "alhendin-dev")
        val user = AuthUser("uid-migue", "migue@example.test", "Migue")
        val auth = AuthRepository(
            FakeAuthBackend(user),
            HangingMemberStore(),
            "alhendin-dev",
            cache,
            membershipTimeoutMs = 5_000L
        )
        val session = auth.resolveCurrent()
        assertTrue(session is AuthSession.Ready)
        session as AuthSession.Ready
        assertEquals("uid-migue", session.user.uid)
    }

    @Test
    fun neverValidated_offlineDoesNotGrantAccess() = runTest {
        val user = AuthUser("uid-new", "nuevo@example.test", "Nuevo")
        val auth = AuthRepository(
            FakeAuthBackend(user),
            HangingMemberStore(),
            "alhendin-dev",
            MemoryMembershipCache(),
            membershipTimeoutMs = 50L
        )
        val session = auth.resolveCurrent()
        assertTrue(session is AuthSession.Unavailable)
    }

    @Test
    fun cachedMember_onlineNonMemberRevokes() = runTest {
        val cache = MemoryMembershipCache()
        cache.seed("uid-migue", "alhendin-dev")
        val store = MemoryCloudStore()
        val user = AuthUser("uid-migue", "migue@example.test", "Migue")
        val auth = AuthRepository(FakeAuthBackend(user), store, "alhendin-dev", cache)
        assertTrue(auth.resolveCurrent() is AuthSession.Ready)
        val confirmed = auth.confirmMembership()
        assertTrue(confirmed is AuthSession.NonMember)
    }
}

private class HangingMemberStore(
    private val inner: MemoryCloudStore = MemoryCloudStore()
) : CloudStore by inner {
    override suspend fun isMember(uid: String): Boolean {
        kotlinx.coroutines.delay(Long.MAX_VALUE)
        return false
    }
}
