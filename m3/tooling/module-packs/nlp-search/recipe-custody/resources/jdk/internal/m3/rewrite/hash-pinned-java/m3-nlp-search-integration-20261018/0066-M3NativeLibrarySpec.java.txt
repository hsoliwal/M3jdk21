// Copyright 2026 Hitesh Soliwal and contributors
// Full donor dependency receiver; source custody in JSON_CLOSURE_PROVENANCE.json.
// SPDX-License-Identifier: Apache-2.0

package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** One native library expressed as either an explicit path or a platform-resolved logical name. */
public record M3NativeLibrarySpec(String logicalName, Path explicitPath) {
    private static final int MAX_PATH_FILE_BYTES = 65_536;

    public M3NativeLibrarySpec {
        logicalName = logicalName == null ? "" : logicalName.trim();
        explicitPath =
                explicitPath == null ? null : explicitPath.toAbsolutePath().normalize();
        if (logicalName.isBlank() && explicitPath == null) {
            throw new IllegalArgumentException("A logical library name or explicit path is required");
        }
    }

    public static M3NativeLibrarySpec named(String logicalName) {
        return new M3NativeLibrarySpec(Objects.requireNonNull(logicalName, "logicalName"), null);
    }

    public static M3NativeLibrarySpec at(Path explicitPath) {
        return new M3NativeLibrarySpec("", Objects.requireNonNull(explicitPath, "explicitPath"));
    }

    /**
     * Resolve an explicit path, a CMake-generated path file, or a logical name, in that order.
     *
     * <p>A configured path file is a bounded UTF-8 file containing exactly one nonblank path,
     * optionally followed by LF or CRLF. Relative library paths are resolved against the file's
     * directory. Invalid or unreadable configured files fail instead of silently selecting a
     * different library. The file is read once per call; the returned specification is a snapshot.
     */
    public static M3NativeLibrarySpec fromProperties(String prefix, String defaultLogicalName) {
        Objects.requireNonNull(prefix, "prefix");
        String path = System.getProperty(prefix + ".path");
        if (path != null && !path.isBlank()) {
            return at(Path.of(path));
        }
        String pathFile = System.getProperty(prefix + ".pathFile");
        if (pathFile != null && !pathFile.isBlank()) {
            return fromPathFile(Path.of(pathFile));
        }
        String name = System.getProperty(prefix + ".name", defaultLogicalName);
        return named(name);
    }

    private static M3NativeLibrarySpec fromPathFile(Path pathFile) {
        Path file = pathFile.toAbsolutePath().normalize();
        try (InputStream input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(MAX_PATH_FILE_BYTES + 1);
            if (bytes.length > MAX_PATH_FILE_BYTES) {
                throw new IllegalArgumentException("Native library path file exceeds byte limit");
            }
            String path = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (path.endsWith("\r\n")) path = path.substring(0, path.length() - 2);
            else if (path.endsWith("\n")) path = path.substring(0, path.length() - 1);
            if (path.isBlank() || path.indexOf('\n') >= 0 || path.indexOf('\r') >= 0
                    || path.indexOf('\0') >= 0) {
                throw new IllegalArgumentException(
                        "Native library path file must contain exactly one nonblank path");
            }
            Path library = Path.of(path);
            return at(library.isAbsolute() ? library : file.getParent().resolve(library));
        } catch (IOException failure) {
            throw new IllegalArgumentException("Cannot read native library path file: " + file,
                    failure);
        }
    }

    public boolean explicit() {
        return explicitPath != null;
    }

    public String loadTarget() {
        return explicit() ? explicitPath.toString() : logicalName;
    }

    public String cacheKey() {
        return explicit() ? "path:" + explicitPath : "name:" + logicalName;
    }
}
