package com.skedgo.tripkit.ui.data;

import android.database.Cursor;

import io.reactivex.functions.Function;

public interface CursorToEntityConverter<E> extends Function<Cursor, E> {
}