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
 * A {@link FileSelector} that selects everything.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 763740 $ $Date: 2009-04-09 12:22:31 -0500 (Thu, 09 Apr 2009) $
 */
public class AllFileSelector
    implements FileSelector
{
    /**
     * Determines if a file or folder should be selected.
     * @param fileInfo The file selection information.
     * @return true if the file should be selected, false otherwise.
     */
    public boolean includeFile(final FileSelectInfo fileInfo)
    {
        return true;
    }

    /**
     * Determines whether a folder should be traversed.
     * @param fileInfo The file selection information.
     * @return true if descendents should be traversed, fase otherwise.
     */
    public boolean traverseDescendents(final FileSelectInfo fileInfo)
    {
        return true;
    }
}
