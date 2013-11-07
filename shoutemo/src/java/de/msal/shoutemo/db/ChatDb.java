/*
 * Copyright 2013 Maximilian Salomon.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package de.msal.shoutemo.db;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the Messages content provider and its clients. A contract defines the
 * information that a client needs to access the provider as one or more data tables. A contract is
 * a public, non-extendable (final) class that contains constants defining column names and URIs. A
 * well-written client depends only on the constants in the contract.
 */
public final class ChatDb {

    public static final String AUTHORITY = "de.msal.provider.ChatDb";

    public static final String SCHEME = "content://";

    /* This class cannot be instantiated */
    private ChatDb() {
    }

    public static final class Posts implements BaseColumns {

        public static final String PATH_POSTS = "/posts";

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
                + PATH_POSTS);
    }

    /**
     * Messages table contract
     */
    public static final class Messages implements BaseColumns {

        public static final String TABLE_NAME = "messages";

        /*
         * URI definitions
         */
        public static final String PATH_MESSAGES = "/messages";

        public static final String PATH_MESSAGE_ID = "/message/";

        public static final int MESSAGE_ID_PATH_POSITION = 1;

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
                + PATH_MESSAGES);

        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME
                + AUTHORITY + PATH_MESSAGE_ID);

        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME
                + AUTHORITY + PATH_MESSAGE_ID + "/#");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
                + AUTHORITY + TABLE_NAME;

        /*
         * MIME type definitions
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
                + AUTHORITY + TABLE_NAME;

        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";

        /*
         * Column definitions
         */
        public static final String COLUMN_NAME_MESSAGE_TEXT = "message_text";

        public static final String COLUMN_NAME_MESSAGE_HTML = "message_html";

        public static final String COLUMN_NAME_TYPE = "message_type";

        public static final String COLUMN_NAME_AUTHOR_NAME = "message_author_name";

        /* This class cannot be instantiated */
        private Messages() {
        }

    }

    /**
     * Authors table contract
     */
    public static final class Authors implements BaseColumns {

        public static final String TABLE_NAME = "authors";

        /*
         * URI definitions
         */
        public static final String PATH_AUTHORS = "/authors";

        public static final String PATH_AUTHOR_ID = "/author/";

        public static final int AUTHOR_ID_PATH_POSITION = 1;

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
                + PATH_AUTHORS);

        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME
                + AUTHORITY + PATH_AUTHOR_ID);

        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME
                + AUTHORITY + PATH_AUTHOR_ID + "/#");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
                + AUTHORITY + TABLE_NAME;

        /*
         * MIME type definitions
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
                + AUTHORITY + TABLE_NAME;

        public static final String COLUMN_NAME_NAME = "author_name";

        public static final String COLUMN_NAME_TYPE = "author_type";

        /* This class cannot be instantiated */
        private Authors() {
        }

    }

}
