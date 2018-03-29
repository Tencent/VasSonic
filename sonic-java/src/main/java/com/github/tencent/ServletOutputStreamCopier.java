package com.github.tencent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class ServletOutputStreamCopier extends ServletOutputStream {

    private ByteArrayOutputStream copy;

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
        return this.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        this.setWriteListener(writeListener);
    }

}
