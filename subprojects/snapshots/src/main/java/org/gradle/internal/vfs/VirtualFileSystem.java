/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.vfs;

import org.gradle.internal.hash.HashCode;
import org.gradle.internal.snapshot.FileSystemLocationSnapshot;
import org.gradle.internal.snapshot.FileSystemSnapshot;
import org.gradle.internal.snapshot.FileSystemSnapshotBuilder;
import org.gradle.internal.snapshot.FileSystemSnapshotVisitor;
import org.gradle.internal.snapshot.SnapshottingFilter;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface VirtualFileSystem {

    void read(String location, FileSystemSnapshotVisitor visitor);

    /**
     * Visits the hash of the content of the file only if the file is a regular file.
     */
    <T> Optional<T> readRegularFileContentHash(String location, Function<HashCode, T> visitor);

    void read(String location, SnapshottingFilter filter, FileSystemSnapshotVisitor visitor);

    void update(Iterable<String> locations, Runnable action);

    void invalidateAll();

    void updateWithKnownSnapshot(String location, FileSystemLocationSnapshot snapshot);

    FileSystemSnapshot snapshotWithBuilder(Consumer<FileSystemSnapshotBuilder> buildAction);
}