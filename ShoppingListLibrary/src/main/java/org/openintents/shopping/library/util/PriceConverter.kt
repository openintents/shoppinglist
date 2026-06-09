/*
 * Copyright (C) 2007-2011 OpenIntents.org
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

package org.openintents.shopping.library.util

import android.text.TextUtils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

class PriceConverter {

    companion object {
        @JvmField
        var mPriceFormatter: NumberFormat = DecimalFormat.getNumberInstance(Locale.ENGLISH)

        private var initialized: Boolean = false

        @JvmStatic
        fun init() {
            mPriceFormatter.maximumFractionDigits = 2
            mPriceFormatter.minimumFractionDigits = 2
            initialized = true
        }

        @JvmStatic
        fun getCentPriceFromString(price: String?): Long? {
            if (!initialized) {
                init()
            }
            val priceLong: Long?
            if (TextUtils.isEmpty(price)) {
                priceLong = 0L
            } else {
                priceLong = try {
                    Math.round(100 * mPriceFormatter.parse(price!!)!!.toDouble())
                } catch (e: ParseException) {
                    null
                }
            }
            return priceLong
        }

        @JvmStatic
        fun getStringFromCentPrice(pricecent: Long): String {
            if (!initialized) {
                init()
            }
            var price = mPriceFormatter.format(pricecent * 0.01)
            if (pricecent == 0L) {
                // Empty field for easier editing
                // (Otherwise "0.00" has to be deleted manually first)
                price = ""
            }
            return price
        }
    }
}
