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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.List;

import de.msal.shoutemo.connector.model.Post;

/**
 * Provides access to a database.
 */
public class ChatProvider extends ContentProvider {

    private static final int URI_MATCH_MESSAGES = 10;
    private static final int URI_MATCH_MESSAGE_ID = 11;
    private static final int URI_MATCH_AUTHORS = 20;
    private static final int URI_MATCH_AUTHOR_ID = 21;
    private static final int URI_MATCH_POSTS = 30;
    private static final UriMatcher mUriMatcher;
    private DatabaseHelper mOpenHelper;

    /**
     * A block that instantiates and sets static objects
     */
    static {
      /* Create a new instance */
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(ChatDb.AUTHORITY, "messages", URI_MATCH_MESSAGES);
        mUriMatcher.addURI(ChatDb.AUTHORITY, "message/#", URI_MATCH_MESSAGE_ID);
        mUriMatcher.addURI(ChatDb.AUTHORITY, "authors", URI_MATCH_AUTHORS);
        mUriMatcher.addURI(ChatDb.AUTHORITY, "author/#", URI_MATCH_AUTHOR_ID);
        mUriMatcher.addURI(ChatDb.AUTHORITY, "posts", URI_MATCH_POSTS);
    }

    /*
     * =======================================================================
     * ContentProvider
     * =======================================================================
     */

    /**
     * Initializes the provider by creating a new DatabaseHelper. onCreate() is called automatically
     * when Android creates the provider in response to a resolver request from a client.
     */
    @Override
    public boolean onCreate() {
        /*
         * Creates a new helper object. Note that the database itself isn't
         * opened until something tries to access it, and it's only created if
         * it doesn't already exist.
         */
        mOpenHelper = new DatabaseHelper(getContext());

        /* Assumes that any failures will be reported by a thrown exception. */
        return true;
    }

    /**
     * This method is called when a client calls {@link android.content.ContentResolver#query(android.net.Uri,
     * String[], String, String[], String)}. Queries the database and returns a cursor containing
     * the results.
     *
     * @return A cursor containing the results of the query. The cursor exists but is empty if the
     * query returns no results or an exception occurs.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        String groupBy = null;

        SQLiteDatabase db = mOpenHelper
                .getReadableDatabase(); // READ, since no writes need to be done
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder(); // construct a new query builder

        if (sortOrder == null) {
            sortOrder = ChatDb.Messages.COLUMN_NAME_TIMESTAMP + " ASC";
        }

        switch (mUriMatcher.match(uri)) {
            case URI_MATCH_MESSAGES:
                builder.setTables(ChatDb.Messages.TABLE_NAME);
                break;
            case URI_MATCH_POSTS:
                builder.setTables(ChatDb.Messages.TABLE_NAME
                        + " LEFT OUTER JOIN "
                        + ChatDb.Authors.TABLE_NAME
                        + " ON ("
                        + ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME
                        + " = "
                        + ChatDb.Authors.COLUMN_NAME_NAME
                        + ")");
                break;
            case URI_MATCH_AUTHORS:
                builder.setTables(ChatDb.Authors.TABLE_NAME);
                break;
            case URI_MATCH_MESSAGE_ID: // select a single entry
                builder.setTables(ChatDb.Messages.TABLE_NAME);
                builder.appendWhere(ChatDb.Messages._ID + "="
                        + uri.getPathSegments().get(ChatDb.Messages.MESSAGE_ID_PATH_POSITION));
                break;
            case URI_MATCH_AUTHOR_ID: // select a single entry
                builder.setTables(ChatDb.Authors.TABLE_NAME);
                builder.appendWhere(ChatDb.Messages._ID + "="
                        + uri.getPathSegments().get(ChatDb.Authors.AUTHOR_ID_PATH_POSITION));
                break;
            default: // If the URI doesn't match any of the known patterns, throw an  exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor c = builder.query(db, // The database to query
                projection, // The columns to return from the query
                selection, // The columns for the where clause
                selectionArgs, // The values for the where clause
                groupBy,
                null, // don't filter by row groups
                sortOrder // The sort order
        );

        /* Tells the Cursor what URI to watch, so it knows when its source data changes */
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /**
     * This is called when a client calls {@link android.content.ContentResolver#getType(android.net.Uri)}.
     * Returns the MIME data type of the URI given as a parameter.
     *
     * @param uri The URI whose MIME type is desired.
     * @return The MIME type of the URI.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public String getType(Uri uri) {
        /**
         * Chooses the MIME type based on the incoming URI pattern
         */
        switch (mUriMatcher.match(uri)) {
         /* general content type. */
            case URI_MATCH_MESSAGES:
                return ChatDb.Messages.CONTENT_TYPE;
            case URI_MATCH_AUTHORS:
                return ChatDb.Authors.CONTENT_TYPE;
         /* ID content type. */
            case URI_MATCH_MESSAGE_ID:
                return ChatDb.Messages.CONTENT_ITEM_TYPE;
            case URI_MATCH_AUTHOR_ID:
                return ChatDb.Authors.CONTENT_ITEM_TYPE;
         /* If the URI pattern doesn't match any permitted patterns, throws an exception. */
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * This is called when a client calls {@link android.content.ContentResolver#insert(android.net.Uri,
     * android.content.ContentValues)}. Inserts a new row into the database. This method sets up
     * default values for any columns that are not included in the incoming map. If rows were
     * inserted, then listeners are notified of the change.
     *
     * @return The row ID of the inserted row.
     * @throws android.database.SQLException if the insertion fails.
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        ContentValues values = (initialValues == null) ? new ContentValues()
                : new ContentValues(initialValues);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase(); // open in WRITE mode
        long rowId = 0;
      /* validates the incoming URI. Only the full provider URI is allowed for inserts */
        switch (mUriMatcher.match(uri)) {
            case URI_MATCH_MESSAGES:
            /* performs the insert and returns the ID of the new entry */
                rowId = db.insertWithOnConflict(ChatDb.Messages.TABLE_NAME, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
                break;
            case URI_MATCH_AUTHORS:
                rowId = db.insertWithOnConflict(ChatDb.Authors.TABLE_NAME, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
                break;
         /* If the URI pattern doesn't match any permitted patterns, throws an exception. */
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

//        if (rowId > 0) { // if the insert succeeded, the row ID exists
         /* notifies observers registered against this provider that the data changed */
        getContext().getContentResolver().notifyChange(uri, null);
        getContext().getContentResolver().notifyChange(ChatDb.Posts.CONTENT_URI, null);
        return uri;
//        }
//        else { // if the insert didn't succeed, then the rowID is <= 0: throws an exception
//            throw new SQLException("Failed to insert row into " + uri);
//        }
    }

    public void insert(List<Post> posts) {

        for (Post post : posts) {
            ContentValues values = new ContentValues();

            values.put(ChatDb.Authors.COLUMN_NAME_NAME, post.getAuthor().getName());
            values.put(ChatDb.Authors.COLUMN_NAME_TYPE, post.getAuthor().getType().name());
            insert(ChatDb.Authors.CONTENT_URI, values);

            values = new ContentValues();
            values.put(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME, post.getAuthor().getName());
            values.put(ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML, post.getMessage().getHtml());
            values.put(ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT, post.getMessage().getText());
            values.put(ChatDb.Messages.COLUMN_NAME_TYPE, post.getMessage().getType().name());
            values.put(ChatDb.Messages.COLUMN_NAME_TIMESTAMP, post.getDate().getTime());
            insert(ChatDb.Messages.CONTENT_URI, values);
        }
    }

    /**
     * This is called when a client calls {@link android.content.ContentResolver#delete(android.net.Uri,
     * String, String[])}. Deletes records from the database. If the incoming URI matches the note
     * ID URI pattern, this method deletes the one record specified by the ID in the URI. Otherwise,
     * it deletes a a set of records. The record or records must also match the input selection
     * criteria specified by where and whereArgs. <p/> If rows were deleted, then listeners are
     * notified of the change.
     *
     * @return If a "where" clause is used, the number of rows affected is returned, otherwise 0 is
     * returned. To delete all rows and get a row count, use "1" as the where clause.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper
                .getWritableDatabase(); // Opens the database object in "write" mode.
        String finalWhere;

        int count;

      /* Does the delete based on the incoming URI pattern. */
        switch (mUriMatcher.match(uri)) {
         /*
          * If the incoming pattern matches the general pattern for message, does a
          * delete based on the incoming "where" columns and arguments.
          */
            case URI_MATCH_MESSAGES:
                count = db.delete(ChatDb.Messages.TABLE_NAME, // The database table name
                        where, // The incoming where clause column names
                        whereArgs // The incoming where clause values
                );
                break;
            case URI_MATCH_AUTHORS:
                count = db.delete(ChatDb.Authors.TABLE_NAME, // The database table name
                        where, // The incoming where clause column names
                        whereArgs // The incoming where clause values
                );
                break;
         /*
          * If the incoming URI matches a single note ID, does the delete based
          * on the incoming data, but modifies the where clause to restrict it to
          * the particular note ID.
          */
            case URI_MATCH_MESSAGE_ID:
             /* Starts a final WHERE clause by restricting it to the desired ID. */
                finalWhere = ChatDb.Messages._ID
                        + " = "
                        + uri.getPathSegments().get(ChatDb.Messages.MESSAGE_ID_PATH_POSITION);
             /*
              * If there were additional selection criteria, append them to the
              * final WHERE clause
              */
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }
             /* Performs the delete. */
                count = db.delete(ChatDb.Messages.TABLE_NAME, // The database table name.
                        finalWhere, // The final WHERE clause
                        whereArgs); // The incoming where clause values.
                break;
            case URI_MATCH_AUTHOR_ID:
             /* Starts a final WHERE clause by restricting it to the desired ID. */
                finalWhere = ChatDb.Messages._ID
                        + " = "
                        + uri.getPathSegments().get(ChatDb.Authors.AUTHOR_ID_PATH_POSITION);
             /*
              * If there were additional selection criteria, append them to the
              * final WHERE clause
              */
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }
             /* Performs the delete. */
                count = db.delete(ChatDb.Authors.TABLE_NAME, // The database table name.
                        finalWhere, // The final WHERE clause
                        whereArgs); // The incoming where clause values.
                break;
        /* If the incoming pattern is invalid, throws an exception. */
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*
         * Gets a handle to the content resolver object for the current context,
         * and notifies it that the incoming URI changed. The object passes this
         * along to the resolver framework, and observers that have registered
         * themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows deleted.
        return count;
    }

    /**
     * This is called when a client calls {@link android.content.ContentResolver#update(android.net.Uri,
     * android.content.ContentValues, String, String[])} Updates records in the database. The column
     * names specified by the keys in the values map are updated with new data specified by the
     * values in the map. If the incoming URI matches the note ID URI pattern, then the method
     * updates the one record specified by the ID in the URI; otherwise, it updates a set of
     * records. The record or records must match the input selection criteria specified by where and
     * whereArgs. If rows were updated, then listeners are notified of the change.
     *
     * @param uri       The URI pattern to match and update.
     * @param values    A map of column names (keys) and new values (values).
     * @param where     An SQL "WHERE" clause that selects records based on their column values. If
     *                  this is null, then all records that match the URI pattern are selected.
     * @param whereArgs An array of selection criteria. If the "where" param contains value
     *                  placeholders ("?"), then each placeholder is replaced by the corresponding
     *                  element in the array.
     * @return The number of rows updated.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere, entryId;

        switch (mUriMatcher.match(uri)) {
            case URI_MATCH_MESSAGES:
                count = db.update(ChatDb.Messages.TABLE_NAME,
                        values,
                        where,
                        whereArgs);
                break;
            case URI_MATCH_MESSAGE_ID:
                entryId = uri.getPathSegments().get(ChatDb.Messages.MESSAGE_ID_PATH_POSITION);

                finalWhere = ChatDb.Messages._ID + " = " + entryId;
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.update(ChatDb.Messages.TABLE_NAME,
                        values,
                        finalWhere,
                        whereArgs);
                break;
            case URI_MATCH_AUTHORS:
                count = db.update(ChatDb.Authors.TABLE_NAME,
                        values,
                        where,
                        whereArgs);
                break;
            case URI_MATCH_AUTHOR_ID:
                entryId = uri.getPathSegments().get(ChatDb.Authors.AUTHOR_ID_PATH_POSITION);

                finalWhere = ChatDb.Authors._ID + " = " + entryId;
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.update(ChatDb.Authors.TABLE_NAME,
                        values,
                        finalWhere,
                        whereArgs);
                break;
         /* If the incoming pattern is invalid, throws an exception. */
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*
         * Gets a handle to the content resolver object for the current context,
         * and notifies it that the incoming URI changed. The object passes this
         * along to the resolver framework, and observers that have registered
         * themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        /* Returns the number of rows updated. */
        return count;
    }

}
