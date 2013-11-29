package org.openintents.shopping.provider;

import android.text.TextUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class PriceConverter {

	public static NumberFormat mPriceFormatter = DecimalFormat.getNumberInstance(Locale.ENGLISH);

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
				priceLong = (long) Math.round(100 * PriceConverter.mPriceFormatter.parse(price).doubleValue());
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
