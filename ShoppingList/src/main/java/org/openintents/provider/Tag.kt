/*
 * Copyright (C) 2007-2008 OpenIntents.org
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

package org.openintents.provider

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log

/**
 * Definition for content provider related to tag.
 */
open class Tag(private val mContext: Context) {

    companion object {
        private const val TAG = "Tag.java"
        private const val DELETE_URI =
            "tag.content_id = (select content2._id FROM content content2 WHERE content2.uri = ?)"
        private const val DELETE_TAG_URI =
            "tag.tag_id = (select content1._id FROM content content1 WHERE content1.uri = ?) " +
                    "AND tag.content_id = (select content2._id FROM content content2 WHERE content2.uri = ?)"
    }

    fun removeTag(tag: String, uri: String) {
        mContext.contentResolver.delete(
            Tags.CONTENT_URI,
            DELETE_TAG_URI,
            arrayOf(tag, uri)
        )
    }

    fun removeAllTags(uri: String) {
        mContext.contentResolver.delete(
            Tags.CONTENT_URI,
            DELETE_URI,
            arrayOf(uri)
        )
    }

    fun insertTag(tag: String, content: String) {
        val values = ContentValues(2)
        values.put(Tags.URI_1, tag)
        values.put(Tags.URI_2, content)

        try {
            mContext.contentResolver.insert(Tags.CONTENT_URI, values)
        } catch (e: Exception) {
            Log.i(TAG, "insert failed", e)
        }
    }

    fun insertUniqueTag(tag: String, content: String) {
        val values = ContentValues(2)
        values.put(Tags.URI_1, tag)
        values.put(Tags.URI_2, content)

        try {
            val uri = Tags.CONTENT_URI.buildUpon()
                .appendQueryParameter(Tags.QUERY_UNIQUE_TAG, "true")
                .build()
            mContext.contentResolver.insert(uri, values)
        } catch (e: Exception) {
            Log.i(TAG, "insert failed", e)
        }
    }

    /**
     * cursor over contentUriStrings is returned where the content is tagged
     * with the given tag.
     *
     * @param tag
     * @param contentUri
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    @Deprecated("Cursor has to be closed by caller. Alternative API desired.")
    fun findTaggedContent(tag: String, contentUri: String): Cursor? {
        return mContext.contentResolver.query(
            Tags.CONTENT_URI,
            arrayOf(Tags._ID, Tags.URI_2),
            "content1.uri like ? and content2.uri like ?",
            arrayOf(tag, contentUri + "%"),
            "content2.uri"
        )
    }

    /**
     * cursor over tags with all tags for the given content is returned.
     *
     * @param tag
     * @param contentUri
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    @Deprecated("Cursor has to be closed by caller. Alternative API desired.")
    fun findTags(contentUri: String): Cursor? {
        return mContext.contentResolver.query(
            Tags.CONTENT_URI,
            arrayOf(Tags._ID, Tags.URI_1),
            "content2.uri = ?",
            arrayOf(contentUri),
            "content1.uri"
        )
    }

    fun findTags(uri: String, separator: String): String {
        val tags = findTags(uri)!!
        val sb = StringBuffer()
        val colIndex = tags.getColumnIndex(Tags.URI_1)
        while (tags.moveToNext()) {
            sb.append(tags.getString(colIndex))
            sb.append(separator)
        }
        if (sb.length > 0) {
            sb.deleteCharAt(sb.length - separator.length)
        }
        tags.close()
        return sb.toString()
    }

    /**
     * cursor over tags with all tags for the given content is returned.
     *
     * @param contentUriPrefix
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    @Deprecated("Cursor has to be closed by caller. Alternative API desired.")
    fun findTagsForContentType(contentUriPrefix: String): Cursor? {
        val uri = Contents.CONTENT_URI.buildUpon()
            .appendQueryParameter(Tags.DISTINCT, "true")
            .build()
        return mContext.contentResolver.query(
            uri,
            arrayOf(Contents._ID, Contents.URI),
            "exists(select * from content content2, tag tag where content2.uri like ? and content2._id = tag.content_id and content._id = tag.tag_id)",
            arrayOf(contentUriPrefix + "%"),
            "content.uri"
        )
    }

    /**
     * Get a cursor with all tags
     *
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    @Deprecated("Cursor has to be closed by caller. Alternative API desired.")
    fun findAllTags(): Cursor? {
        return mContext.contentResolver.query(
            Contents.CONTENT_URI,
            arrayOf(Contents._ID, Contents.URI, Contents.TYPE),
            "type like 'TAG%'",
            null,
            Contents.DEFAULT_SORT_ORDER
        )
    }

    /**
     * Get a cursor with all used tags, i.e. at least one content has been
     * tagged with this tag.
     *
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    @Deprecated("Cursor has to be closed by caller. Alternative API desired.")
    fun findAllUsedTags(): Cursor? {
        return mContext.contentResolver.query(
            Contents.CONTENT_URI,
            arrayOf(Contents._ID, Contents.URI, Contents.TYPE),
            "type like 'TAG%' and (select count(*) from tag where tag.tag_id = content._id) > 0",
            null,
            Contents.DEFAULT_SORT_ORDER
        )
    }

    /**
     * start add tag activity. Only useful, if tag or uri is null. Consider
     * using insertTag if you want to add the tag without user interaction.
     *
     * @param tag
     * @param uri
     */
    fun startAddTagActivity(tag: String?, uri: String?) {
        val intent = Intent(org.openintents.OpenIntents.TAG_ACTION, Tags.CONTENT_URI)
            .putExtra(Tags.QUERY_TAG, tag)
            .putExtra(Tags.QUERY_URI, uri)
        mContext.startActivity(intent)
    }

    class Tags : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            /**
             * The content:// style URL for this table
             */
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.tags/tags")

            /**
             * The default sort order for this table.
             */
            const val DEFAULT_SORT_ORDER = "modified DESC"

            /**
             * The id of the tag.
             *
             * Type: STRING
             */
            const val TAG_ID = "tag_id"

            /**
             * The id of the content.
             *
             * Type: STRING
             */
            const val CONTENT_ID = "content_id"

            /**
             * The timestamp for when the tag was created.
             *
             * Type: INTEGER (long)
             */
            const val CREATED_DATE = "created"

            /**
             * The timestamp for when the tag was last modified.
             *
             * Type: INTEGER (long)
             */
            const val MODIFIED_DATE = "modified"

            /**
             * The timestamp for when the tag was last modified.
             *
             * Type: INTEGER (long)
             */
            const val ACCESS_DATE = "accessed"

            /**
             * First URI of the relationship (usually the tag).
             */
            const val URI_1 = "uri_1"

            /**
             * Second URI of the relationship (usually the content).
             */
            const val URI_2 = "uri_2"

            /**
             * The Uri to be tagged that the query is about.
             */
            const val QUERY_URI = "uri"

            /**
             * The tag that the query is about.
             */
            const val QUERY_TAG = "tag"

            const val DISTINCT = "distinct"
            const val QUERY_UNIQUE_TAG = "unique"
        }
    }

    class Contents : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            /**
             * The content:// style URL for this table
             */
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.tags/contents")

            /**
             * The default sort order for this table.
             */
            const val DEFAULT_SORT_ORDER = "type DESC, uri"

            /**
             * The uri of the content, or the tag text if the uri starts with "TAG".
             * This can be tested in SQL using "WHERE type like 'TAG%'".
             *
             * Type: TEXT
             */
            const val URI = "uri"

            /**
             * The type of the content, e.g TAG null means CONTENT.
             *
             * Type: TEXT
             */
            const val TYPE = "type"

            /**
             * The timestamp for when the note was created.
             *
             * Type: INTEGER (long)
             */
            const val CREATED_DATE = "created"

            const val QUERY_BY_TYPE = "byType"
        }
    }
}
