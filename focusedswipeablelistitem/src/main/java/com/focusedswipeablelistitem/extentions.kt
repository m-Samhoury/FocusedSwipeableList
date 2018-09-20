package com.focusedswipeablelistitem

import android.view.View

inline fun View.dip(value: Int): Int = (value * (resources?.displayMetrics?.density ?: 0f)).toInt()
inline fun View.dip(value: Float): Int = (value * (resources?.displayMetrics?.density ?: 0f)).toInt()
