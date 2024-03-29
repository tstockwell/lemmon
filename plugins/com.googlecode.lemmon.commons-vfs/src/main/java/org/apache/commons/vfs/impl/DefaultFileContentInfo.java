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

import org.apache.commons.vfs.FileContentInfo;

/**
 * The default file content information.
 * @author <a href="http://commons.apache.org/vfs/team-list.html">Commons VFS team</a>
 */
public class DefaultFileContentInfo implements FileContentInfo
{
    private final String contentType;
    private final String contentEncoding;

    public DefaultFileContentInfo(final String contentType, final String contentEncoding)
    {
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getContentEncoding()
    {
        return contentEncoding;
    }
}
