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
package org.apache.commons.vfs.provider.tar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * The TarBuffer class implements the tar archive concept of a buffered input
 * stream. This concept goes back to the days of blocked tape drives and special
 * io devices. In the Java universe, the only real function that this class
 * performs is to ensure that files have the correct "block" size, or other tars
 * will complain. <p>
 * <p/>
 * You should never have a need to access this class directly. TarBuffers are
 * created by Tar IO Streams.
 *
 * @author <a href="mailto:time@ice.com">Timothy Gerard Endres</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision: 764356 $ $Date: 2009-04-12 23:06:01 -0500 (Sun, 12 Apr 2009) $
 */
class TarBuffer
{
    public static final int DEFAULT_RECORDSIZE = (512);
    public static final int DEFAULT_BLOCKSIZE = (DEFAULT_RECORDSIZE * 20);

    private byte[] blockBuffer;
    private int blockSize;
    private int currBlkIdx;
    private int currRecIdx;
    private boolean debug;

    private InputStream input;
    private OutputStream output;
    private int recordSize;
    private int recsPerBlock;

    TarBuffer(final InputStream input)
    {
        this(input, TarBuffer.DEFAULT_BLOCKSIZE);
    }

    TarBuffer(final InputStream input, final int blockSize)
    {
        this(input, blockSize, TarBuffer.DEFAULT_RECORDSIZE);
    }

    TarBuffer(final InputStream input,
              final int blockSize,
              final int recordSize)
    {
        this.input = input;
        initialize(blockSize, recordSize);
    }

    TarBuffer(final OutputStream output)
    {
        this(output, TarBuffer.DEFAULT_BLOCKSIZE);
    }

    TarBuffer(final OutputStream output, final int blockSize)
    {
        this(output, blockSize, TarBuffer.DEFAULT_RECORDSIZE);
    }

    TarBuffer(final OutputStream output,
              final int blockSize,
              final int recordSize)
    {
        this.output = output;
        initialize(blockSize, recordSize);
    }

    /**
     * Set the debugging flag for the buffer.
     *
     * @param debug If true, print debugging output.
     */
    public void setDebug(final boolean debug)
    {
        this.debug = debug;
    }

    /**
     * Get the TAR Buffer's block size. Blocks consist of multiple records.
     *
     * @return The BlockSize value
     */
    public int getBlockSize()
    {
        return blockSize;
    }

    /**
     * Get the current block number, zero based.
     *
     * @return The current zero based block number.
     */
    public int getCurrentBlockNum()
    {
        return currBlkIdx;
    }

    /**
     * Get the current record number, within the current block, zero based.
     * Thus, current offset = (currentBlockNum * recsPerBlk) + currentRecNum.
     *
     * @return The current zero based record number.
     */
    public int getCurrentRecordNum()
    {
        return currRecIdx - 1;
    }

    /**
     * Get the TAR Buffer's record size.
     *
     * @return The RecordSize value
     */
    public int getRecordSize()
    {
        return recordSize;
    }

    /**
     * Determine if an archive record indicate End of Archive. End of archive is
     * indicated by a record that consists entirely of null bytes.
     *
     * @param record The record data to check.
     * @return The EOFRecord value
     */
    public boolean isEOFRecord(final byte[] record)
    {
        final int size = getRecordSize();
        for (int i = 0; i < size; ++i)
        {
            if (record[i] != 0)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Close the TarBuffer. If this is an output buffer, also flush the current
     * block before closing.
     */
    public void close()
            throws IOException
    {
        if (debug)
        {
            debug("TarBuffer.closeBuffer().");
        }

        if (null != output)
        {
            flushBlock();

            if (output != System.out && output != System.err)
            {
                output.close();
                output = null;
            }
        }
        else if (input != null)
        {
            if (input != System.in)
            {
                input.close();
                input = null;
            }
        }
    }

    /**
     * Read a record from the input stream and return the data.
     *
     * @return The record data.
     * @throws IOException Description of Exception
     */
    public byte[] readRecord()
            throws IOException
    {
        if (debug)
        {
            final String message = "ReadRecord: recIdx = " + currRecIdx +
                    " blkIdx = " + currBlkIdx;
            debug(message);
        }

        if (null == input)
        {
            final String message = "reading from an output buffer";
            throw new IOException(message);
        }

        if (currRecIdx >= recsPerBlock)
        {
            if (!readBlock())
            {
                return null;
            }
        }

        final byte[] result = new byte[recordSize];
        System.arraycopy(blockBuffer,
                (currRecIdx * recordSize),
                result,
                0,
                recordSize);

        currRecIdx++;

        return result;
    }

    /**
     * Skip over a record on the input stream.
     */
    public void skipRecord()
            throws IOException
    {
        if (debug)
        {
            final String message = "SkipRecord: recIdx = " + currRecIdx +
                    " blkIdx = " + currBlkIdx;
            debug(message);
        }

        if (null == input)
        {
            final String message = "reading (via skip) from an output buffer";
            throw new IOException(message);
        }

        if (currRecIdx >= recsPerBlock)
        {
            if (!readBlock())
            {
                return;// UNDONE
            }
        }

        currRecIdx++;
    }

    /**
     * Write an archive record to the archive.
     *
     * @param record The record data to write to the archive.
     */
    public void writeRecord(final byte[] record)
            throws IOException
    {
        if (debug)
        {
            final String message = "WriteRecord: recIdx = " + currRecIdx +
                    " blkIdx = " + currBlkIdx;
            debug(message);
        }

        if (null == output)
        {
            final String message = "writing to an input buffer";
            throw new IOException(message);
        }

        if (record.length != recordSize)
        {
            final String message = "record to write has length '" +
                    record.length + "' which is not the record size of '" +
                    recordSize + "'";
            throw new IOException(message);
        }

        if (currRecIdx >= recsPerBlock)
        {
            writeBlock();
        }

        System.arraycopy(record,
                0,
                blockBuffer,
                (currRecIdx * recordSize),
                recordSize);

        currRecIdx++;
    }

    /**
     * Write an archive record to the archive, where the record may be inside of
     * a larger array buffer. The buffer must be "offset plus record size" long.
     *
     * @param buffer The buffer containing the record data to write.
     * @param offset The offset of the record data within buf.
     */
    public void writeRecord(final byte[] buffer, final int offset)
            throws IOException
    {
        if (debug)
        {
            final String message = "WriteRecord: recIdx = " + currRecIdx +
                    " blkIdx = " + currBlkIdx;
            debug(message);
        }

        if (null == output)
        {
            final String message = "writing to an input buffer";
            throw new IOException(message);
        }

        if ((offset + recordSize) > buffer.length)
        {
            final String message = "record has length '" + buffer.length +
                    "' with offset '" + offset + "' which is less than the record size of '" +
                    recordSize + "'";
            throw new IOException(message);
        }

        if (currRecIdx >= recsPerBlock)
        {
            writeBlock();
        }

        System.arraycopy(buffer,
                offset,
                blockBuffer,
                (currRecIdx * recordSize),
                recordSize);

        currRecIdx++;
    }

    /**
     * Flush the current data block if it has any data in it.
     */
    private void flushBlock()
            throws IOException
    {
        if (debug)
        {
            final String message = "TarBuffer.flushBlock() called.";
            debug(message);
        }

        if (output == null)
        {
            final String message = "writing to an input buffer";
            throw new IOException(message);
        }

        if (currRecIdx > 0)
        {
            writeBlock();
        }
    }

    /**
     * Initialization common to all constructors.
     */
    private void initialize(final int blockSize, final int recordSize)
    {
        debug = false;
        this.blockSize = blockSize;
        this.recordSize = recordSize;
        recsPerBlock = (this.blockSize / this.recordSize);
        blockBuffer = new byte[this.blockSize];

        if (null != input)
        {
            currBlkIdx = -1;
            currRecIdx = recsPerBlock;
        }
        else
        {
            currBlkIdx = 0;
            currRecIdx = 0;
        }
    }

    /**
     * @return false if End-Of-File, else true
     */
    private boolean readBlock()
            throws IOException
    {
        if (debug)
        {
            final String message = "ReadBlock: blkIdx = " + currBlkIdx;
            debug(message);
        }

        if (null == input)
        {
            final String message = "reading from an output buffer";
            throw new IOException(message);
        }

        currRecIdx = 0;

        int offset = 0;
        int bytesNeeded = blockSize;

        while (bytesNeeded > 0)
        {
            final long numBytes = input.read(blockBuffer, offset, bytesNeeded);

            //
            // NOTE
            // We have fit EOF, and the block is not full!
            //
            // This is a broken archive. It does not follow the standard
            // blocking algorithm. However, because we are generous, and
            // it requires little effort, we will simply ignore the error
            // and continue as if the entire block were read. This does
            // not appear to break anything upstream. We used to return
            // false in this case.
            //
            // Thanks to 'Yohann.Roussel@alcatel.fr' for this fix.
            //
            if (numBytes == -1)
            {
                // However, just leaving the unread portion of the buffer dirty does
                // cause problems in some cases.  This problem is described in
                // http://issues.apache.org/bugzilla/show_bug.cgi?id=29877
                //
                // The solution is to fill the unused portion of the buffer with zeros.

                Arrays.fill(blockBuffer, offset, offset + bytesNeeded, (byte) 0);

                break;
            }

            offset += numBytes;
            bytesNeeded -= numBytes;

            if (numBytes != blockSize)
            {
                if (debug)
                {
                    System.err.println("ReadBlock: INCOMPLETE READ "
                            + numBytes + " of " + blockSize
                            + " bytes read.");
                }
            }
        }

        currBlkIdx++;

        return true;
    }

    /**
     * Write a TarBuffer block to the archive.
     *
     * @throws IOException Description of Exception
     */
    private void writeBlock()
            throws IOException
    {
        if (debug)
        {
            final String message = "WriteBlock: blkIdx = " + currBlkIdx;
            debug(message);
        }

        if (null == output)
        {
            final String message = "writing to an input buffer";
            throw new IOException(message);
        }

        output.write(blockBuffer, 0, blockSize);
        output.flush();

        currRecIdx = 0;
        currBlkIdx++;
    }

    protected void debug(final String message)
    {
        if (debug)
        {
            System.err.println(message);
        }
    }
}
