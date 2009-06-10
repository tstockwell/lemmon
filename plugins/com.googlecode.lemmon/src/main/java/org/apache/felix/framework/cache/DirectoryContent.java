/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.framework.cache;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.NameScope;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.moduleloader.IContent;

public class DirectoryContent implements IContent
{
    private static final int BUFSIZE = 4096;
    private static final transient String EMBEDDED_DIRECTORY = "-embedded";

    private Logger m_logger;
    private final Object m_revisionLock;
    private FileObject m_rootDir;
    private FileObject m_dir;

    public DirectoryContent(Logger logger, Object revisionLock, FileObject rootDir, FileObject dir)
    {
        m_logger = logger;
        m_revisionLock = revisionLock;
        m_rootDir = rootDir;
        m_dir = dir;
    }

    public void close()
    {
        // Nothing to clean up.
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        try
        {
            return m_dir.resolveFile( name, NameScope.CHILD).exists();
        }
        catch ( FileSystemException e )
        {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized Enumeration getEntries()
    {
        try
        {
            // Wrap entries enumeration to filter non-matching entries.
            Enumeration e = new EntriesEnumeration(m_dir);

            // Spec says to return null if there are no entries.
            return (e.hasMoreElements()) ? e : null;
        }
        catch ( FileSystemException e )
        {
            throw new RuntimeException(e);
        }
    }

    public synchronized byte[] getEntryAsBytes(String name) throws IllegalStateException
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        // Get the embedded resource.
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try
        {
            is = new BufferedInputStream(m_dir.resolveFile( name, NameScope.CHILD).getContent().getInputStream());
            baos = new ByteArrayOutputStream(BUFSIZE);
            byte[] buf = new byte[BUFSIZE];
            int n = 0;
            while ((n = is.read(buf, 0, buf.length)) >= 0)
            {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();

        }
        catch (Exception ex)
        {
            return null;
        }
        finally
        {
            try
            {
                if (baos != null) baos.close();
            }
            catch (Exception ex)
            {
            }
            try
            {
                if (is != null) is.close();
            }
            catch (Exception ex)
            {
            }
        }
    }

    public synchronized InputStream getEntryAsStream(String name)
        throws IllegalStateException, IOException
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return m_dir.resolveFile( name, NameScope.CHILD).getContent().getInputStream();
    }

    public synchronized IContent getEntryAsContent(String entryName)
    {
        try
        {
            // If the entry name refers to the content itself, then
            // just return it immediately.
            if (entryName.equals(FelixConstants.CLASS_PATH_DOT))
            {
                return new DirectoryContent(m_logger, m_revisionLock, m_rootDir, m_dir);
            }

            // Remove any leading slash, since all bundle class path
            // entries are relative to the root of the bundle.
            entryName = (entryName.startsWith("/")) ? entryName.substring(1) : entryName;

            // Any embedded JAR files will be extracted to the embedded directory.
            FileObject embedDir = m_rootDir.resolveFile( m_dir.getName() + EMBEDDED_DIRECTORY, NameScope.CHILD);

            // Determine if the entry is an emdedded JAR file or
            // directory in the bundle JAR file. Ignore any entries
            // that do not exist per the spec.
            FileObject file = m_dir.resolveFile( entryName, NameScope.CHILD);
            if (BundleCache.getSecureAction().isFileDirectory(file))
            {
                return new DirectoryContent(m_logger, m_revisionLock, m_rootDir, file);
            }
            else if (BundleCache.getSecureAction().fileExists(file)
                && entryName.endsWith(".jar"))
            {
                FileObject extractedDir = embedDir.resolveFile( 
                    (entryName.lastIndexOf('/') >= 0)
                        ? entryName.substring(0, entryName.lastIndexOf('/'))
                        : entryName, NameScope.CHILD);
                synchronized (m_revisionLock)
                {
                    if (!BundleCache.getSecureAction().fileExists(extractedDir))
                    {
                        if (!BundleCache.getSecureAction().mkdirs(extractedDir))
                        {
                            m_logger.log(
                                Logger.LOG_ERROR,
                                "Unable to extract embedded directory.");
                        }
                    }
                }
                return new JarContent(m_logger, m_revisionLock, extractedDir, file);
            }

            // The entry could not be found, so return null.
            return null;
        }
        catch ( FileSystemException e )
        {
            throw new RuntimeException(e);
        }
    }

// TODO: This will need to consider security.
    public synchronized String getEntryAsNativeLibrary(String name)
    {
        try
        {
            return BundleCache.getSecureAction().getAbsolutePath(m_rootDir.resolveFile( name, NameScope.CHILD));
        }
        catch ( FileSystemException e )
        {
            throw new RuntimeException(e);
        }
    }

    public String toString()
    {
        return "DIRECTORY " + m_dir;
    }

    private static class EntriesEnumeration implements Enumeration
    {
        private FileObject m_dir = null;
        private FileObject[] m_children = null;
        private int m_counter = 0;

        public EntriesEnumeration(FileObject dir) 
        throws FileSystemException
        {
            m_dir = dir;
            m_children = listFilesRecursive(m_dir);
        }

        public boolean hasMoreElements()
        {
            return (m_children != null) && (m_counter < m_children.length);
        }

        public Object nextElement()
        {
            try
            {
                if ((m_children == null) || (m_counter >= m_children.length))
                {
                    throw new NoSuchElementException("No more entry paths.");
                }

                // Convert the file separator character to slashes.
                String abs = m_children[m_counter].getName().getPathDecoded()
                    .replace(File.separatorChar, '/');

                // Remove the leading path of the reference directory, since the
                // entry paths are supposed to be relative to the root.
                StringBuffer sb = new StringBuffer(abs);
                sb.delete(0, m_dir.getName().getPathDecoded().length() + 1);
                // Add a '/' to the end of directory entries.
                if (FileType.FOLDER.equals( m_children[m_counter].getType()))
                {
                    sb.append('/');
                }
                m_counter++;
                return sb.toString();
            }
            catch ( FileSystemException e )
            {
                throw new RuntimeException(e);
            }
        }

        public FileObject[] listFilesRecursive(FileObject dir) 
        throws FileSystemException
        {
            FileObject[] children = dir.getChildren();
            FileObject[] combined = children;
            for (int i = 0; i < children.length; i++)
            {
                if (FileType.FOLDER.equals( children[i].getType()))
                {
                    FileObject[] grandchildren = listFilesRecursive(children[i]);
                    if (grandchildren.length > 0)
                    {
                        FileObject[] tmp = new FileObject[combined.length + grandchildren.length];
                        System.arraycopy(combined, 0, tmp, 0, combined.length);
                        System.arraycopy(grandchildren, 0, tmp, combined.length, grandchildren.length);
                        combined = tmp;
                    }
                }
            }
            return combined;
        }
    }
}