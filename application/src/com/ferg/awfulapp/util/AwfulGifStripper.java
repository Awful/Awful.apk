package com.ferg.awfulapp.util;

import android.util.Log;
import com.ferg.awfulapp.constants.Constants;

import java.io.IOException;
import java.io.InputStream;

/**
 * Author: Matthew Shepard
 * Date: 4/13/13 - 1:56 PM
 */
public class AwfulGifStripper extends InputStream {
    private static final boolean DEBUG = Constants.DEBUG;
    private String TAG = "AwfulGif-";

    private InputStream data;

    private enum STATE{OPEN, CONFIRMED, IMAGE_BLOCK, DONE, CLOSED, CANCELLED}

    private STATE fileState;

    private int BUFFER_SIZE = 512;
    private int[] peekBuffer = new int[BUFFER_SIZE];
    private int peekStart = 0;
    private int peekEnd = 0;


    public AwfulGifStripper() {
        super();
        if(DEBUG) Log.e(TAG, "EMPTY CONSTRUCTOR");
        fileState = STATE.CLOSED;
    }

    public AwfulGifStripper(InputStream wrapped, String name){
        data = wrapped;
        fileState = STATE.OPEN;
        TAG = TAG + name;
    }

    @Override
    public int read() throws IOException {
        if(valid()){
            int cur = readVal();
            switch(fileState){
                case CLOSED:
                    return -1;
                case CANCELLED:
                    return cur;
                case OPEN:
                    if(cur != 'G' && peek() != 'I' && peek() != 'F'){
                        if(DEBUG) Log.e(TAG, "NOT A GIF, SKIPPING");
                        fileState = STATE.CANCELLED;
                        return cur;
                    }else{
                        if(DEBUG) Log.e(TAG, "GIF CONFIRMED");
                        fileState = STATE.CONFIRMED;
                    }
                    break;
                case CONFIRMED:
                    if(cur == 0x21 && peek(0) == 0xF9 && peek(1) == 0x04){
                        if(DEBUG) Log.e(TAG, "ANIMATION BLOCK");
                        int delay = (peek(4) << 8) | peek(3);
                        if(DEBUG) Log.e(TAG, "DELAY: "+delay);
                        tweakPeek(3, 0xFF);
                        tweakPeek(4, 0x20);
                        int pdelay = (peek(4) << 8) | peek(3);
                        if(DEBUG) Log.e(TAG, "POSTDELAY: " + pdelay);
                        return cur;
                    }
                    break;
                case DONE:
                    fileState = STATE.CLOSED;
                    return 0x3B;
            }
            return cur;
        }
        return -1;
    }

    @Override
    public int available() throws IOException {
        if(valid()){
            return data.available();
        }
        return super.available();
    }

    @Override
    public void close() throws IOException {
        if(DEBUG) Log.e(TAG, "close");
        if(data != null){
            try{
                data.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        fileState = STATE.CLOSED;
        super.close();
    }

    @Override
    public void mark(int readlimit) {
        if(DEBUG) Log.e(TAG, "mark");
        super.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        if(DEBUG) Log.e(TAG, "markSupported");
        return false;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return super.read(buffer);
    }

    @Override
    public synchronized void reset() throws IOException {
        if(DEBUG) Log.e(TAG, "RESET");
        super.reset();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return super.read(buffer, offset, length);
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return super.skip(byteCount);
    }

    private boolean valid(){
        return data != null && fileState != STATE.CLOSED;
    }

    private int readVal() throws IOException{
        if(peekStart < peekEnd){
            return peekBuffer[peekStart++];
        }
        if(valid()){
            return data.read();
        }
        return -1;
    }

    private int peek(int pos) throws IOException{
        while(peekEnd - peekStart < pos+1){
            if(peek() < 0){
                return -1;
            }
        }
        return peekBuffer[peekStart+pos];
    }

    private int peek() throws IOException{
        if(peekStart == peekEnd){
            peekStart = peekEnd = 0;
        }
        if(valid()){
            peekBuffer[peekEnd++] = data.read();
            return peekBuffer[peekEnd-1];
        }
        return -1;
    }

    private void clearPeek(){
        peekStart = peekEnd = 0;
    }

    private void tweakPeek(int pos, int num){
        peekBuffer[peekStart+pos] = num;
    }
}
