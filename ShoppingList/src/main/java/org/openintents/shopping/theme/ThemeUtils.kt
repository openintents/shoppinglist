package org.openintents.shopping.theme

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.Log
import android.util.Xml

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.util.LinkedList

/**
 * Helper functions for retrieving remote themes, that are themes in external
 * packages.
 *
 * @author Peli
 */
class ThemeUtils {

    class ThemeInfo {
        @JvmField var packageName: String? = null
        @JvmField var title: String? = null
        @JvmField var styleName: String? = null
    }

    companion object {
        const val METADATA_THEMES = "org.openintents.themes"
        const val ELEM_THEMES = "themes"

        // For XML:
        const val ELEM_ATTRIBUTESET = "attributeset"
        const val ELEM_THEME = "theme"
        const val ATTR_NAME = "name"
        const val ATTR_TITLE = "title"
        const val ATTR_STYLE = "style"
        private const val TAG = "ThemeUtils"

        @JvmField
        var SCHEMA = "http://schemas.openintents.org/android/themes"

        @JvmStatic
        fun getAttributeIds(context: Context, attrNames: Array<String>, packageName: String): IntArray {
            val len = attrNames.size
            val res: Resources = context.resources
            val attrIds = IntArray(len)
            for (i in 0 until len) {
                attrIds[i] = res.getIdentifier(attrNames[i], "attr", packageName)
            }
            return attrIds
        }

        /**
         * Return list of all applications that contain the theme meta-tag.
         *
         * @param pm
         * @param firstPackage : package name of package that should be moved to front.
         * @return
         */
        private fun getThemePackages(pm: PackageManager, firstPackage: String): List<ApplicationInfo> {
            val appinfolist: MutableList<ApplicationInfo> = LinkedList()
            try {
                val allapps: List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (ai in allapps) {
                    if (ai.metaData != null) {
                        if (ai.metaData.containsKey(METADATA_THEMES)) {
                            if (ai.packageName == firstPackage) {
                                // Add this package at the beginning of the list
                                appinfolist.add(0, ai)
                            } else {
                                appinfolist.add(ai)
                            }
                        }
                    }
                }
                return appinfolist
            } catch (e: Exception) {
                // getInstalledApplications can throw android.os.TransactionTooLargeException (data > 1mb)
            }
            return appinfolist
        }

        private fun addThemeInfos(
            pm: PackageManager,
            attributeset: String,
            appinfo: ApplicationInfo,
            themeinfolist: MutableList<ThemeInfo>
        ) {
            val xml: XmlResourceParser = appinfo.loadXmlMetaData(pm, METADATA_THEMES)
            var useThisAttributeSet = false
            try {
                var tagType = xml.next()
                while (XmlPullParser.END_DOCUMENT != tagType) {
                    if (XmlPullParser.START_TAG == tagType) {
                        val attr: AttributeSet = Xml.asAttributeSet(xml)
                        if (xml.name == ELEM_THEMES) {
                            // nothing to do
                        } else if (xml.name == ELEM_ATTRIBUTESET) {
                            val name = attr.getAttributeValue(SCHEMA, ATTR_NAME)
                            useThisAttributeSet = name == attributeset
                        } else if (xml.name == ELEM_THEME) {
                            if (useThisAttributeSet) {
                                val ti = ThemeInfo()
                                ti.packageName = appinfo.packageName
                                val titleResId = attr.getAttributeResourceValue(SCHEMA, ATTR_TITLE, 0)
                                val styleResId = attr.getAttributeResourceValue(SCHEMA, ATTR_STYLE, 0)
                                try {
                                    val res: Resources = pm.getResourcesForApplication(appinfo)
                                    ti.title = res.getString(titleResId)
                                    ti.styleName = res.getResourceName(styleResId)
                                } catch (e: PackageManager.NameNotFoundException) {
                                    ti.title = ""
                                }
                                themeinfolist.add(ti)
                            }
                        }
                    } else if (XmlPullParser.END_TAG == tagType) {
                        // nothing to do
                    }
                    tagType = xml.next()
                }
            } catch (ex: XmlPullParserException) {
                Log.e(TAG, String.format(
                    "XML parse exception when parsing metadata for '%s': %s",
                    appinfo.packageName, ex.message))
            } catch (ex: IOException) {
                Log.e(TAG, String.format(
                    "I/O exception when parsing metadata for '%s': %s",
                    appinfo.packageName, ex.message))
            }
            xml.close()
        }

        /**
         * Create a list of all possible themes installed on the device for a
         * specific attributeset.
         *
         * @param context
         * @param attributeset
         * @return
         */
        @JvmStatic
        fun getThemeInfos(context: Context, attributeset: String): List<ThemeInfo> {
            val pm: PackageManager = context.packageManager
            val thisPackageName: String = context.packageName
            val appinfolist: List<ApplicationInfo> = getThemePackages(pm, thisPackageName)
            val themeinfolist: MutableList<ThemeInfo> = LinkedList()
            for (ai in appinfolist) {
                addThemeInfos(pm, attributeset, ai, themeinfolist)
            }
            return themeinfolist
        }

        @JvmStatic
        fun getPackageNameFromStyle(style: String): String? {
            val pos = style.indexOf(':')
            return if (pos >= 0) {
                style.substring(0, pos)
            } else {
                null
            }
        }
    }
}
