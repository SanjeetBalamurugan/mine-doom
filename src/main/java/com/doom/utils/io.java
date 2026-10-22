package com.doom.utils;

import java.io.IOException;
import java.io.RandomAccessFile;

public class io {
    public static int readInt32LE(RandomAccessFile raf) throws IOException {
        int b1 = raf.read();
        int b2 = raf.read();
        int b3 = raf.read();
        int b4 = raf.read();
        if ((b1 | b2 | b3 | b4) < 0) {
            throw new IOException("Unexpected End of File reached while reading header.");
        }
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }
}
