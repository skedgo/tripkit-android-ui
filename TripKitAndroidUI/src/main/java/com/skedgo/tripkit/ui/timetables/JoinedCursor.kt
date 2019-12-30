package com.skedgo.tripkit.ui.timetables
import android.annotation.TargetApi
import android.content.ContentResolver
import android.database.AbstractCursor
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Build

class JoinedCursor constructor(val leftCursor: Cursor,
                               val rightCursor: Cursor,
                               val leftJoinColumn: String,
                               val rightJoinColumn: String) : AbstractCursor() {

  val mapIdToRightCursorPosition: Map<Long, Int> by lazy {
    (0 until rightCursor.count)
        .map {
          rightCursor.moveToPosition(it)
          rightCursor.getLong(rightCursor.getColumnIndex(rightJoinColumn)) to it
        }
        .toMap()
  }
  private val joinedColumnNames by lazy { leftCursor.columnNames + rightCursor.columnNames }

  override fun getCount(): Int = leftCursor.count

  override fun getColumnNames(): Array<String> = joinedColumnNames

  override fun onMove(oldPosition: Int, newPosition: Int): Boolean =
      leftCursor.moveToPosition(newPosition)

  override fun getInt(column: Int): Int {
    if (column < leftCursor.columnCount) {
      return leftCursor.getInt(column)
    }

    val leftRowId = leftCursor.getLong(leftCursor.getColumnIndex(leftJoinColumn))
    if (leftRowId in mapIdToRightCursorPosition) {
      rightCursor.moveToPosition(mapIdToRightCursorPosition[leftRowId]!!)
      val rightCursorIndex = column - leftCursor.columnCount
      return rightCursor.getInt(rightCursorIndex)
    }
    return -1
  }

  override fun getLong(column: Int): Long {
    if (column < leftCursor.columnCount) {
      return leftCursor.getLong(column)
    }
    throw NotImplementedError()
  }

  override fun getShort(column: Int): Short {
    if (column < leftCursor.columnCount) {
      return leftCursor.getShort(column)
    }

    throw NotImplementedError()
  }

  override fun getFloat(column: Int): Float {
    if (column < leftCursor.columnCount) {
      return leftCursor.getFloat(column)
    }

    throw NotImplementedError()
  }

  override fun getDouble(column: Int): Double {
    if (column < leftCursor.columnCount) {
      return leftCursor.getDouble(column)
    }

    throw NotImplementedError()
  }

  override fun isNull(column: Int): Boolean {
    if (column < leftCursor.columnCount) {
      return leftCursor.isNull(column)
    }

    val leftRowId = leftCursor.getLong(leftCursor.getColumnIndex(leftJoinColumn))
    if (leftRowId in mapIdToRightCursorPosition) {
      rightCursor.moveToPosition(mapIdToRightCursorPosition[leftRowId]!!)
      val rightCursorIndex = column - leftCursor.columnCount
      return rightCursor.isNull(rightCursorIndex)
    }
    return true
  }


  override fun getString(column: Int): String? {
    if (column < leftCursor.columnCount) {
      return leftCursor.getString(column)
    }
    throw NotImplementedError()
  }


  override fun setNotificationUri(cr: ContentResolver?, notifyUri: Uri?) {
    leftCursor.setNotificationUri(cr, notifyUri)
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  override fun getNotificationUri(): Uri {
    return leftCursor.notificationUri
  }

  override fun getColumnCount(): Int = leftCursor.columnCount + rightCursor.columnCount

  override fun getType(column: Int): Int {
    if (column < leftCursor.columnCount) {
      return leftCursor.getType(column)
    }

    val leftRowId = leftCursor.getLong(leftCursor.getColumnIndex(leftJoinColumn))
    if (leftRowId in mapIdToRightCursorPosition) {
      rightCursor.moveToPosition(mapIdToRightCursorPosition[leftRowId]!!)
      val rightCursorIndex = column - leftCursor.columnCount
      return rightCursor.getType(rightCursorIndex)
    }
    return Cursor.FIELD_TYPE_INTEGER
  }

  override fun registerDataSetObserver(observer: DataSetObserver?) {
    leftCursor.registerDataSetObserver(observer)
  }

  override fun registerContentObserver(observer: ContentObserver) {
    leftCursor.registerContentObserver(observer)
  }

  override fun unregisterContentObserver(observer: ContentObserver?) {
    leftCursor.unregisterContentObserver(observer)
  }

  override fun unregisterDataSetObserver(observer: DataSetObserver?) {
    leftCursor.unregisterDataSetObserver(observer)
  }

  override fun close() {
    leftCursor.close()
    rightCursor.close()
    super.close()
  }
}