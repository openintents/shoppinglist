/*
 * Copyright (C) 2010 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.shopping.theme

import android.content.Context
import android.content.res.TypedArray

/**
 * There were trouble retrieving several attributes at once in
 * obtainStyledAttributes - that's why this class tries to retrieve one
 * attribute at a time.
 *
 * @author Peli
 */
class ThemeAttributes(
    private val mContext: Context,
    private val mPackageName: String,
    private val mThemeId: Int
) {

    fun getBoolean(attrName: String, defaultValue: Boolean): Boolean {
        val attr = ThemeUtils.getAttributeIds(mContext, arrayOf(attrName), mPackageName)
        val a: TypedArray = mContext.obtainStyledAttributes(mThemeId, attr)
        val b = a.getBoolean(0, defaultValue)
        a.recycle()
        return b
    }

    fun getColor(attrName: String, defaultValue: Int): Int {
        val attr = ThemeUtils.getAttributeIds(mContext, arrayOf(attrName), mPackageName)
        val a: TypedArray = mContext.obtainStyledAttributes(mThemeId, attr)
        val c = a.getColor(0, defaultValue)
        a.recycle()
        return c
    }

    fun getDimensionPixelOffset(attrName: String, defaultValue: Int): Int {
        val attr = ThemeUtils.getAttributeIds(mContext, arrayOf(attrName), mPackageName)
        val a: TypedArray = mContext.obtainStyledAttributes(mThemeId, attr)
        val i = a.getDimensionPixelOffset(0, defaultValue)
        a.recycle()
        return i
    }

    fun getInteger(attrName: String, defaultValue: Int): Int {
        val attr = ThemeUtils.getAttributeIds(mContext, arrayOf(attrName), mPackageName)
        val a: TypedArray = mContext.obtainStyledAttributes(mThemeId, attr)
        val i = a.getInteger(0, defaultValue)
        a.recycle()
        return i
    }

    fun getResourceId(attrName: String, defaultValue: Int): Int {
        val attr = ThemeUtils.getAttributeIds(mContext, arrayOf(attrName), mPackageName)
        val a: TypedArray = mContext.obtainStyledAttributes(mThemeId, attr)
        val i = a.getResourceId(0, defaultValue)
        a.recycle()
        return i
    }

    fun getString(attrName: String): String? {
        val attr = ThemeUtils.getAttributeIds(mContext, arrayOf(attrName), mPackageName)
        val a: TypedArray = mContext.obtainStyledAttributes(mThemeId, attr)
        val s = a.getString(0)
        a.recycle()
        return s
    }
}
