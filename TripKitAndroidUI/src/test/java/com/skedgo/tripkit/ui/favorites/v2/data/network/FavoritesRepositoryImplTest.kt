package com.skedgo.tripkit.ui.favorites.v2.data.network

import com.skedgo.TripKit
import com.skedgo.network.Resource
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteDaoV2
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteV2
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository.FavoritesRepositoryImpl
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FavoritesRepositoryImplTest {

    private lateinit var repository: FavoritesRepositoryImpl

    private val mockApi: FavoritesApi = mockk()
    private val mockDao: FavoriteDaoV2 = mockk()
    private val mockConfigs: Configs = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(TripKit) // Mock the singleton
        every { TripKit.getInstance() } returns mockk {
            every { configs() } returns mockConfigs
        }

        every { mockConfigs.userIdentifier() } returns mockk {
            every { call() } returns "test-user-id" // Ensures it returns a valid string
        }

        repository = FavoritesRepositoryImpl(mockApi, mockDao)
    }

    @Test
    fun `getFavorites should fetch favorites correctly`() = runBlocking {
        // Arrange
        val favoriteList = listOf(mockk<FavoriteV2>(relaxed = true))
        coEvery { mockDao.getAllFavoritesWithEmptyUserId(any()) } returns favoriteList

        // Act
        val flow = repository.getFavorites()

        // Assert
        val results = flow.toList() // Collect all emissions
        assertEquals(Resource.success(FavoriteResponse(result = favoriteList)), results.last()) // Validate the final emission
    }

    @Test
    fun `addFavorite should insert a favorite correctly`() = runBlocking {
        // Arrange
        val favorite = mockk<FavoriteV2>(relaxed = true)
        coEvery { mockDao.insertFavorite(any()) } just Runs
        coEvery { mockApi.addFavorite(any()) } returns favorite.copy(userId = "test-user-id") // Ensures correct type

        // Act
        val flow = repository.addFavorite(favorite)

        // Assert
        val results = flow.toList() // Collect all emissions
        assertEquals(Resource.success(favorite), results.last()) // Validate the final emission
    }

    @Test
    fun `deleteFavorite should remove a favorite correctly`() = runBlocking {
        // Arrange
        val favoriteId = "123"
        coEvery { mockDao.deleteFavoriteByObjectId(favoriteId) } just Runs
        coEvery { mockApi.deleteFavorite(favoriteId) } just Runs

        // Act
        val flow = repository.deleteFavorite(favoriteId)

        // Assert
        val results = flow.toList() // Collect all emissions
        assertEquals(Resource.success(Unit), results.last()) // Validate only the final state
    }

    @Test
    fun `isFavorite should check if a favorite exists`() = runBlocking {
        // Arrange
        val favoriteId = "123"
        val userId = "test-user-id"

        coEvery { mockConfigs.userIdentifier()?.call() } returns userId
        coEvery { mockDao.favoriteExistsForUser(favoriteId, userId) } returns true

        // Act
        val flow = repository.isFavorite(favoriteId)

        // Assert
        val results = flow.toList() // Collect all emissions
        assertEquals(Resource.success(true), results.last()) // Validate only the final state
    }

}

