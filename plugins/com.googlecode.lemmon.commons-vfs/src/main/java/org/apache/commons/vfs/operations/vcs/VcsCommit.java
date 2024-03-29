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
package org.apache.commons.vfs.operations.vcs;

import org.apache.commons.vfs.operations.FileOperation;

/**
 * todo: add class description here
 *
 * @author Siarhei Baidun
 * @since 0.1
 */
public interface VcsCommit extends FileOperation
{

    /**
     *
     * @param isRecursive
     */
    void setRecursive(final boolean isRecursive);

    /**
     *
     * @param message
     */
    void setMessage(final String message);

    /**
     *
     * @param listener
     */
    void addCommitListener(final VcsCommitListener listener);

    /**
     *
     * @param listener
     */
    void removeCommitListener(final VcsCommitListener listener);
}
