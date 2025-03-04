package com.skedgo.tripkit.ui.database.location_history

import com.skedgo.tripkit.common.model.location.Location
import io.mockk.*
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class LocationHistoryRepositoryTest {

    private lateinit var repository: LocationHistoryRepository
    private val mockDao: LocationHistoryDao = mockk()
    private val mockMapper: LocationHistoryMapper = mockk()

    @Before
    fun setUp() {
        repository = LocationHistoryRepository(mockDao, mockMapper)
    }

    @Test
    fun `saveLocationsToHistory should map and insert locations`() {
        // Arrange
        val mockLocations = listOf(mockk<Location>(relaxed = true))
        val mockEntities = listOf(mockk<LocationHistoryEntity>(relaxed = true))

        every { mockMapper.toEntity(mockLocations) } returns mockEntities
        every { mockDao.insert(mockEntities) } returns Completable.complete()

        // Act
        val testObserver = repository.saveLocationsToHistory(mockLocations).test()

        // Assert
        testObserver.assertComplete()
        verify { mockMapper.toEntity(mockLocations) }
        verify { mockDao.insert(mockEntities) }
    }

    @Test
    fun `getLocationHistory should fetch and map locations`() {
        // Arrange
        val mockEntities = listOf(mockk<LocationHistoryEntity>(relaxed = true))
        val mockLocations = listOf(mockk<Location>(relaxed = true))

        every { mockDao.getAllLocationInHistory() } returns Single.just(mockEntities)
        every { mockMapper.toLocation(mockEntities) } returns mockLocations

        // Act
        val testObserver = repository.getLocationHistory().test()

        // Assert
        testObserver.assertValue(mockLocations)
        verify { mockDao.getAllLocationInHistory() }
        verify { mockMapper.toLocation(mockEntities) }
    }

    @Test
    fun `getLatestLocationHistory should delete old entries and return filtered locations`() {
        // Arrange
        val timestamp = 1640995200000L
        val mockEntities = listOf(mockk<LocationHistoryEntity>(relaxed = true))
        val mockLocations = listOf(mockk<Location>(relaxed = true))

        every { mockDao.deleteOldHistory(timestamp) } returns Completable.complete()
        every { mockDao.getLocationInHistory(timestamp) } returns Single.just(mockEntities)
        every { mockMapper.toLocation(mockEntities) } returns mockLocations

        // Act
        val testObserver = repository.getLatestLocationHistory(timestamp).test()

        // Assert
        testObserver.assertValue(mockLocations)
        verify { mockDao.deleteOldHistory(timestamp) }
        verify { mockDao.getLocationInHistory(timestamp) }
        verify { mockMapper.toLocation(mockEntities) }
    }
}
