package com.skedgo.tripkit.ui.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;

import com.skedgo.sqlite.DatabaseField;
import com.skedgo.sqlite.DatabaseTable;
import com.skedgo.tripkit.common.util.StringBuilderPool;

import androidx.annotation.Nullable;

/**
 * @author Daniel Grech
 */
public class ProviderUtils {
    private static final StringBuilderPool sSbPool = new StringBuilderPool();

    public static Uri doInsert(Context c, Uri providerUri, ContentValues values) {
        return c.getContentResolver().insert(providerUri, values);
    }

    /**
     * Insert multiple rows
     *
     * @author Tran Vu Tat Binh
     */
    public static int doBulkInsert(Context c, Uri providerUri,
                                   ContentValues[] valuesArray) {
        return c.getContentResolver().bulkInsert(providerUri, valuesArray);
    }

    public static boolean doUpdate(Context c, Uri providerUri,
                                   ContentValues values, String sel, String[] selArgs) {
        return c.getContentResolver().update(providerUri, values, sel, selArgs) > 0;
    }

    public static int doDelete(Context c, Uri providerUri, String where) {
        return c.getContentResolver().delete(providerUri, where, null);
    }

    public static long upsert(SQLiteDatabase db, DatabaseTable table,
                              ContentValues values, @Nullable DatabaseField fieldToMatch) {
        return upsert(db, table, values, new DatabaseField[]{fieldToMatch});
    }

    public static long upsert(SQLiteDatabase db, DatabaseTable table,
                              ContentValues values, @Nullable DatabaseField[] fieldsToMatch) {
        long newId = -1;
        if (values != null) {
            boolean containsAllFieldsToMatch = true;
            if (fieldsToMatch != null && fieldsToMatch.length > 0) {
                for (DatabaseField field : fieldsToMatch) {
                    if (!values.containsKey(field.getName())) {
                        containsAllFieldsToMatch = false;
                        break;
                    }
                }
            } else {
                containsAllFieldsToMatch = false;
            }

            if (containsAllFieldsToMatch) {
                final String[] tableFields = table.getFieldNames();

                final StringBuilder sb = sSbPool.retrieve();

                sb.append("INSERT OR REPLACE INTO ").append(table).append(" (")
                    .append(TextUtils.join(", ", tableFields)).append(")");
                sb.append(" VALUES ");
                sb.append("(");

                for (int i = 0, len = tableFields.length; i < len; i++) {
                    if (i != 0) {
                        sb.append(" ,");
                    }

                    if (values.containsKey(tableFields[i])) {
                        sb.append("?");
                    } else {
                        sb.append(" (SELECT ").append(tableFields[i])
                            .append(" FROM ").append(table)
                            .append(" WHERE ");

                        for (int j = 0, fieldLen = fieldsToMatch.length; j < fieldLen; j++) {
                            if (j != 0) {
                                sb.append(" AND ");
                            }

                            sb.append(fieldsToMatch[j]);
                            sb.append(" = ");

                            final String val = values
                                .getAsString(fieldsToMatch[j].getName());
                            if (val == null) {
                                sb.append(val);
                            } else {
                                sb.append(DatabaseUtils.sqlEscapeString(values
                                    .getAsString(fieldsToMatch[j].getName())));
                            }
                        }

                        sb.append(")");
                    }
                }

                sb.append(")");

                SQLiteStatement statement = db.compileStatement(sb.toString());

                int ac = 1;
                for (String field : tableFields) {
                    if (values.containsKey(field)) {
                        SqlUtils.bind(statement, ac++,
                            values.getAsString(field));
                    }
                }

                newId = statement.executeInsert();

                sSbPool.save(sb);
            } else {
                newId = db.replaceOrThrow(table.getName(), null, values);
            }
        }

        return newId;
    }

}