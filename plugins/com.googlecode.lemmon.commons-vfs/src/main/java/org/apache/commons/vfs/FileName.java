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
package org.apache.commons.vfs;

/**
 * Represents a file name.  File names are immutable, and work correctly as
 * keys in hash tables.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 764356 $ $Date: 2009-04-12 23:06:01 -0500 (Sun, 12 Apr 2009) $
 * @see FileObject
 */
public interface FileName extends Comparable
{
    /**
     * The separator character used in file paths.
     */
    char SEPARATOR_CHAR = '/';

    /**
     * The separator used in file paths.
     */
    String SEPARATOR = "/";

    /**
     * The absolute path of the root of a file system.
     */
    String ROOT_PATH = "/";

    /**
     * Returns the base name of this file.  The base name is the last element
     * of the file name.  For example the base name of
     * <code>/somefolder/somefile</code> is <code>somefile</code>.
     * <p/>
     * <p>The root file of a file system has an empty base name.
     *
     * @return The base name.  Never returns null.
     */
    String getBaseName();

    /**
     * Returns the absolute path of this file, within its file system.  This
     * path is normalised, so that <code>.</code> and <code>..</code> elements
     * have been removed.  Also, the path only contains <code>/</code> as its
     * separator character.  The path always starts with <code>/</code>
     * <p/>
     * <p>The root of a file system has <code>/</code> as its absolute path.
     *
     * @return The path.  Never returns null.
     */
    String getPath();

    /**
     * Returns the absolute path of this file, within its file system.  This
     * path is normalised, so that <code>.</code> and <code>..</code> elements
     * have been removed.  Also, the path only contains <code>/</code> as its
     * separator character.  The path always starts with <code>/</code>
     * <p/>
     * <p>The root of a file system has <code>/</code> as its absolute path.
     * <p/>
     * In contrast to {@link #getPath()} the path is decoded i.e. all %nn stuff
     * replaced by its character.
     *
     * @return The path.  Never returns null.
     * @throws FileSystemException if the path is not correctly encoded
     */
    String getPathDecoded() throws FileSystemException;

    /**
     * Returns the extension of this file name.
     *
     * @return The extension.  Returns an empty string if the name has no
     *         extension.
     */
    String getExtension();

    /**
     * Returns the depth of this file name, within its file system.  The depth
     * of the root of a file system is 0.  The depth of any other file is
     * 1 + the depth of its parent.
     * @return The depth of this file name.
     */
    int getDepth();

    /**
     * Returns the URI scheme of this file.
     * @return The URI scheme of this file.
     */
    String getScheme();

    /**
     * Returns the absolute URI of this file.
     * @return the absolute URI of this file.
     */
    String getURI();

    /**
     * Returns the root URI of the file system this file belongs to.
     * @return the root URI.
     */
    String getRootURI();

    /**
     * find the root of the filesystem
     * @return the file system root.
     */
    FileName getRoot();

    /**
     * Returns the file name of the parent of this file.  The root of a
     * file system has no parent.
     *
     * @return A {@link FileName} object representing the parent name.  Returns
     *         null for the root of a file system.
     */
    FileName getParent();

    /**
     * Resolves a name, relative to this file name.  Equivalent to calling
     * <code>resolveName( path, NameScope.FILE_SYSTEM )</code>.
     *
     * @param name The name to resolve.
     * @return A {@link FileName} object representing the resolved file name.
     * @throws FileSystemException If the name is invalid.
     */
    // FileName resolveName(String name) throws FileSystemException;

    /**
     * Resolves a name, relative to this file name.  Refer to {@link NameScope}
     * for a description of how names are resolved.
     *
     * @param name  The name to resolve.
     * @param scope The scope to use when resolving the name.
     * @return A {@link FileName} object representing the resolved file name.
     * @throws FileSystemException If the name is invalid.
     */
    // FileName resolveName(String name, NameScope scope)
    //     throws FileSystemException;

    /**
     * Converts a file name to a relative name, relative to this file name.
     *
     * @param name The name to convert to a relative path.
     * @return The relative name.
     * @throws FileSystemException On error.
     */
    String getRelativeName(FileName name) throws FileSystemException;

    /**
     * Determines if another file name is an ancestor of this file name.
     * @param ancestor The FileName to check.
     * @return true if another file name is an ancestor of this file name.
     */
    boolean isAncestor(FileName ancestor);

    /**
     * Determines if another file name is a descendent of this file name.
     * @param descendent the FileName to check.
     * @return true if the other FileName is a descendent of this file name.
     */
    boolean isDescendent(FileName descendent);

    /**
     * Determines if another file name is a descendent of this file name.
     * @param descendent the FileName to check.
     * @param nameScope the NameScope of the FileName.
     * @return true if the other FileName is a descendent of this file name.
     */
    boolean isDescendent(FileName descendent, NameScope nameScope);

    /**
     * Returns the requested or current type of this name. <br />
     * <p/>
     * The "requested" type is the one determined during resolving the name. <br/>
     * In this case the name is a {@link FileType#FOLDER} if it ends with an "/" else
     * it will be a {@link FileType#FILE}<br/>
     * </p>
     * <p/>
     * Once attached it will be changed to reflect the real type of this resource.
     * </p>
     *
     * @return {@link FileType#FOLDER} or {@link FileType#FILE}
     */
    FileType getType();

    /**
     * returns a "friendly path", this is a path without a password.<br />
     * This path can not be used to resolve the path again
     * @return the friendly URI as a String.
     */
    String getFriendlyURI();
}
