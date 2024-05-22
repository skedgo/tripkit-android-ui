package com.skedgo.tripkit.ui.utils

object TripSearchUtils {

    var dateTimeQuery: Long = 0L

}

object LocationUtil {
    private fun computeLevenshteinDistance(str1: String, str2: String): Int {
        val distance = Array(str1.length + 1) { IntArray(str2.length + 1) }
        for (i in 0..str1.length) {
            distance[i][0] = i
        }
        for (j in 1..str2.length) {
            distance[0][j] = j
        }
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                distance[i][j] = minOf(
                    distance[i - 1][j] + 1,
                    distance[i][j - 1] + 1,
                    distance[i - 1][j - 1] + if (str1[i - 1] == str2[j - 1]) 0 else 1
                )
            }
        }
        return distance[str1.length][str2.length]
    }

    private fun relevance(query: String, searchResult: String): Double {
        val queryLower = query.lowercase().trim()
        val searchResultLower = searchResult.lowercase().trim()

        if (searchResultLower == queryLower) {
            return 1.0
        }

        if (searchResultLower.startsWith(queryLower)) {
            return 0.85
        }

        val queryWords = queryLower.split(" ")
        val searchResultWords = searchResultLower.split(" ")
        var relevance = 0.0
        for (queryWord in queryWords) {
            when {
                searchResultWords.contains(queryWord) -> relevance += 0.8 / queryWords.size
                searchResultWords.any { it.startsWith(queryWord) } -> relevance += 0.7 / queryWords.size
                searchResultWords.any { it.contains(queryWord) } -> relevance += 0.6 / queryWords.size
                else -> {
                    var minDistance = Int.MAX_VALUE
                    for (searchResultWord in searchResultWords) {
                        minDistance = minOf(
                            minDistance,
                            LocationUtil.computeLevenshteinDistance(queryWord, searchResultWord)
                        )
                    }
                    relevance += 0.5 / (queryWords.size + minDistance)
                }
            }
        }
        return relevance
    }

    fun getRelevancePoint(firstLocationString: String, secondLocationString: String): Double {
        return relevance(firstLocationString, secondLocationString).coerceAtLeast(
            relevance(
                secondLocationString,
                firstLocationString
            )
        )
    }

}