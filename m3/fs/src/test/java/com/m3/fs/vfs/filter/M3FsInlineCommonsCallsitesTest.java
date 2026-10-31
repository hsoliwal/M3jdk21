/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs.vfs.filter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.fs.vfs.FileFilter;
import com.m3.fs.vfs.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exact source-inlined Commons callsite semantics, no vendor runtime APIs. */
final class M3FsInlineCommonsCallsitesTest {
    @Test void sharedEmptyExceptionInfoPreservesIdentityAndValues() {
        FileSystemException left = new FileSystemException("code", null, (Object[]) null);
        FileSystemException right = new FileSystemException("code", null, (Object[]) null);
        assertSame(left.getInfo(), right.getInfo());
        assertEquals(0, left.getInfo().length);
        assertEquals("code", left.getCode());
        FileSystemException withInfo =
                new FileSystemException("code", null, new Object[] {"a", 42});
        assertArrayEquals(new String[] {"a", "42"}, withInfo.getInfo());
    }

    @Test void nullEmptyAndNullMemberFiltersKeepOriginalExceptions() {
        assertThrows(IllegalArgumentException.class,
                () -> new AndFileFilter((FileFilter[]) null));
        assertThrows(IllegalArgumentException.class,
                () -> new AndFileFilter(new FileFilter[0]));
        assertThrows(IllegalArgumentException.class,
                () -> new AndFileFilter(new FileFilter[] {null}));
    }

    @Test void wildcardSplitRetainsConsecutiveMaskCharacters() {
        assertArrayEquals(new String[] {"*", "a", "*", "?", "b"},
                WildcardFileFilter.splitOnTokens("*a**?b"));
        assertArrayEquals(new String[] {"*"}, WildcardFileFilter.splitOnTokens("***"));
        assertArrayEquals(new String[] {"name"}, WildcardFileFilter.splitOnTokens("name"));
        assertThrows(NullPointerException.class, () -> WildcardFileFilter.splitOnTokens(null));
    }

    @Test void changedOwnersContainNoCommonsLangImports() throws Exception {
        List<String> paths = List.of(
                "com/m3/fs/vfs/FileSystemException.java",
                "com/m3/fs/vfs/filter/AndFileFilter.java",
                "com/m3/fs/vfs/filter/WildcardFileFilter.java",
                "com/m3/fs/vfs/operations/DefaultFileOperations.java",
                "com/m3/fs/vfs/provider/AbstractFileName.java",
                "com/m3/fs/vfs/impl/DefaultFileSystemManager.java",
                "com/m3/fs/vfs/impl/StandardFileSystemManager.java");
        for (String path : paths) {
            Path source = Path.of("src/main/java", path);
            assertTrue(Files.isRegularFile(source), source.toString());
            String contents = Files.readString(source);
            assertEquals(false, contents.contains("import org.apache.commons.lang3."),
                    "inlined callsite still imports Commons Lang: " + path);
        }
    }
}
