package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.gson.Gson
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.TripSegmentBinding
import me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter

private const val TAG = "TripSegmentRVAdapter"

class TripSegmentCustomRecyclerViewAdapter<T> : BindingRecyclerViewAdapter<T>() {

    lateinit var context: Context

    override fun onCreateBinding(
        inflater: LayoutInflater, @LayoutRes layoutId: Int,
        viewGroup: ViewGroup
    ): ViewDataBinding {
        return super.onCreateBinding(inflater, layoutId, viewGroup).apply {
            Log.e(TAG, "created binding: $this")
        }
    }

    override fun onBindBinding(
        binding: ViewDataBinding,
        variableId: Int, @LayoutRes layoutRes: Int,
        position: Int,
        item: T
    ) {
        super.onBindBinding(binding, variableId, layoutRes, position, item)

        context = binding.root.context

        if (item is TripSegmentItemViewModel) {
            item.generateRoadTags()
            item.roadTagChartItems.observe(binding.lifecycleOwner!!) { chartItems ->
                with(binding as TripSegmentBinding) {
                    val segmentLength = item.tripSegment?.metres ?: chartItems.maxOf { it.length }
                        .roundToNearestHundred()
                    val max = segmentLength
                    val middle = max / 2
                    val items = chartItems.map {
                        it.maxProgress = max
                        it
                    }.sortedBy { it.index }

                    RoadTagChartAdapter().let { adapter ->
                        binding.layoutRoadTags.rvRoadTagsChart.adapter = adapter
                        adapter.collection = listOf(
                            RoadTagChart(
                                max = max,
                                middle = middle,
                                items = items
                            )
                        )
                    }
                }
            }
        }
    }

    private fun List<RoadTagChartItem>.generateLabels(): List<String> {
        val result = mutableListOf<String>()
        var labelIndex = 0
        forEach { roadTagChartItem ->
            result.add(labelIndex, roadTagChartItem.label)
            result.add(labelIndex + 1, " ")
            labelIndex += 2
        }
        Log.e("RoadTagLabels", Gson().toJson(result))
        return result
    }

    private fun Int.roundToNearestHundred(): Int {
        val remainder = this % 100
        return if (remainder < 50) {
            this - remainder
        } else {
            this + (100 - remainder)
        }
    }

    private fun List<RoadTagChartItem>.generateDataSets(): List<IBarDataSet> {
        val result = mutableListOf<BarDataSet>()
        forEachIndexed { index, roadTagChartItem ->
            Log.e("RoadTag", Gson().toJson(roadTagChartItem))
            result.add(roadTagChartItem.generateDataSet(index * 2))
        }
        return result
    }

    private fun RoadTagChartItem.generateDataSet(index: Int): BarDataSet {
        Log.e("RoadTagEntry", "index $index")
        val barEntry = BarEntry(index.toFloat(), this.length.toFloat())
        val dataSet = BarDataSet(listOf(barEntry), null)
        dataSet.color = this.color
        return dataSet
    }

    private fun HorizontalBarChart.configureForSegments(
        labels: List<String>,
        axisLeftGranularity: Float
    ) {
        setDrawValueAboveBar(true)

        // Customize the X-axis (bottom axis)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        //xAxis.granularity = 1f
        // Add labels to the bars (optional)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        axisRight.isEnabled = false

        // Customize the Y-axis (left axis)
        axisLeft.setDrawGridLines(false)
        axisLeft.setDrawAxisLine(true)
        //axisLeft.mAxisMinimum = 0f
        axisLeft.granularity = axisLeftGranularity


        isDoubleTapToZoomEnabled = false
        setPinchZoom(false)
    }
}