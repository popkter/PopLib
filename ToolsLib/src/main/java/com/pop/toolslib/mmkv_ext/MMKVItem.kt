package com.popkter.common.mmkv_ext

import android.util.Log
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 基于MMKV的数据存储对象，委托实现数据的getter和setter
 * ``` kotlin
 *    //新建对象
 *    var item  by  MmkvItem("key", 0)
 *    //setter
 *    item = 1
 *    //getter
 *    item
 * ```
 */
class MMKVItem<T>(private val key: String, private val def: T) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return MMKVUtils.INSTANCE.get(key, def).also {
            Log.v(TAG, "getValue: key= $key value= $it")
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        MMKVUtils.INSTANCE.put(key, value).also {
            Log.v(TAG, "setValue: key= $key value= $value")
        }
    }
}

private const val TAG  = "MMKVItem"