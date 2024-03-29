/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs.provider.ram;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.vfs.FileSystemException;

/**
 * OutputStream to a RamFile
 */
public class RamFileOutputStream extends OutputStream
{

    /**
     * File
     */
    protected RamFileObject file;

    /**
     * buffer
     */
    protected byte buffer1[] = new byte[1];

    protected boolean closed = false;

    private IOException exc;

    /**
     * @param file
     */
    public RamFileOutputStream(RamFileObject file)
    {
        super();
        this.file = file;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.DataOutput#write(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException
    {
        int size = this.file.getData().size();
        int newSize = this.file.getData().size() + len;
        // Store the Exception in order to notify the client again on close()
        try
        {
            this.file.resize(newSize);
        }
        catch (IOException e)
        {
            this.exc = e;
            throw e;
        }
        System.arraycopy(b, off, this.file.getData().getBuffer(), size, len);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.DataOutput#write(int)
     */
    public void write(int b) throws IOException
    {
        buffer1[0] = (byte) b;
        this.write(buffer1);
    }

    public void flush() throws IOException
    {
    }

    public void close() throws IOException
    {
        if (closed)
        {
            return;
        }
        // Notify on close that there was an IOException while writing
        if (exc != null)
        {
            throw exc;
        }
        try
        {
            this.closed = true;
            // Close the
            this.file.endOutput();
        }
        catch (Exception e)
        {
            throw new FileSystemException(e);
        }
    }

}
