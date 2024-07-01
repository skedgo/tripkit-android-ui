package com.skedgo.tripkit.ui.utils

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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