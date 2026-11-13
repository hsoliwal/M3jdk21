/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One exact entry from a committed Git tree with a SHA-256 over its source bytes.
 *
 * @param mode exact six-octal-digit Git tree mode
 * @param objectType Git object type, either {@code blob} or {@code commit} for a gitlink
 * @param objectId exact lowercase Git object identity
 * @param path normalized repository-relative tree path
 * @param sourceSize exact blob size or canonical gitlink-pointer size
 * @param sourceSha256 SHA-256 of exact blob bytes or canonical gitlink-pointer bytes
 */
public record GitTreeEntry(
        String mode,
        String objectType,
        String objectId,
        String path,
        long sourceSize,
        String sourceSha256) {

    private static final Pattern MODE = Pattern.compile("[0-7]{6}");
    private static final Pattern OBJECT_ID = Pattern.compile("(?:[0-9a-f]{40}|[0-9a-f]{64})");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    /** Validates the Git and ledger identity at construction time. */
    public GitTreeEntry {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(objectType, "objectType");
        Objects.requireNonNull(objectId, "objectId");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(sourceSha256, "sourceSha256");
        if (!MODE.matcher(mode).matches()) {
            throw new IllegalArgumentException("invalid Git mode: " + mode);
        }
        if (!objectType.equals("blob") && !objectType.equals("commit")) {
            throw new IllegalArgumentException("unsupported Git object type: " + objectType);
        }
        if (!OBJECT_ID.matcher(objectId).matches()) {
            throw new IllegalArgumentException("invalid Git object id: " + objectId);
        }
        validatePath(path);
        if (sourceSize < 0) {
            throw new IllegalArgumentException("sourceSize must be nonnegative");
        }
        if (!SHA256.matcher(sourceSha256).matches()) {
            throw new IllegalArgumentException("sourceSha256 must be lowercase SHA-256");
        }
    }

    private static void validatePath(String value) {
        boolean windowsAbsolute = value.length() >= 3 && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':' && value.charAt(2) == '/';
        if (value.isBlank() || value.startsWith("/") || value.startsWith("\\") || value.contains("\\")
                || value.endsWith("/") || windowsAbsolute) {
            throw new IllegalArgumentException("path must be normalized and repository-relative: " + value);
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\t' || Character.isISOControl(character)) {
                throw new IllegalArgumentException("path contains a control character");
            }
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw new IllegalArgumentException("path contains an empty or traversal segment: " + value);
            }
        }
    }
}
