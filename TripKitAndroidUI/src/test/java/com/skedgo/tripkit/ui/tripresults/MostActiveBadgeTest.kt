package com.skedgo.tripkit.ui.tripresults

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ContextThemeWrapper
import com.skedgo.tripkit.ui.databinding.TripResultListItemBinding
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.TripKit
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MostActiveBadgeTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val context get() = RuntimeEnvironment.getApplication()

    @After fun tearDown() { unmockkAll() }

    private fun viewModel(): TripResultViewModel {
        // Keep the global singleton and notification setup out of this badge test.
        mockkObject(TripKit.Companion)
        val kit = mockk<TripKit>()
        val configs = mockk<Configs>(relaxed = true)
        every { TripKit.getInstance() } returns kit
        every { kit.configs() } returns configs
        every { configs.hasTripLabels() } returns true
        return TripResultViewModel(context, mockk(relaxed = true), mockk(relaxed = true),
            context.resources, mockk(relaxed = true))
    }

    @Test fun `active category is presented as Most Active with its existing style`() {
        val vm = viewModel()
        val group = TripGroup().apply { addTrip(Trip().apply { segmentList = arrayListOf(TripSegment()) }) }
        vm.setTripGroup(context, group, TripGroupClassifier.Classification.HEALTHIEST)
        assertThat(vm.badgeText.value).isEqualTo("Most Active")
        assertThat(vm.badgeVisible.value).isTrue()
        assertThat(vm.badgeDrawable.value).isNotNull()
        assertThat(vm.badgeTextColor.value).isEqualTo(context.getColor(R.color.classification_healthiest))
        // Badge TextView has no overriding content description: TalkBack uses this text.
        val themed = ContextThemeWrapper(context, androidx.appcompat.R.style.Theme_AppCompat)
        val binding = TripResultListItemBinding.inflate(LayoutInflater.from(themed))
        binding.viewModel = vm
        binding.executePendingBindings()
        val badge = binding.badgeText
        assertThat(badge.text.toString()).isEqualTo("Most Active")
        assertThat(badge.contentDescription).isNull()
        vm.setTripGroup(context, group, TripGroupClassifier.Classification.NONE)
        assertThat(vm.badgeText.value).isNull()
        assertThat(vm.badgeVisible.value).isFalse()
    }

    @Test fun `neighboring badge terms remain unchanged`() {
        val vm = viewModel()
        val group = TripGroup().apply { addTrip(Trip().apply { segmentList = arrayListOf(TripSegment()) }) }
        listOf(
            TripGroupClassifier.Classification.EASIEST to "Easiest",
            TripGroupClassifier.Classification.FASTEST to "Fastest",
            TripGroupClassifier.Classification.CHEAPEST to "Cheapest",
            TripGroupClassifier.Classification.GREENEST to "Greenest",
            TripGroupClassifier.Classification.RECOMMENDED to "Recommended"
        ).forEach { (category, expected) ->
            vm.setTripGroup(context, group, category)
            assertThat(vm.badgeText.value).isEqualTo(expected)
        }
    }

    @Test fun `untranslated locales fall back to current English and Spanish is preserved`() {
        listOf("en", "ar", "da", "de", "fi", "fr", "it", "ja", "ko", "nl",
            "no", "pl", "pt", "ru", "sv", "tr", "zh", "en-GB", "zz").forEach { tag ->
            val configuration = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            }
            val localized = context.createConfigurationContext(configuration)
            assertThat(localized.getString(R.string.healthiest)).describedAs(tag).isEqualTo("Most Active")
        }
        val spanish = Configuration(context.resources.configuration).apply { setLocale(Locale("es")) }
        assertThat(context.createConfigurationContext(spanish).getString(R.string.healthiest)).isEqualTo("Más activo")
    }
}
