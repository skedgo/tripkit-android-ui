package com.skedgo.tripkit.ui.utils

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.widget.TextView

fun TextView.highlightTexts(textsToHighLight: List<Pair<String, ClickableSpan>>) {
    val originalText = this.text
    val spannableString = SpannableString(originalText)

    textsToHighLight.forEach { pair ->
        val textToSpan = pair.first
        val start = originalText.indexOf(textToSpan, ignoreCase = true)
        val end = start + textToSpan.length

        spannableString.setSpan(
            pair.second,
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    this.text = spannableString
    this.movementMethod = LinkMovementMethod.getInstance()
}

fun TextView.highlightTexts(matcher: List<String>?, matchByWord: Boolean = true) {
    matcher?.forEach {
        try {
            val spannableString = SpannableString(text)

            if(matchByWord) {
                // Split the matcher string into individual words
                val words = it.split(" ")

                // Loop through each word and find its position in the text
                for (word in words) {
                    var spanStartPosition = 0

                    // Find the position of the word in the text
                    while (spanStartPosition != -1) {
                        spanStartPosition =
                            spannableString.indexOf(word, spanStartPosition, ignoreCase = true)

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
            } else {
                var spanStartPosition = 0

                // Find the position of the word in the text
                while (spanStartPosition != -1) {
                    spanStartPosition =
                        spannableString.indexOf(it, spanStartPosition, ignoreCase = true)

                    // If the word is found, bold it
                    if (spanStartPosition != -1) {
                        spannableString.setSpan(
                            StyleSpan(Typeface.BOLD),
                            spanStartPosition,
                            spanStartPosition + it.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        // Move to the next position to search for the same word
                        spanStartPosition += it.length
                    }
                }
            }

            text = spannableString
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }
}
