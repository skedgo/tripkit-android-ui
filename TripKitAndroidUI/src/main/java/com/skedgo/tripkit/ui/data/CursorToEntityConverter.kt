package com.skedgo.tripkit.ui.data

import android.database.Cursor
import io.reactivex.functions.Function

interface CursorToEntityConverter<E> : Function<Cursor, E>