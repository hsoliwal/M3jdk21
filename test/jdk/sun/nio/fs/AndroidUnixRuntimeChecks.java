/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 */

/*
 * @test
 * @summary Check Unix identity lookup and descriptor-relative file operations on Android
 * @requires (os.family == "linux")
 * @run main/othervm/native AndroidUnixRuntimeChecks
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AndroidUnixRuntimeChecks {
    static {
        System.loadLibrary("AndroidUnixRuntimeChecks");
    }

    private static native boolean hasDescriptorRelativeCalls();

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("android-unix-runtime-");
        try {
            checkIdentityLookups(directory);
            checkDirectoryOperations(directory);
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("Unix identity and descriptor-relative file checks passed");
    }

    private static void checkIdentityLookups(Path directory) throws Exception {
        PosixFileAttributes attributes = Files.readAttributes(directory, PosixFileAttributes.class);
        var owner = attributes.owner();
        var group = attributes.group();
        UserPrincipalLookupService lookup = directory.getFileSystem().getUserPrincipalLookupService();
        if (!lookup.lookupPrincipalByName(owner.getName()).equals(owner) ||
            !lookup.lookupPrincipalByGroupName(group.getName()).equals(group)) {
            throw new AssertionError("Filesystem identities did not round-trip by name");
        }
        String missing = "m3jdk-missing-" + UUID.randomUUID();
        try {
            lookup.lookupPrincipalByName(missing);
            throw new AssertionError("Missing user was resolved");
        } catch (UserPrincipalNotFoundException expected) {
            // A normal absent name keeps its public Java exception contract.
        }
        try {
            lookup.lookupPrincipalByGroupName(missing);
            throw new AssertionError("Missing group was resolved");
        } catch (UserPrincipalNotFoundException expected) {
            // An absent group must not become a fabricated identity.
        }
        try (var pool = Executors.newFixedThreadPool(8)) {
            List<Callable<Void>> calls = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                calls.add(() -> {
                    for (int j = 0; j < 100; j++) {
                        if (!lookup.lookupPrincipalByName(owner.getName()).equals(owner) ||
                            !lookup.lookupPrincipalByGroupName(group.getName()).equals(group)) {
                            throw new AssertionError("Concurrent identity lookup changed its result");
                        }
                    }
                    return null;
                });
            }
            for (var result : pool.invokeAll(calls)) result.get();
        }
    }

    private static void checkDirectoryOperations(Path directory) throws Exception {
        Path original = Path.of("original");
        Path moved = Path.of("moved");
        byte[] contents = {7, 13, 29, 43};
        Files.write(directory.resolve(original), contents);
        try (DirectoryStream<Path> opened = Files.newDirectoryStream(directory)) {
            if (!(opened instanceof SecureDirectoryStream<Path> secure)) {
                if (hasDescriptorRelativeCalls()) {
                    throw new AssertionError("Usable descriptor-relative APIs did not enable SecureDirectoryStream");
                }
                System.out.println("Descriptor-relative APIs are unavailable on this platform");
                return;
            }
            BasicFileAttributeView view = secure.getFileAttributeView(original, BasicFileAttributeView.class);
            FileTime access = FileTime.from(1_700_000_123_456_789L, TimeUnit.MICROSECONDS);
            FileTime modified = FileTime.from(1_700_100_321_987_654L, TimeUnit.MICROSECONDS);
            view.setTimes(modified, access, null);
            BasicFileAttributes observed = view.readAttributes();
            if (observed.lastAccessTime().to(TimeUnit.MICROSECONDS) != access.to(TimeUnit.MICROSECONDS) ||
                observed.lastModifiedTime().to(TimeUnit.MICROSECONDS) != modified.to(TimeUnit.MICROSECONDS)) {
                throw new AssertionError("Descriptor-relative timestamps lost microsecond precision");
            }
            secure.move(original, secure, moved);
            try (SeekableByteChannel channel = secure.newByteChannel(moved, Set.of(StandardOpenOption.READ))) {
                ByteBuffer bytes = ByteBuffer.allocate(contents.length + 1);
                while (bytes.hasRemaining() && channel.read(bytes) != -1) { }
                if (bytes.position() != contents.length) throw new AssertionError("Incorrect moved-file size");
                for (int i = 0; i < contents.length; i++) {
                    if (bytes.get(i) != contents[i]) throw new AssertionError("Moved-file payload changed");
                }
            }
            secure.deleteFile(moved);
            try {
                view.readAttributes();
                throw new AssertionError("A missing relative path still had attributes");
            } catch (IOException expected) {
                // The descriptor-relative error remains visible to Java.
            }
            Path child = Path.of("child");
            Files.createDirectory(directory.resolve(child));
            try (SecureDirectoryStream<Path> nested = secure.newDirectoryStream(child)) {
                if (nested.iterator().hasNext()) throw new AssertionError("New directory was not empty");
            }
            secure.deleteDirectory(child);
        }
    }
}
