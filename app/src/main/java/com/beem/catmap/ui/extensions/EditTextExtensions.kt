package com.beem.catmap.ui.extensions // Projendeki paket yoluna göre düzenleyebilirsin

import android.text.InputFilter
import android.widget.EditText

fun EditText.applyInputLimits(maxLength: Int = 280, maxLines: Int = 5) {
    val maxLinesFilter = InputFilter { source, start, end, dest, dstart, dend ->
        val newText = dest.subSequence(0, dstart).toString() +
                source.subSequence(start, end) +
                dest.subSequence(dend, dest.length)

        if (newText.count { it == '\n' } >= maxLines) {
            ""
        } else {
            null
        }
    }

    this.filters = arrayOf(
        InputFilter.LengthFilter(maxLength),
        maxLinesFilter
    )
}