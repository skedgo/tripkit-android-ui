package com.skedgo.tripkit.ui.search

sealed class SearchErrorType(val searchText: String? = null) {

  data class NoResults(val query: String? = null) : SearchErrorType(query)
  object NoConnection : SearchErrorType()
  object OtherError : SearchErrorType()

}