package com.github.tencent;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ServletOutputStreamCopier extends ServletOutputStream {

    private final ByteArrayOutputStream copy;

    public ServletOutputStreamCopier() {
        this.copy = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
        copy.write(b);
    }

    public byte[] getCopy() {
        return copy.toByteArray();
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new UnsupportedOperationException();
    }

}
