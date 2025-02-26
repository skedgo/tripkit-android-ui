package com.skedgo.tripkit.ui.utils

import android.database.sqlite.SQLiteStatement

object SqlUtils {
    fun bind(statement: SQLiteStatement, bindArg: Int, value: String?) {
        if (value == null) {
            statement.bindNull(bindArg)
        } else {
            statement.bindString(bindArg, value)
        }
    }
}