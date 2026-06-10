package org.openintents.distribution.about;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;

/* To make the app work with both meta-data tags and about.xml files */
public class MetaDataReader {
    public static final String ELEM_ABOUT = "about";
    public static final String SCHEMA = "http://schemas.openintents.org/android/about";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_RESOURCE = "resource";
    private static final String TAG = MetaDataReader.class.getSimpleName();

    private Context ctx;
    private String packagename;
    private Map<String, String> tagNameToMetadataName;
    private Bundle bundle;

    public MetaDataReader(Context context, String packagename, Map<String, String> tagNameToMetadataName)
            throws NameNotFoundException {
        this.ctx = context;
        this.packagename = packagename;
        this.tagNameToMetadataName = tagNameToMetadataName;
        //Check if the packagename exists
        ctx.getPackageManager().getApplicationInfo(packagename, PackageManager.GET_META_DATA);
    }

    public Bundle getBundle() {
        if (bundle == null)
            bundle = createBundle();
        return bundle;
    }

    private PackageManager getPackageManager() {
        return ctx.getPackageManager();
    }

    private Bundle createBundle() {
        Bundle manifestMD = getManifestMetaData();
        if (manifestMD == null)
            return null;

        if (manifestMD.containsKey(AboutMetaData.METADATA_ABOUT)) {
            int id = manifestMD.getInt(AboutMetaData.METADATA_ABOUT, -1);
            if (id == -1)
                return null;
            XmlResourceParser xml;
            try {
                xml = ctx.getPackageManager().getResourcesForApplication(packagename).getXml(id);
            } catch (NotFoundException e) {
                Log.d("error", "About.xml file not found.");
                return null;
            } catch (NameNotFoundException e) {
                // Should never get here
                return null;
            }
            if (xml == null)
                return null;
            return createAboutFileBundle(xml);
        } else {
            /*Bundle ret = new Bundle(manifestMD.size());
			Set<String> keySet = manifestMD.keySet();
			for(String key : keySet){
				String newKey = key;
				if(metaDataNameToTagName.containsKey(key)){
					newKey = metaDataNameToTagName.get(key);
				}
				Parcelable value = manifestMD.getParcelable(key);
				ret.putParcelable(newKey, value);
			}
			return ret;*/
            return manifestMD;
        }
    }

    private Bundle createAboutFileBundle(XmlResourceParser xml) {
        Bundle bundle = new Bundle();
        PackageManager pm = getPackageManager();
        Resources resources = null;
        try {
            resources = pm.getResourcesForApplication(packagename);
        } catch (NameNotFoundException e) {
            // It should never get here, the packagename existence is checked in constructor.
        }

        boolean inAbout = false;

        try {
            int tagType = xml.next();
            while (XmlPullParser.END_DOCUMENT != tagType) {
                String name = xml.getName();
                if (XmlPullParser.START_TAG == tagType) {
                    if (inAbout == true) {
                        if (tagNameToMetadataName.containsKey(name)) {
                            AttributeSet attr = Xml.asAttributeSet(xml);
                            String resIdName = attr.getAttributeValue(SCHEMA, ATTR_RESOURCE);
                            if (resIdName != null) {
                                int resId = 0;
                                if (resIdName.startsWith("@")) {
                                    // oi:resource="@type/name"
                                    resId = resources.getIdentifier(resIdName.substring(1), // Cut the @
                                            null, packagename);
                                } else {
                                    // oi:resource="123456"
                                    try {
                                        resId = Integer.parseInt(resIdName);
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                                bundle.putInt(tagNameToMetadataName.get(name), resId);
                            } else {
                                String value = attr.getAttributeValue(SCHEMA, ATTR_VALUE);
                                if (value.startsWith("@")) {
                                    // oi:value="@type/name"
                                    int valId = resources.getIdentifier(value.substring(1), null, packagename);
                                    if (value.startsWith("@string/")) {
                                        // oi:value="@string/name"
                                        String valString = resources.getString(valId);
                                        bundle.putString(tagNameToMetadataName.get(name), valString);
                                    } else {
                                        //Cannot process the type, treat it as a resource
                                        // oi:value="@integer/name" or other resource types
                                        Log.w(TAG, String.format("attribute %s must be a string or string resource", name));
                                        bundle.putInt(tagNameToMetadataName.get(name), valId);
                                    }
                                } else {
                                    // oi:value="a value"
                                    bundle.putString(tagNameToMetadataName.get(name), value);
                                }
                            }
                        }
                    }
                    if (ELEM_ABOUT.equals(name)) {
                        inAbout = true;
                    }
                } else if (XmlPullParser.END_TAG == tagType) {
                    if (ELEM_ABOUT.equals(name)) {
                        inAbout = false;
                        //Allow only one <about> tag
                        break;
                    }
                }
                tagType = xml.next();
            }
        } catch (XmlPullParserException ex) {
            Log.d("About.xml", "XmlPullParserException");
        } catch (IOException ex) {
            Log.d("About.xml", "IOException");
        }

        xml.close();

        return bundle;
    }

    private Bundle getManifestMetaData() {
        try {
            return ctx.getPackageManager().getApplicationInfo(packagename,
                    PackageManager.GET_META_DATA).metaData;
        } catch (NameNotFoundException e) {
            //It will never get here, the packagename validity is checked in the constructor
            return null;
        }
    }
}
