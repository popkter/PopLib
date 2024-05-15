package com.pop.viewlib.printer_textview

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.animation.doOnEnd
import kotlinx.coroutines.*

/**
 * TextView that implements the printer effect
 */
class TextViewPrinter : androidx.appcompat.widget.AppCompatTextView {

    companion object {
        private const val DEFAULT_DATA = "default"
        private const val TEXT_DATA = "text"
    }

    private var mText: CharSequence = ""
    private var mCurrentIndex = 0

    private lateinit var mTextAnimator: ValueAnimator
    private lateinit var mJob: Job

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

    /**
     * setText to TextView
     * [text] the Text you want to display
     * [interpolator] Interpolator, default is AccelerateDecelerateInterpolator
     * [singleCharDuration] single char load duration, default is 200L
     * [modifyPropOnLoad] apply modify text span When Load
     * [modifyPropOnEnd] apply modify text span after Load
     */
    fun updateText(
        text: CharSequence,
        interpolator: Interpolator = AccelerateDecelerateInterpolator(),
        singleCharDuration: Long = 200L,
        modifyPropOnEnd: Boolean = true,
        modifyPropOnLoad: Boolean = true
    ) {
        mText = text
        mCurrentIndex = 0
        if (this::mJob.isInitialized) mJob.cancel()
        if (this::mTextAnimator.isInitialized) mTextAnimator.cancel()
        mJob = CoroutineScope(Dispatchers.Main).launch {
            mTextAnimator = ValueAnimator.ofInt(0, mText.length - 1)
            with(mTextAnimator) {
                this.addUpdateListener {
                    val index = it.animatedValue as Int
                    if (mCurrentIndex != index && index < mText.length) {
                        mCurrentIndex = index
                        if (modifyPropOnLoad) {
                            this@TextViewPrinter.text = mText.subSequence(0, index + 1)
                        } else {
                            this@TextViewPrinter.text = mText.substring(0, index + 1)
                        }
                    }
                }
                this.duration = mText.length * singleCharDuration
                this.interpolator = interpolator
                doOnEnd {
                    if (!modifyPropOnLoad && modifyPropOnEnd) {
                        this@TextViewPrinter.text = mText
                    }
                    removeAllUpdateListeners()
                }
            }
            mTextAnimator.start()
            cancel()
        }

    }

    override fun onSaveInstanceState(): Parcelable {
        if (this::mJob.isInitialized) mJob.cancel()
        if (this::mTextAnimator.isInitialized) mTextAnimator.cancel()
        text = mText
        return Bundle().apply {
            val state = super.onSaveInstanceState()
            putParcelable(DEFAULT_DATA, state)
            putCharSequence(TEXT_DATA, mText)
        }
    }


    override fun onRestoreInstanceState(state: Parcelable?) {
        with(state as Bundle) {
            mText = getCharSequence(TEXT_DATA, "")
            text = mText
            super.onRestoreInstanceState(getParcelable(DEFAULT_DATA))
        }
    }


}