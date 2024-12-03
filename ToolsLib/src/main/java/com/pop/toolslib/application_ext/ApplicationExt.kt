package com.popkter.common.application_ext

import android.app.Application
import androidx.annotation.CallSuper
import com.tencent.mmkv.MMKV

open class ApplicationExt: Application() {

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        ApplicationModule.init(this)
        MMKV.initialize(this)
    }

}