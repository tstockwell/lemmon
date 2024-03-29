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
package org.apache.commons.vfs.provider.local;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.AbstractFileSystem;

import java.io.File;
import java.io.FilePermission;
import java.util.Collection;

/**
 * A local file system.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 480428 $ $Date: 2006-11-29 00:15:24 -0600 (Wed, 29 Nov 2006) $
 */
public class LocalFileSystem
    extends AbstractFileSystem
    implements FileSystem
{
    private final String rootFile;

    public LocalFileSystem(final FileName rootName,
                           final String rootFile,
                           final FileSystemOptions opts)
    {
        super(rootName, null, opts);
        this.rootFile = rootFile;
    }

    /**
     * Creates a file object.
     */
    protected FileObject createFile(final FileName name) throws FileSystemException
    {
        // Create the file
        return new LocalFile(this, rootFile, name);
    }

    /**
     * Returns the capabilities of this file system.
     */
    protected void addCapabilities(final Collection caps)
    {
        caps.addAll(DefaultLocalFileProvider.capabilities);
    }

    /**
     * Creates a temporary local copy of a file and its descendents.
     */
    protected File doReplicateFile(final FileObject fileObject,
                                   final FileSelector selector)
        throws Exception
    {
        final LocalFile localFile = (LocalFile) fileObject;
        final File file = localFile.getLocalFile();
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            final FilePermission requiredPerm = new FilePermission(file.getAbsolutePath(), "read");
            sm.checkPermission(requiredPerm);
        }
        return file;
    }

}
