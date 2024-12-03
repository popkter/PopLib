package com.pop.toolslib.click_ext

import android.view.View

/**
 * 单击防抖，默认初始的延迟500ms
 * [delayMillis] 屏蔽时长
 * [onClick] 点击事件
 */
inline fun View.setOnSingleClickListener(delayMillis: Long = 500L, crossinline onClick: () -> Unit) {
    this.setOnClickListener {
        isClickable = false
        onClick()
        postDelayed({ isClickable = true }, delayMillis)
    }
}

/**
 * 长按防抖，默认初始的延迟500ms
 * [delayMillis] 屏蔽时长
 * [onLongClick] 长按事件
 */
inline fun View.setOnLongSingleClickListener(delayMillis: Long = 500L, crossinline onLongClick: () -> Boolean) {
    this.setOnLongClickListener {
        isClickable = false
        val result = onLongClick()
        postDelayed({ isClickable = true }, delayMillis)
        result
    }
}
