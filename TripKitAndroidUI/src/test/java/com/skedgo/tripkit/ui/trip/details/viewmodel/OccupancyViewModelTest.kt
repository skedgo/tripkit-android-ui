package com.skedgo.tripkit.ui.trip.details.viewmodel

import android.content.Context
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.routing.Occupancy
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.VehicleComponent
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class OccupancyViewModelTest {

    private lateinit var viewModel: OccupancyViewModel
    private val context: Context = mockk()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { context.getString(any()) } returns "Moderate Occupancy"
        every { ContextCompat.getDrawable(context, any()) } returns mockk()
        every { ContextCompat.getColor(context, any()) } returns 0xFF0000

        viewModel = OccupancyViewModel(context)
    }

    @Test
    fun `setOccupancy updates fields correctly for single vehicle`() {
        val vehicle = mockk<RealTimeVehicle>()
        val component = mockk<VehicleComponent>() {
            every { this@mockk.occupancy() } returns Occupancy.StandingRoomOnly.value
        }

        every { vehicle.hasVehiclesOccupancy() } returns false
        every { vehicle.hasSingleVehicleOccupancy() } returns true
        every { vehicle.getAverageOccupancy() } returns Occupancy.StandingRoomOnly
        every { vehicle.components } returns listOf(listOf(component)) // Ensure it's not empty

        viewModel.setOccupancy(vehicle, showAverage = true)

        assertTrue(viewModel.hasOccupancySingleInformation.get())
        assertEquals("Moderate Occupancy", viewModel.occupancyText.get())
        assertNotNull(viewModel.drawableLeft.get())
    }

    @Test
    fun `setOccupancy updates train occupancy items when vehicle has components`() {
        val vehicle = mockk<RealTimeVehicle>()
        val component1 = mockk<VehicleComponent>(relaxed = true) {
            every { occupancy() } returns "FEW_SEATS_AVAILABLE"
        }
        val component2 = mockk<VehicleComponent>(relaxed = true) {
            every { occupancy() } returns "FULL"
        }

        every { vehicle.hasVehiclesOccupancy() } returns true
        every { vehicle.components } returns listOf(listOf(component1, component2))

        every { ContextCompat.getColor(context, any()) } returns 0xFF0000
        every { ContextCompat.getDrawable(context, any()) } returns mockk()

        viewModel.setOccupancy(vehicle, showAverage = false)

        assertTrue(viewModel.hasOccupancyInformation.get())
        assertEquals(2, viewModel.items.size) // Expecting 2 items
    }


    @Test
    fun `setOccupancy does not update train items if no components exist`() {
        val vehicle = mockk<RealTimeVehicle>()
        every { vehicle.hasVehiclesOccupancy() } returns false
        every { vehicle.components } returns emptyList()

        viewModel.setOccupancy(vehicle, showAverage = false)

        assertFalse(viewModel.hasOccupancyInformation.get())
        assertEquals(0, viewModel.items.size)
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `occupied people progress left to right with stronger contrast in both themes`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val levels = listOf(Occupancy.Empty to 1, Occupancy.ManySeatsAvailable to 1,
            Occupancy.FewSeatsAvailable to 2, Occupancy.StandingRoomOnly to 3,
            Occupancy.CrushedStandingRoomOnly to 3, Occupancy.Full to 4,
            Occupancy.NotAcceptingPassengers to 4, null to 0)
        for (night in listOf(false, true)) {
            val configuration = android.content.res.Configuration(app.resources.configuration).apply {
                uiMode = if (night) android.content.res.Configuration.UI_MODE_NIGHT_YES
                    else android.content.res.Configuration.UI_MODE_NIGHT_NO
            }
            val themed = app.createConfigurationContext(configuration)
            val surface = themed.getColor(com.skedgo.tripkit.ui.R.color.white)
            val sheet = android.graphics.Bitmap.createBitmap(600, levels.size * 200, android.graphics.Bitmap.Config.ARGB_8888)
            sheet.eraseColor(surface)
            val canvas = android.graphics.Canvas(sheet)
            val labelPaint = android.graphics.Paint().apply { color = themed.getColor(com.skedgo.tripkit.ui.R.color.black); textSize = 24f }
            for ((row, entry) in levels.withIndex()) {
                val (level, count) = entry
                val bitmap = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(surface)
                val model = OccupancyViewModel(themed)
                val occupancy = level?.let { "\"occupancy\":\"${it.value}\"" }.orEmpty()
                model.setOccupancy(com.google.gson.Gson().fromJson("""{"components":[[{$occupancy}]]}""", RealTimeVehicle::class.java), false)
                assertEquals(level != null, model.hasInformation())
                model.drawableLeft.get()?.let { drawable ->
                    drawable.setBounds(0, 0, 400, 300)
                    drawable.draw(android.graphics.Canvas(bitmap))
                }
                canvas.drawText(level?.name ?: "Unknown: no indicator", 20f, row * 200f + 30f, labelPaint)
                canvas.drawBitmap(bitmap, null, android.graphics.Rect(20, row * 200 + 40, 220, row * 200 + 190), null)
                for (person in 0..3) {
                    val contrast = androidx.core.graphics.ColorUtils.calculateContrast(bitmap.getPixel(50 + person * 100, 120), surface)
                    if (person < count) assertTrue("$level night=$night person=$person occupied contrast=$contrast", contrast >= 3.0)
                    else assertTrue("$level night=$night person=$person unoccupied contrast=$contrast", contrast < 2.0)
                }
            }
            val file = java.io.File("build/test-artifacts/occupancy-${if (night) "dark" else "light"}.png")
            file.parentFile.mkdirs()
            file.outputStream().use { sheet.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun `unknown component clears an earlier occupancy instead of reporting empty`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val model = OccupancyViewModel(app)
        val gson = com.google.gson.Gson()
        model.setOccupancy(gson.fromJson("""{"components":[[{"occupancy":"FULL"}]]}""", RealTimeVehicle::class.java), false)
        assertTrue(model.hasInformation())
        val unknown = gson.fromJson("""{"components":[[{}]]}""", RealTimeVehicle::class.java)
        assertNull(unknown.getAverageOccupancy())
        model.setOccupancy(unknown, false)
        assertFalse(model.hasInformation())
        assertNull(model.drawableLeft.get())
        assertNull(model.occupancyText.get())
        assertTrue(model.items.isEmpty())
    }

    @Test fun `missing occupancy does not dilute known components and empty updates clear state`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val model = OccupancyViewModel(app)
        val gson = com.google.gson.Gson()
        val mixed = gson.fromJson("""{"components":[[{}, {"occupancy":"FULL"}]]}""", RealTimeVehicle::class.java)
        assertEquals(Occupancy.Full, mixed.getAverageOccupancy())
        model.setOccupancy(mixed, false)
        assertTrue(model.hasInformation())
        assertEquals(2, model.items.size)
        model.setOccupancy(gson.fromJson("""{"components":[]}""", RealTimeVehicle::class.java), false)
        assertFalse(model.hasInformation())
        assertTrue(model.items.isEmpty())
        assertNull(model.drawableLeft.get())
    }
}
