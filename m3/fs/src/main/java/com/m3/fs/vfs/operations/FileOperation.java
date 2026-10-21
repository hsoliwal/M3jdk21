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
// M3FS adaptation, Copyright 2026 Hitesh Soliwal and contributors.
// Original Apache license and attribution retained.
// Relocated Apache Commons VFS 2.10.0 to the M3FS namespace.
package com.m3.fs.vfs.operations;

import com.m3.fs.vfs.FileSystemException;

/**
 * <p>
 * A FileOperation is an object that brings an extra function to a FileObject. The VFS provides the basic functionality
 * to deal with FileObject's. That is create, delete, rename, copy, and so on functions. However, if you are working
 * with FileSystem and its files are, for example, under Version Control System (VCS) you might want to get an access to
 * the versioning framework and to be able to manage your files regarding VCS (e.g. commit them, update, get logs,
 * etc.). Such type of extended functionality is provided by FileOperation.
 * </p>
 * <p>
 * The FileOperation interface is a genetic interface that should not be implemented directly. It rather should be
 * extended by other interfaces that provide some concrete functions.
 * </p>
 * <p>
 * FileOperation is provided by
 * </p>
 *
 * @see FileOperationProvider Especially the FileOperationProvider is responsible for looking up and instantiating any
 *      concrete FileOperation.
 *
 * @since 0.1
 */
public interface FileOperation {

    /**
     * Performs necessary actions that are related to the concrete implementation of a FileOperation.
     *
     * @throws FileSystemException if an error occurs
     */
    void process() throws FileSystemException;
}
