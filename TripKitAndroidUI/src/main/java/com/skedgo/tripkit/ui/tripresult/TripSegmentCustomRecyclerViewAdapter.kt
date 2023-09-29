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

                    val max = chartItems.maxOf { it.length }.roundToNearestHundred()
                    val middle = max / 2
                    val items = chartItems.map {
                        it.maxProgress = max
                        it
                    }

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

                    /*
                    val layoutParams = binding.layoutRoadTags.parent.layoutParams
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    binding.layoutRoadTags.parent.layoutParams = layoutParams

                    this.layoutRoadTags.chartRoadTags.let { horizontalBarChart ->

                        //val labels = mutableListOf<String>()
                        //labels.addAll(listOf("One", "", "Two", "", "Three", "", "Four"))

                        *//*
                        val entries = ArrayList<BarEntry>()
                        entries.add(BarEntry(0f, 4f))
                        entries.add(BarEntry(2f, 7f))
                        val entries2 = ArrayList<BarEntry>()
                        entries2.add(BarEntry(4f, 2f)) // Bar 3
                        entries2.add(BarEntry(6f, 3f)) // Bar 3

                        val dataSet = BarDataSet(entries, "")
                        dataSet.color = Color.BLUE

                        val dataSet2 = BarDataSet(entries2, "")
                        dataSet.color = Color.RED

                        val dataSets = ArrayList<IBarDataSet>()
                        dataSets.add(dataSet)
                        dataSets.add(dataSet2)

                        val data = BarData(dataSets)
                        *//*
                        *//*
                        val labels = chartItems.generateLabels()
                        val axisLeftGranularity = chartItems.maxOf {
                            it.length
                        }.roundToNearestHundred() / 2

                        horizontalBarChart.configureForSegments(
                            labels,
                            (axisLeftGranularity).toFloat()
                        )

                        val data = BarData(chartItems.generateDataSets())
                        data.barWidth = 1f

                        horizontalBarChart.data = data
                        horizontalBarChart.data.barWidth = 1f
                        horizontalBarChart.setFitBars(true)

                        // Customize the chart appearance
                        horizontalBarChart.description.isEnabled = false
                        horizontalBarChart.setDrawGridBackground(false)

                        // Set chart title
                        val description = Description()
                        description.text = ""
                        horizontalBarChart.description = description
                        horizontalBarChart.legend.isEnabled = false
                        horizontalBarChart.zoomOut()
                        horizontalBarChart.zoomOut()
                        *//*
                    }
                    */
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