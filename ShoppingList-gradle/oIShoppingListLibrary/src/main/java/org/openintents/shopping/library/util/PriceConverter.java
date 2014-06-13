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

package org.openintents.shopping.library.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import android.text.TextUtils;

public class PriceConverter {

	public static NumberFormat mPriceFormatter = DecimalFormat
			.getNumberInstance(Locale.ENGLISH);

	private static boolean initialized = false;

	public static void init() {
		PriceConverter.mPriceFormatter.setMaximumFractionDigits(2);
		PriceConverter.mPriceFormatter.setMinimumFractionDigits(2);
		initialized = true;
	}

	public static Long getCentPriceFromString(String price) {
		if (!initialized) {
			init();
		}
		Long priceLong;
		if (TextUtils.isEmpty(price)) {
			priceLong = 0L;
		} else {
			try {
				priceLong = (long) Math
						.round(100 * PriceConverter.mPriceFormatter
								.parse(price).doubleValue());
			} catch (ParseException e) {
				priceLong = null;
			}
		}
		return priceLong;
	}

	public static String getStringFromCentPrice(long pricecent) {
		if (!initialized) {
			init();
		}
		String price = mPriceFormatter.format(pricecent * 0.01d);
		if (pricecent == 0) {
			// Empty field for easier editing
			// (Otherwise "0.00" has to be deleted manually first)
			price = "";
		}
		return price;
	}
}
