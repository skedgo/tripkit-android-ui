package com.skedgo.tripkit.ui.search

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.databinding.BindingAdapter

// Added for highlighting, setting to BOLD, matched words in the TextView text
@BindingAdapter("matcher")
fun setMatcher(textView: TextView, matcher: String?) {
    matcher?.let {
        try {
            val spannableString = SpannableString(textView.text)

            // Split the matcher string into individual words
            val words = it.split(" ")

            // Loop through each word and find its position in the text
            for (word in words) {
                var spanStartPosition = 0

                // Find the position of the word in the text
                while (spanStartPosition != -1 && word.isNotEmpty()) {
                    spanStartPosition = spannableString.indexOf(word, spanStartPosition, ignoreCase = true)

                    // If the word is found, bold it
                    if (spanStartPosition != -1) {
                        spannableString.setSpan(
                            StyleSpan(Typeface.BOLD),
                            spanStartPosition,
                            spanStartPosition + word.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        // Move to the next position to search for the same word
                        spanStartPosition += word.length
                    }
                }
            }

            textView.text = spannableString
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }
}

@BindingAdapter("matcher")
fun setMatcher(textView: TextView, matcher: List<String>?) {
    matcher?.forEach {
        try {
            val spannableString = SpannableString(textView.text)

            // Split the matcher string into individual words
            val words = it.split(" ")

            // Loop through each word and find its position in the text
            for (word in words) {
                var spanStartPosition = 0

                // Find the position of the word in the text
                while (spanStartPosition != -1) {
                    spanStartPosition = spannableString.indexOf(word, spanStartPosition, ignoreCase = true)

                    // If the word is found, bold it
                    if (spanStartPosition != -1) {
                        spannableString.setSpan(
                            StyleSpan(Typeface.BOLD),
                            spanStartPosition,
                            spanStartPosition + word.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        // Move to the next position to search for the same word
                        spanStartPosition += word.length
                    }
                }
            }

            textView.text = spannableString
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }
}