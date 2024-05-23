package com.skedgo.tripkit.ui.utils

import android.location.Location

object TripSearchUtils {

    var dateTimeQuery: Long = 0L

}

object LocationUtil {

    /**
     * Computes the Levenshtein distance between two strings. The Levenshtein distance is a measure of the difference between two sequences.
     * It is the minimum number of single-character edits (insertions, deletions, or substitutions) required to change one word into the other.
     *
     * @param str1 The first string to compare.
     * @param str2 The second string to compare.
     * @return The computed Levenshtein distance as an Int.
     */

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

    /**
     * Calculates the textual relevance of one string to another.
     *
     * @param query the search query, typed by the user.
     * @param searchResult the text against which the query is matched.
     * @return a Double between 0 and 1, indicating the relevance of the searchResult to the query.
     */

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
                            computeLevenshteinDistance(queryWord, searchResultWord)
                        )
                    }
                    relevance += 0.5 / (queryWords.size + minDistance)
                }
            }
        }
        return relevance
    }

    /**
     * Computes the maximum textual relevance between two strings, in both directions.
     *
     * @param firstLocationString the first string to compare.
     * @param secondLocationString the second string to compare.
     * @return the maximum relevance score as a Double, between 0 and 1.
     */

    fun getRelevancePoint(firstLocationString: String, secondLocationString: String): Double {
        return relevance(firstLocationString, secondLocationString).coerceAtLeast(
            relevance(
                secondLocationString,
                firstLocationString
            )
        )
    }


    /**
     * Calculates the distance between two points on Earth (specified in decimal degrees) and checks if it is less than 100 meters.
     *
     * @param lat1 Latitude of the first location.
     * @param lon1 Longitude of the first location.
     * @param lat2 Latitude of the second location.
     * @param lon2 Longitude of the second location.
     * @return True if the distance is less than 100 meters, false otherwise.
     */
    fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}