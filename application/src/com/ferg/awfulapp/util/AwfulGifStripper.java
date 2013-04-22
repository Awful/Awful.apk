package com.ferg.awfulapp.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Author: Matthew Shepard
 * Date: 4/13/13 - 1:56 PM
 */
public class AwfulGifStripper extends FilterInputStream {
    private String TAG = "AwfulGif-";
    private static final byte[] animFlag = {(byte) 0x21, (byte) 0xF9,(byte) 0x04};

    public AwfulGifStripper(InputStream wrapped, String name){
        super(wrapped);
        TAG = TAG + name;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        try{
            int len = super.read(buffer, offset, length);
            scanReplace(buffer, offset, len);
            return len;
        }catch(IOException e){
            e.printStackTrace();
        }
        return -1;
    }

    private void scanReplace(byte[] buffer, int offset, int length) throws IOException {
        for(int ix = offset; ix < length-4; ix++){
            if(buffer[ix] == animFlag[0] && buffer[ix+1] == animFlag[1] && buffer[ix+2] == animFlag[2]){
                buffer[ix+4] = (byte) 0xFF;
                buffer[ix+5] = (byte) 0x70;
            }
        }
    }
}
