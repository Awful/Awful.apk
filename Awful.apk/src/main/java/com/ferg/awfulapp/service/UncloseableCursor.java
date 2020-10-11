package com.ferg.awfulapp.service;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;

/**
 * <p>Created by baka kaba on 19/03/2016.</p>
 *
 * <p>Simple wrapper for a Cursor, to enforce the 'don't close this' requirement in
 * {@link ThreadCursorAdapter#getRow(long)} and {@link AwfulCursorAdapter#getRow(long)}.
 * Calling {@link #close()} throws an UnsupportedOperationException.</p>
 */
public class UncloseableCursor implements Cursor {

    private final Cursor mCursor;


    public UncloseableCursor(@NonNull Cursor mCursor) {
        this.mCursor = mCursor;
    }


    @Override
    public int getCount() {
        return mCursor.getCount();
    }


    @Override
    public int getPosition() {
        return mCursor.getPosition();
    }


    @Override
    public boolean move(int offset) {
        return mCursor.move(offset);
    }


    @Override
    public boolean moveToPosition(int position) {
        return mCursor.moveToPosition(position);
    }


    @Override
    public boolean moveToFirst() {
        return mCursor.moveToFirst();
    }


    @Override
    public boolean moveToLast() {
        return mCursor.moveToLast();
    }


    @Override
    public boolean moveToNext() {
        return mCursor.moveToNext();
    }


    @Override
    public boolean moveToPrevious() {
        return mCursor.moveToPrevious();
    }


    @Override
    public boolean isFirst() {
        return mCursor.isFirst();
    }


    @Override
    public boolean isLast() {
        return mCursor.isLast();
    }


    @Override
    public boolean isBeforeFirst() {
        return mCursor.isBeforeFirst();
    }


    @Override
    public boolean isAfterLast() {
        return mCursor.isAfterLast();
    }


    @Override
    public int getColumnIndex(String columnName) {
        return mCursor.getColumnIndex(columnName);
    }


    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        return mCursor.getColumnIndexOrThrow(columnName);
    }


    @Override
    public String getColumnName(int columnIndex) {
        return mCursor.getColumnName(columnIndex);
    }


    @Override
    public String[] getColumnNames() {
        return mCursor.getColumnNames();
    }


    @Override
    public int getColumnCount() {
        return mCursor.getColumnCount();
    }


    @Override
    public byte[] getBlob(int columnIndex) {
        return mCursor.getBlob(columnIndex);
    }


    @Override
    public String getString(int columnIndex) {
        return mCursor.getString(columnIndex);
    }


    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        mCursor.copyStringToBuffer(columnIndex, buffer);
    }


    @Override
    public short getShort(int columnIndex) {
        return mCursor.getShort(columnIndex);
    }


    @Override
    public int getInt(int columnIndex) {
        return mCursor.getInt(columnIndex);
    }


    @Override
    public long getLong(int columnIndex) {
        return mCursor.getLong(columnIndex);
    }


    @Override
    public float getFloat(int columnIndex) {
        return mCursor.getFloat(columnIndex);
    }


    @Override
    public double getDouble(int columnIndex) {
        return mCursor.getDouble(columnIndex);
    }


    @Override
    public int getType(int columnIndex) {
        return mCursor.getType(columnIndex);
    }


    @Override
    public boolean isNull(int columnIndex) {
        return mCursor.isNull(columnIndex);
    }


    @Override
    @Deprecated
    public void deactivate() {
        mCursor.deactivate();
    }


    @Override
    @Deprecated
    public boolean requery() {
        return mCursor.requery();
    }


    @Override
    public void close() {
        throw new UnsupportedOperationException("This cursor cannot be closed! Namaste");
    }


    @Override
    public boolean isClosed() {
        return mCursor.isClosed();
    }


    @Override
    public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }


    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }


    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mCursor.registerDataSetObserver(observer);
    }


    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mCursor.unregisterDataSetObserver(observer);
    }


    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        mCursor.setNotificationUri(cr, uri);
    }


    @Override
    public Uri getNotificationUri() {
        return mCursor.getNotificationUri();
    }


    @Override
    public boolean getWantsAllOnMoveCalls() {
        return mCursor.getWantsAllOnMoveCalls();
    }


    @Override
    public void setExtras(Bundle extras) {
        mCursor.setExtras(extras);
    }


    @Override
    public Bundle getExtras() {
        return mCursor.getExtras();
    }


    @Override
    public Bundle respond(Bundle extras) {
        return mCursor.respond(extras);
    }
}
