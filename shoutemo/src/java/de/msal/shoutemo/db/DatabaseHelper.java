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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class helps open, create, and upgrade the database file. Set to package visibility for
 * testing purposes.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /* Used for debugging and logging */
    private static final String TAG = "Shoutemo|DatabaseHelper";
    /**
     * The database that the provider uses as its underlying data store
     */
    private static final String DATABASE_NAME = "chat.db";
    /**
     * The database version
     */
    private static final int DATABASE_VERSION = 2;

    DatabaseHelper(Context context) {
        /* calls the super constructor, requesting the default cursor factory. */
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ChatDb.Authors.TABLE_NAME + " ("
                + ChatDb.Authors._ID + " INTEGER PRIMARY KEY,"
                + ChatDb.Authors.COLUMN_NAME_NAME + " STRING UNIQUE NOT NULL,"
                + ChatDb.Authors.COLUMN_NAME_TYPE + " STRING NOT NULL"
                + ");");

        db.execSQL("CREATE TABLE " + ChatDb.Messages.TABLE_NAME + " ("
                + ChatDb.Messages._ID + " INTEGER PRIMARY KEY,"
                + ChatDb.Messages.COLUMN_NAME_TIMESTAMP + " INTEGER NOT NULL,"
                + ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML + " STRING,"
                + ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT + " STRING,"
                + ChatDb.Messages.COLUMN_NAME_TYPE + " STRING NOT NULL,"
                + ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME + " STRING,"
                + "UNIQUE("
                + ChatDb.Messages.COLUMN_NAME_TIMESTAMP + ","
                + ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML
                + "),"
                + "FOREIGN KEY (" + ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME + ") REFERENCES "
                + ChatDb.Authors.TABLE_NAME + "(" + ChatDb.Authors.COLUMN_NAME_NAME + ")"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /* Logs that the database is being upgraded */
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (newVersion == 2) {
            String sql = "UPDATE " + ChatDb.Messages.TABLE_NAME
                    + " SET "
                    + ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML
                    + " = replace(" + ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML + ","
                    + " 'src=\"http://www.autemo.com/images/smileys/',"
                    + " 'src=\"images/smileys/')"
                    + ";";
            db.execSQL(sql);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON;"); // API >16: .setForeignKeyConstraintsEnabled(true);
    }

}
