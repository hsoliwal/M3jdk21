// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import jdk.m3.text.FrameCollector;
import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;
import jdk.m3.text.TextTools;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.regex.Pattern;

/** Explicit filesystem atoms. Paths and side effects are supplied by the host, not a text plan. */
public final class FileTools {
    private FileTools() { }

    /** Relative paths in deterministic UTF-16 order; does not follow directory symlinks. */
    public static TextFrame find(Path root, int maxDepth, Pattern match, Limits limits) throws IOException {
        Objects.requireNonNull(root, "root"); Objects.requireNonNull(match, "match");
        if (maxDepth < 0) throw new IllegalArgumentException("negative depth");
        FrameCollector result = new FrameCollector(limits);
        try (var paths = Files.walk(root, maxDepth)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                String path = root.relativize(iterator.next()).toString().replace('\\', '/');
                if (path.isEmpty()) path = ".";
                if (match.matcher(path).matches()) result.accept(new TextLine(path, "\n"));
            }
        }
        return TextTools.sort(result.frame(), false, limits);
    }

    /** Stages UTF-8 output next to the destination; publication happens only after a successful write. */
    public static TextFrame tee(TextFrame input, Path destination, boolean replace, Limits limits) throws IOException {
        limits.check(input);
        Path target = Objects.requireNonNull(destination, "destination").toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".m3-tee-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                for (TextLine line : input.lines()) { writer.write(line.content()); writer.write(line.ending()); }
            }
            if (replace) Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            else Files.move(temporary, target);
            return input;
        } finally { Files.deleteIfExists(temporary); }
    }

    /** POSIX FIFO creation; mode is limited to permission bits and still subject to the process umask. */
    public static void mkfifo(Path path, int mode) throws IOException {
        if ((mode & ~0777) != 0) throw new IllegalArgumentException("invalid permission bits");
        String name = Objects.requireNonNull(path, "path").toAbsolutePath().toString();
        // Match the text substrate's Unicode contract before crossing JNI.
        TextFrame.fromString(name);
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > 65535 || name.indexOf('\0') >= 0)
            throw new IllegalArgumentException("invalid FIFO path");
        Native.ensureLoaded();
        int error = mkfifo0(bytes, mode);
        if (error != 0) throw new FileSystemException(name, null, "mkfifo errno " + error);
    }

    private static native int mkfifo0(byte[] path, int mode);
    private static final class Native {
        static {
            Path bundled = Path.of(System.getProperty("java.home"), "lib", System.mapLibraryName("m3textio"));
            if (Files.isRegularFile(bundled)) System.load(bundled.toAbsolutePath().toString());
            else System.loadLibrary("m3textio");
        }
        static void ensureLoaded() { }
    }
}
