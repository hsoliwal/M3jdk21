// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import com.m3.fs.M3FS;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Native-backed contract: stop paged enumeration before deleting any child. */
final class M3EfsPagedCancellationTest {
    private static final int PAGE_SIZE = 512;
    private static final int ENTRIES = PAGE_SIZE + 8;

    @TempDir Path temporary;

    @Test
    void cancellationWhileLoadingSecondDirectoryPagePreservesAllChildren() throws Exception {
        assertTrue(M3FS.nativeAvailable(), "Real M3FS JNI is mandatory, not a skipped test");
        M3EclipseFileSystem filesystem = new M3EclipseFileSystem();
        try {
            M3FS image = M3FS.create(temporary.resolve("paged-cancel.m3fs"));
            filesystem.initialize("m3fs");
            IFileStore directory = filesystem.mount("work", image)
                    .getChild("paged").mkdir(EFS.NONE, null);
            for (int index = 0; index < ENTRIES; index++) {
                String name = String.format(Locale.ROOT, "f%04d", index);
                try (OutputStream output = directory.getChild(name)
                        .openOutputStream(EFS.NONE, null)) {
                    output.write(index & 255);
                }
            }

            AtomicInteger polls = new AtomicInteger();
            NullProgressMonitor monitor = new NullProgressMonitor() {
                @Override public boolean isCanceled() {
                    return polls.incrementAndGet() >= 4;
                }
            };
            assertThrows(OperationCanceledException.class,
                    () -> directory.delete(EFS.NONE, monitor));
            assertEquals(4, polls.get(), "must poll before the second native page");
            assertEquals(ENTRIES, directory.childNames(EFS.NONE, null).length,
                    "no child deletion may precede cancellation during enumeration");
        } finally {
            filesystem.close();
        }
    }
}
