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
package org.apache.commons.vfs.impl;

import org.apache.commons.vfs.FileSystemConfigBuilder;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.UserAuthenticator;

/**
 * Default options usable for all filesystems
 * @author <a href="http://commons.apache.org/vfs/team-list.html">Commons VFS team</a>
 */
public class DefaultFileSystemConfigBuilder extends FileSystemConfigBuilder
{
    /** The default FileSystemConfigBuilder */
    private static final DefaultFileSystemConfigBuilder BUILDER = new DefaultFileSystemConfigBuilder();

    public static DefaultFileSystemConfigBuilder getInstance()
    {
        return BUILDER;
    }

    /**
     * Sets the user authenticator to get authentication informations
     * @param opts The FileSystemOptions.
     * @param userAuthenticator The UserAuthenticator.
     * @throws FileSystemException if an error occurs setting the UserAuthenticator.
     */
    public void setUserAuthenticator(FileSystemOptions opts, UserAuthenticator userAuthenticator)
            throws FileSystemException
    {
        setParam(opts, "userAuthenticator", userAuthenticator);
    }

    /**
     * @see #setUserAuthenticator
     * @param opts The FileSystemOptions.
     * @return The UserAuthenticator.
     */
    public UserAuthenticator getUserAuthenticator(FileSystemOptions opts)
    {
        return (UserAuthenticator) getParam(opts, "userAuthenticator");
    }

    protected Class getConfigClass()
    {
        return DefaultFileSystemConfigBuilder.class;
    }
}
