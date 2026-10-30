package com.ferg.awfulapp.service

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle

/**
 * 
 * Created by baka kaba on 19/03/2016.
 * 
 * 
 * Simple wrapper for a Cursor, to enforce the 'don't close this' requirement in
 * [ThreadCursorAdapter.getRow] and [AwfulCursorAdapter.getRow].
 * Calling [.close] throws an UnsupportedOperationException.
 */
class UncloseableCursor(private val mCursor: Cursor) : Cursor {
    override fun getCount(): Int {
        return mCursor.count
    }


    override fun getPosition(): Int {
        return mCursor.position
    }


    override fun move(offset: Int): Boolean {
        return mCursor.move(offset)
    }


    override fun moveToPosition(position: Int): Boolean {
        return mCursor.moveToPosition(position)
    }


    override fun moveToFirst(): Boolean {
        return mCursor.moveToFirst()
    }


    override fun moveToLast(): Boolean {
        return mCursor.moveToLast()
    }


    override fun moveToNext(): Boolean {
        return mCursor.moveToNext()
    }


    override fun moveToPrevious(): Boolean {
        return mCursor.moveToPrevious()
    }


    override fun isFirst(): Boolean {
        return mCursor.isFirst
    }


    override fun isLast(): Boolean {
        return mCursor.isLast
    }


    override fun isBeforeFirst(): Boolean {
        return mCursor.isBeforeFirst
    }


    override fun isAfterLast(): Boolean {
        return mCursor.isAfterLast
    }


    override fun getColumnIndex(columnName: String?): Int {
        return mCursor.getColumnIndex(columnName)
    }


    @Throws(IllegalArgumentException::class)
    override fun getColumnIndexOrThrow(columnName: String?): Int {
        return mCursor.getColumnIndexOrThrow(columnName)
    }


    override fun getColumnName(columnIndex: Int): String? {
        return mCursor.getColumnName(columnIndex)
    }


    override fun getColumnNames(): Array<String?>? {
        return mCursor.columnNames
    }


    override fun getColumnCount(): Int {
        return mCursor.columnCount
    }


    override fun getBlob(columnIndex: Int): ByteArray? {
        return mCursor.getBlob(columnIndex)
    }


    override fun getString(columnIndex: Int): String? {
        return mCursor.getString(columnIndex)
    }


    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer?) {
        mCursor.copyStringToBuffer(columnIndex, buffer)
    }


    override fun getShort(columnIndex: Int): Short {
        return mCursor.getShort(columnIndex)
    }


    override fun getInt(columnIndex: Int): Int {
        return mCursor.getInt(columnIndex)
    }


    override fun getLong(columnIndex: Int): Long {
        return mCursor.getLong(columnIndex)
    }


    override fun getFloat(columnIndex: Int): Float {
        return mCursor.getFloat(columnIndex)
    }


    override fun getDouble(columnIndex: Int): Double {
        return mCursor.getDouble(columnIndex)
    }


    override fun getType(columnIndex: Int): Int {
        return mCursor.getType(columnIndex)
    }


    override fun isNull(columnIndex: Int): Boolean {
        return mCursor.isNull(columnIndex)
    }


    @Deprecated("")
    override fun deactivate() {
        mCursor.deactivate()
    }


    @Deprecated("")
    override fun requery(): Boolean {
        return mCursor.requery()
    }


    override fun close() {
        throw UnsupportedOperationException("This cursor cannot be closed! Namaste")
    }


    override fun isClosed(): Boolean {
        return mCursor.isClosed
    }


    override fun registerContentObserver(observer: ContentObserver?) {
        mCursor.registerContentObserver(observer)
    }


    override fun unregisterContentObserver(observer: ContentObserver?) {
        mCursor.unregisterContentObserver(observer)
    }


    override fun registerDataSetObserver(observer: DataSetObserver?) {
        mCursor.registerDataSetObserver(observer)
    }


    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        mCursor.unregisterDataSetObserver(observer)
    }


    override fun setNotificationUri(cr: ContentResolver?, uri: Uri?) {
        mCursor.setNotificationUri(cr, uri)
    }


    override fun getNotificationUri(): Uri? {
        return mCursor.notificationUri
    }


    override fun getWantsAllOnMoveCalls(): Boolean {
        return mCursor.wantsAllOnMoveCalls
    }


    override fun setExtras(extras: Bundle?) {
        mCursor.extras = extras
    }


    override fun getExtras(): Bundle? {
        return mCursor.extras
    }


    override fun respond(extras: Bundle?): Bundle? {
        return mCursor.respond(extras)
    }
}
