package com.skedgo.tripkit.ui.utils;

import android.database.sqlite.SQLiteStatement;

class SqlUtils {
    public static void bind(SQLiteStatement statement, int bindArg, String value) {
        if (value == null) {
            statement.bindNull(bindArg);
        } else {
            statement.bindString(bindArg, value);
        }
    }
}