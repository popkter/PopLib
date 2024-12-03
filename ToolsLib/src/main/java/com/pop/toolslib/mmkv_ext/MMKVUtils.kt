package com.popkter.common.mmkv_ext

import com.tencent.mmkv.MMKV

/**
 * 基于MMKV的数据存储工具
 */
class MMKVUtils private constructor() {

    companion object {
        val INSTANCE by lazy { MMKVUtils() }
    }

    private val mmkv = MMKV.defaultMMKV()

    fun <T> put(key: String, value: T) {
        when (value) {
            is Int -> mmkv.encode(key, value)
            is Long -> mmkv.encode(key, value)
            is Float -> mmkv.encode(key, value)
            is Double -> mmkv.encode(key, value)
            is Boolean -> mmkv.encode(key, value)
            is String -> mmkv.encode(key, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, defValue: T): T {
        return when (defValue) {
            is Int -> mmkv.decodeInt(key, defValue) as T
            is Long -> mmkv.decodeLong(key, defValue) as T
            is Float -> mmkv.decodeFloat(key, defValue) as T
            is Double -> mmkv.decodeDouble(key, defValue) as T
            is Boolean -> mmkv.decodeBool(key, defValue) as T
            is String -> mmkv.decodeString(key, defValue) as T
            else -> defValue
        }
    }
}