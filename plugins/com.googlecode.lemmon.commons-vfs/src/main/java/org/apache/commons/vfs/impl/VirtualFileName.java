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

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileName;

/**
 * A simple Filename to hold the scheme for to be created virtual filesytsem.
 *
 * @author imario@apache.org
 * @version $Revision: 480428 $ $Date: 2006-11-29 00:15:24 -0600 (Wed, 29 Nov 2006) $
 */
public class VirtualFileName extends AbstractFileName
{
    public VirtualFileName(final String scheme, final String absPath, final FileType type)
    {
        super(scheme, absPath, type);
    }

    public FileName createName(String absPath, FileType type)
    {
        return new VirtualFileName(getScheme(), absPath, type);
    }

    protected void appendRootUri(StringBuffer buffer, boolean addPassword)
    {
        buffer.append(getScheme());
    }
}
