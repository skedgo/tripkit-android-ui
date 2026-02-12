package com.skedgo.tripkit.ui.utils

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.net.Uri
import android.text.TextUtils
import com.skedgo.sqlite.DatabaseField
import com.skedgo.sqlite.DatabaseTable
import com.skedgo.tripkit.common.util.StringBuilderPool

/**
 * @author Daniel Grech
 */
object ProviderUtils {
    private val sSbPool = StringBuilderPool()

    fun doInsert(context: Context, providerUri: Uri, values: ContentValues): Uri? {
        return context.contentResolver.insert(providerUri, values)
    }

    fun doBulkInsert(context: Context, providerUri: Uri, valuesArray: Array<ContentValues>): Int {
        return context.contentResolver.bulkInsert(providerUri, valuesArray)
    }

    fun doUpdate(context: Context, providerUri: Uri, values: ContentValues, sel: String?, selArgs: Array<String>?): Boolean {
        return context.contentResolver.update(providerUri, values, sel, selArgs) > 0
    }

    fun doDelete(context: Context, providerUri: Uri, where: String?): Int {
        return context.contentResolver.delete(providerUri, where, null)
    }

    fun upsert(db: SQLiteDatabase, table: DatabaseTable, values: ContentValues, fieldToMatch: DatabaseField?): Long {
        return upsert(db, table, values, fieldToMatch?.let { arrayOf(it) })
    }

    fun upsert(db: SQLiteDatabase, table: DatabaseTable?, values: ContentValues?, fieldsToMatch: Array<DatabaseField?>?): Long {
        var newId: Long = -1
        if (values != null && table != null) {
            val containsAllFieldsToMatch = fieldsToMatch?.all { values.containsKey(it?.name) } ?: false

            if (containsAllFieldsToMatch) {
                val tableFields = table.getFieldNames()
                val sb = sSbPool.retrieve()

                sb.append("INSERT OR REPLACE INTO ").append(table).append(" (")
                    .append(TextUtils.join(", ", tableFields)).append(")")
                    .append(" VALUES (")

                tableFields.forEachIndexed { index, field ->
                    if (index != 0) sb.append(" ,")
                    if (values.containsKey(field)) {
                        sb.append("?")
                    } else {
                        sb.append(" (SELECT ").append(field).append(" FROM ").append(table).append(" WHERE ")
                        fieldsToMatch!!.forEachIndexed { j, matchField ->
                            if (j != 0) sb.append(" AND ")
                            sb.append(matchField).append(" = ")
                            val valString = values.getAsString(matchField?.name)
                            sb.append(if (valString == null) valString else DatabaseUtils.sqlEscapeString(valString))
                        }
                        sb.append(")")
                    }
                }
                sb.append(")")

                val statement: SQLiteStatement = db.compileStatement(sb.toString())
                var ac = 1
                tableFields.forEach { field ->
                    if (values.containsKey(field)) {
                        SqlUtils.bind(statement, ac++, values.getAsString(field))
                    }
                }
                newId = statement.executeInsert()
                sSbPool.save(sb)
            } else {
                newId = db.replaceOrThrow(table.name, null, values)
            }
        }
        return newId
    }
}