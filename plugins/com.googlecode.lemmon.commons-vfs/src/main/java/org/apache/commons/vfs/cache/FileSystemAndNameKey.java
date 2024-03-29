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
package org.apache.commons.vfs.cache;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystem;

/**
 * Key for various cache implementations.<br>
 * It compares the fileSystem (by hashCode) and the filename.
 *
 * @author <a href="mailto:imario@apache.org">Mario Ivankovits</a>
 * @version $Revision: 764356 $ $Date: 2009-04-12 23:06:01 -0500 (Sun, 12 Apr 2009) $
 */
class FileSystemAndNameKey implements Comparable
{
    /** The FileSystem */
    private final FileSystem fileSystem;

    /** The FileName */
    private final FileName fileName;

    /** hashcode to identify this object */
    private final int fileSystemId;

    FileSystemAndNameKey(final FileSystem fileSystem, final FileName fileName)
    {
        this.fileSystem = fileSystem;
        this.fileSystemId = System.identityHashCode(fileSystem);

        this.fileName = fileName;
    }

    public int compareTo(Object o)
    {
        FileSystemAndNameKey other = (FileSystemAndNameKey) o;

        if (fileSystemId < other.fileSystemId)
        {
            return -1;
        }
        if (fileSystemId > other.fileSystemId)
        {
            return 1;
        }

        return fileName.compareTo(other.fileName);
    }

    FileSystem getFileSystem()
    {
        return fileSystem;
    }

    FileName getFileName()
    {
        return fileName;
    }
}
