package com.pop.viewlib.dynamic_dialog

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import com.pop.viewlib.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.WeakHashMap
import kotlin.math.min

private val viewTimerJobMap = WeakHashMap<View, Job?>()

// 使用扩展属性为 View 添加 timerJob 属性
internal var View.timerJob: Job?
    get() = viewTimerJobMap[this]
    set(value) {
        viewTimerJobMap[this] = value
    }

internal fun View.dismiss() {
    stopTimer()
    doOnDismissStart?.invoke()
    setOnTouchListener(null)
    ValueAnimator.ofInt(showCoordinateY, hideCoordinateY).apply {
        this.interpolator = dismissAnimatorInterpolator
        this.duration = min((-hideCoordinateY) * 2L, 500L)
        this.addUpdateListener { animator ->
            val temY = animator.animatedValue as Int

            if (this@dismiss.isAttachedToWindow) {
                if (temY <= 0) {
                    this@dismiss.y = temY.toFloat()
                } else {
                    windowManager?.updateViewLayout(this@dismiss, windowLayoutParams?.apply {
                        y = temY
                    })
                }
            }
            doOnEnd {
                if (isAttachedToWindow) {
                    windowManager?.removeViewImmediate(this@dismiss)
                }
                doOnDismissEnd?.invoke()
            }
        }
    }.start()
}

internal fun View.resetDismissTimer() {
    this.timerJob?.cancel()
    this.timerJob = CoroutineScope(Dispatchers.Main).launch {
        delay(dismissDuration)
        dismiss()
    }
}

internal fun View.stopTimer() {
    this.timerJob?.cancel()
    this.timerJob = null
}

internal val View.windowManager: WindowManager?
    get() {
        return context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

internal var View.doOnDismissStart: (() -> Unit)?
    get() {
        val lambdaWrapper = getTag(R.string.doOnAnimatorStart) as? LambdaWrapper
        return lambdaWrapper?.lambda
    }
    set(value) {
        setTag(R.string.doOnAnimatorStart, value?.let { LambdaWrapper(it) })
    }

internal var View.doOnDismissEnd: (() -> Unit)?
    get() {
        val lambdaWrapper = getTag(R.string.doOnDismissEnd) as? LambdaWrapper
        return lambdaWrapper?.lambda
    }
    set(value) {
        setTag(R.string.doOnDismissEnd, value?.let { LambdaWrapper(it) })
    }

internal var View.hideCoordinateY: Int
    get() = getTag(R.string.hideCoordinateY) as? Int ?: 0
    set(value) {
        setTag(R.string.hideCoordinateY, value)
    }
internal var View.showCoordinateY: Int
    get() = getTag(R.string.showCoordinateY) as? Int ?: 0
    set(value) {
        setTag(R.string.showCoordinateY, value)
    }

internal var View.startAnimatorInterpolator: Interpolator
    get() =
        getTag(R.string.startAnimatorInterpolator) as? Interpolator ?: OvershootInterpolator(0.5F)
    set(value) {
        setTag(R.string.startAnimatorInterpolator, value)
    }

internal var View.dismissAnimatorInterpolator: Interpolator
    get() =
        getTag(R.string.dismissAnimatorInterpolator) as? Interpolator
            ?: AnticipateOvershootInterpolator(1F)
    set(value) {
        setTag(R.string.dismissAnimatorInterpolator, value)
    }

internal var View.dismissDuration: Long
    get() = getTag(R.string.dismissDuration) as? Long ?: 2000
    set(value) = setTag(R.string.dismissDuration, value)


internal var View.windowLayoutParams: WindowManager.LayoutParams?
    get() = getTag(R.string.windowLayoutParams) as? WindowManager.LayoutParams
    set(value) = setTag(R.string.windowLayoutParams, value)

data class LambdaWrapper(val lambda: () -> Unit)

sealed class FvState(val state: Int) {
    data object Init : FvState(-1)
    data object InShow : FvState(0)
    data object OnShow : FvState(1)
    data object InHide : FvState(2)
    data object OnHide : FvState(3)

}