package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.model.OfflineDataManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.After
import org.junit.Test

class UserSyncManagerTest {

    @After
    fun reset() = AuthSession.signOut()

    @Test
    fun `signing out purges the cached events of that account`() {
        val favorites = mockk<FavoritesRepositoryImpl>(relaxed = true)
        val offline = mockk<OfflineDataManager>(relaxed = true)
        coEvery { favorites.syncBookmarks() } returns Unit
        AuthSession.signOut()

        UserSyncManager(favorites, offline).start()
        AuthSession.signIn("jwt", AuthUser("Jana", "jana@x.cz"))
        val key = AuthSession.ownerKey!!
        coVerify(timeout = 3000) { favorites.syncBookmarks() }

        AuthSession.signOut()
        coVerify(timeout = 3000) { offline.purgeOwnedEvents(key) }
    }
}
