package org.openintents.shopping.provider;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class ThemeUtils2 {

	public static class LiveViewContainerView extends FrameLayout {

		public LiveViewContainerView(Context context) {
			super(context);
		}
		
		public void drawNow(Canvas canvas){
			dispatchDraw(canvas);
		}

	}

	private static final String TAG = "ThemeUtils2";
	public static String mTextTypeface;
	public static Typeface mCurrentTypeface;
	public static boolean mTextUpperCaseFont;
	public static int mTextColor;
	public static int mTextColorPrice;
	public static float mTextSize;
	public static int mTextColorChecked;
	public static boolean mShowCheckBox;
	public static boolean mShowStrikethrough;
	public static String mTextSuffixUnchecked;
	public static String mTextSuffixChecked;
	public static LiveViewContainerView mThemedBackground;
	public static int mBackgroundPadding;
	public static Drawable mDefaultDivider;
	public static Bitmap mBackgrounDrawable;

	public static void setLocalStyle(Context ctx, int styleResId, int size) {

		String styleName;
		switch (styleResId) {

		case 1:
			styleName = "Theme.ShoppingList.Classic";
			break;
		case 2:
			styleName = "Theme.ShoppingList.Android";
			break;
		case 0:
			;
		default:
			styleName = "Theme.ShoppingList";
		}
		boolean themefound = setRemoteStyle(ctx, "org.openintents.shopping:"
				+ styleName, size, false);

		if (!themefound) {
			// Actually this should never happen.
			Log.e(TAG, "Local theme not found: " + styleName);
		}
	}

	public static boolean setRemoteStyle(Context ctx, String styleName, int size,
			boolean debug) {
		if (TextUtils.isEmpty(styleName)) {
			if (debug)
				Log.e(TAG, "Empty style name: " + styleName);
			return false;
		}

		PackageManager pm = ctx.getPackageManager();

		String packageName = ThemeUtils.getPackageNameFromStyle(styleName);

		if (packageName == null) {
			Log.e(TAG, "Invalid style name: " + styleName);
			return false;
		}

		Context c = null;
		try {
			c = ctx.createPackageContext(packageName, 0);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Package for style not found: " + packageName + ", "
					+ styleName);
			return false;
		}

		Resources res = c.getResources();

		int themeid = res.getIdentifier(styleName, "style", null);

		if (themeid == 0) {
			Log.e(TAG, "Theme name not found: " + styleName);
			return false;
		}

		try {
			ThemeAttributes ta = new ThemeAttributes(c, packageName, themeid);

			mTextTypeface = ta.getString(ThemeShoppingList.textTypeface);
			mCurrentTypeface = null;

			// Look for special cases:
			if ("monospace".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.MONOSPACE,
						Typeface.NORMAL);
			} else if ("sans".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.SANS_SERIF,
						Typeface.NORMAL);
			} else if ("serif".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.SERIF,
						Typeface.NORMAL);
			} else if (!TextUtils.isEmpty(mTextTypeface)) {

				try {
					Log.d(TAG, "Reading typeface: package: " + packageName
							+ ", typeface: " + mTextTypeface);
					Resources remoteRes = pm
							.getResourcesForApplication(packageName);
					mCurrentTypeface = Typeface.createFromAsset(
							remoteRes.getAssets(), mTextTypeface);
					Log.d(TAG, "Result: " + mCurrentTypeface);
				} catch (NameNotFoundException e) {
					Log.e(TAG, "Package not found for Typeface", e);
				}
			}

			mTextUpperCaseFont = ta.getBoolean(
					ThemeShoppingList.textUpperCaseFont, false);

			mTextColor = ta.getColor(ThemeShoppingList.textColor,
					android.R.color.white);

			mTextColorPrice = ta.getColor(ThemeShoppingList.textColorPrice,
					android.R.color.white);

			if (size == 0) {
				mTextSize = getTextSizeTiny(ctx, ta);
			} else if (size == 1) {
				mTextSize = getTextSizeSmall(ctx, ta);
			} else if (size == 2) {
				mTextSize = getTextSizeMedium(ctx, ta);
			} else {
				mTextSize = getTextSizeLarge(ctx, ta);
			}
			if (debug)
				Log.d(TAG, "textSize: " + mTextSize);

			mTextColorChecked = ta.getColor(ThemeShoppingList.textColorChecked,
					android.R.color.white);
			mShowCheckBox = ta.getBoolean(ThemeShoppingList.showCheckBox, true);
			mShowStrikethrough = ta.getBoolean(
					ThemeShoppingList.textStrikethroughChecked, false);
			mTextSuffixUnchecked = ta
					.getString(ThemeShoppingList.textSuffixUnchecked);
			mTextSuffixChecked = ta
					.getString(ThemeShoppingList.textSuffixChecked);

			
				mThemedBackground = new LiveViewContainerView(ctx);
				mThemedBackground.setLayoutParams(new LayoutParams(128, 128));
			
			
			if (mThemedBackground != null) {
				mBackgroundPadding = ta.getDimensionPixelOffset(
						ThemeShoppingList.backgroundPadding, -1);
				int backgroundPaddingLeft = ta.getDimensionPixelOffset(
						ThemeShoppingList.backgroundPaddingLeft,
						mBackgroundPadding);
				int backgroundPaddingTop = ta.getDimensionPixelOffset(
						ThemeShoppingList.backgroundPaddingTop,
						mBackgroundPadding);
				int backgroundPaddingRight = ta.getDimensionPixelOffset(
						ThemeShoppingList.backgroundPaddingRight,
						mBackgroundPadding);
				int backgroundPaddingBottom = ta.getDimensionPixelOffset(
						ThemeShoppingList.backgroundPaddingBottom,
						mBackgroundPadding);
				try {
					Resources remoteRes = pm
							.getResourcesForApplication(packageName);
					int resid = ta.getResourceId(ThemeShoppingList.background,
							0);
					if (resid != 0) {
						Drawable d = remoteRes.getDrawable(resid);
						mBackgrounDrawable  = BitmapFactory.decodeResource(remoteRes,resid);
						mThemedBackground.setBackgroundDrawable(d);
					} else {
						// remove background
						mThemedBackground.setBackgroundResource(0);
					}
				} catch (NameNotFoundException e) {
					Log.e(TAG, "Package not found for Theme background.", e);
				} catch (Resources.NotFoundException e) {
					Log.e(TAG, "Resource not found for Theme background.", e);
				}

				// Apply padding
				if (mBackgroundPadding >= 0 || backgroundPaddingLeft >= 0
						|| backgroundPaddingTop >= 0
						|| backgroundPaddingRight >= 0
						|| backgroundPaddingBottom >= 0) {
					mThemedBackground.setPadding(backgroundPaddingLeft,
							backgroundPaddingTop, backgroundPaddingRight,
							backgroundPaddingBottom);
				} else {
					// 9-patches do the padding automatically
					// todo clear padding
				}
			}

			int divider = ta.getInteger(ThemeShoppingList.divider, 0);

			Drawable div = null;
			if (divider > 0) {
				div = ctx.getResources().getDrawable(divider);
			} else if (divider < 0) {
				div = null;
			} else {
				div = mDefaultDivider;
			}

			// TODO in LiveView setDivider(div);

			return true;

		} catch (UnsupportedOperationException e) {
			// This exception is thrown e.g. if one attempts
			// to read an integer attribute as dimension.
			Log.e(TAG, "UnsupportedOperationException", e);
			return false;
		} catch (NumberFormatException e) {
			// This exception is thrown e.g. if one attempts
			// to read a string as integer.
			Log.e(TAG, "NumberFormatException", e);
			return false;
		}
	}

	private static float getTextSizeTiny(Context ctx, ThemeAttributes ta) {
		float size = ta.getDimensionPixelOffset(ThemeShoppingList.textSizeTiny,
				-1);
		if (size == -1) {
			// Try to obtain from small:
			size = (12f / 18f) * getTextSizeSmall(ctx, ta);
		}
		return size;
	}

	private static float getTextSizeSmall(Context ctx, ThemeAttributes ta) {
		float size = ta.getDimensionPixelOffset(
				ThemeShoppingList.textSizeSmall, -1);
		if (size == -1) {
			// Try to obtain from small:
			size = (18f / 23f) * getTextSizeMedium(ctx, ta);
		}
		return size;
	}

	private static float getTextSizeMedium(Context ctx, ThemeAttributes ta) {
		final float scale = ctx.getResources().getDisplayMetrics().scaledDensity;
		float size = ta.getDimensionPixelOffset(
				ThemeShoppingList.textSizeMedium, (int) (23 * scale + 0.5f));
		return size;
	}

	private static float getTextSizeLarge(Context ctx, ThemeAttributes ta) {
		float size = ta.getDimensionPixelOffset(
				ThemeShoppingList.textSizeLarge, -1);
		if (size == -1) {
			// Try to obtain from small:
			size = (28f / 23f) * getTextSizeMedium(ctx, ta);
		}
		return size;
	}

}
