package com.skedgo.tripkit.ui.trippreview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentsSummaryData
import javax.inject.Inject

class TripPreviewSharedViewModel @Inject constructor() : RxViewModel() {

    private val _pageIndex = MutableLiveData<Pair<Long, String>>()
    val pageIndex: LiveData<Pair<Long, String>> = _pageIndex

    private val _previewHeader = MutableLiveData<TripSegmentsSummaryData>()
    val previewHeader: LiveData<TripSegmentsSummaryData> = _previewHeader

    fun setPageIndex(segmentId: Long, transportModeId: String) {
        _pageIndex.value = segmentId to transportModeId
    }

    fun setPreviewHeader(preview: TripSegmentsSummaryData) {
        _previewHeader.value = preview
    }

}