package com.popkter.common.application_ext

import android.app.Application

/**
 * 提供全局的Application实例
 */
class ApplicationModule {

    companion object {
        const val TAG = "Application"

        fun init(application: Application) {
            this.application = application
        }

        private lateinit var mApplication: Application

        var application: Application
            get() = run {
                if (this::mApplication.isInitialized) {
                    mApplication
                } else {
                    throw IllegalStateException("Application instance is not initialized.")
                }
            }
            set(value) {
                if (this::mApplication.isInitialized.not()) {
                    mApplication = value
                } else {
                    throw IllegalStateException("Application instance is already initialized.")
                }
            }

    }
}