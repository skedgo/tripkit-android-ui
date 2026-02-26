package com.skedgo.tripkit.ui.search

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.search.compose.SearchResultRow
import com.skedgo.tripkit.ui.search.compose.SearchResultRowUiModel

class SearchResultsComposeAdapter :
    ListAdapter<SearchResultRowUiModel, SearchResultsComposeAdapter.SearchResultViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<SearchResultRowUiModel>() {
        override fun areItemsTheSame(
            oldItem: SearchResultRowUiModel,
            newItem: SearchResultRowUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: SearchResultRowUiModel,
            newItem: SearchResultRowUiModel
        ): Boolean {
            return oldItem.title == newItem.title &&
                oldItem.subtitle == newItem.subtitle &&
                oldItem.matcher == newItem.matcher &&
                oldItem.titleTextColor == newItem.titleTextColor &&
                oldItem.subtitleTextColor == newItem.subtitleTextColor &&
                oldItem.shouldTintIcon == newItem.shouldTintIcon &&
                oldItem.groupKind == newItem.groupKind &&
                oldItem.isFirstInGroup == newItem.isFirstInGroup &&
                oldItem.isLastInGroup == newItem.isLastInGroup &&
                oldItem.showGroupTopSpacing == newItem.showGroupTopSpacing &&
                oldItem.showTimetableIcon == newItem.showTimetableIcon &&
                oldItem.showInfoIcon == newItem.showInfoIcon
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
        }
        return SearchResultViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SearchResultViewHolder(
        private val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {

        fun bind(item: SearchResultRowUiModel) {
            composeView.setContent {
                TripKitUITheme {
                    SearchResultRow(ui = item)
                }
            }
        }
    }
}
