// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable exact source/dependency snapshot. Paths are logical, never implicit filesystem authority. */
public final class SealedSources {
    public static final int MAX_FILE_BYTES = 4 * 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = 64L * 1024 * 1024;
    public static final int MAX_FILES = 4096;
    private final Map<String, String> files;
    private final String root;
    public SealedSources(Map<String, String> input) {
        Objects.requireNonNull(input, "input");
        if (input.size() > MAX_FILES) throw new IllegalArgumentException("FILE_BUDGET");
        TreeMap<String, String> copy = new TreeMap<>();
        long bytes = 0;
        for (var e : input.entrySet()) {
            String key = path(e.getKey()), value = Objects.requireNonNull(e.getValue(), "source");
            int size = SealHash.utf8(value).length;
            if (size > MAX_FILE_BYTES || (bytes += size) > MAX_TOTAL_BYTES) throw new IllegalArgumentException("BYTE_BUDGET");
            copy.put(key, value);
        }
        files = Collections.unmodifiableMap(copy);
        StringBuilder material = new StringBuilder("SEALED-SOURCES/1\n");
        copy.forEach((key, value) -> material.append(SealHash.frame(key, SealHash.text(value))).append('\n'));
        root = SealHash.text(material.toString());
    }
    public Map<String, String> files() { return files; }
    public String root() { return root; }
    public String require(String path) {
        String text = files.get(path(path));
        if (text == null) throw new IllegalArgumentException("SOURCE_NOT_IN_SNAPSHOT:" + path);
        return text;
    }
    public String hash(String path) { return SealHash.text(require(path)); }
    /** Replacements only; creation/deletion requires a separately admitted parent task. */
    public SealedSources replace(Map<String, String> replacements) {
        TreeMap<String, String> result = new TreeMap<>(files);
        replacements.forEach((path, text) -> { require(path); result.put(path, text); });
        return new SealedSources(result);
    }
    public Set<String> changed(SealedSources other) {
        TreeSet<String> paths = new TreeSet<>(files.keySet()); paths.addAll(other.files.keySet());
        paths.removeIf(path -> Objects.equals(files.get(path), other.files.get(path)));
        return Collections.unmodifiableSet(paths);
    }
    public static String path(String value) {
        Objects.requireNonNull(value, "path");
        if (value.isEmpty() || value.length() > 4096 || value.startsWith("/")
                || !value.equals(value.strip()) || value.matches(".*[\\\\:;*?\\[\\]{}].*")) {
            throw new IllegalArgumentException("UNSAFE_LOGICAL_PATH");
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..") || !part.equals(part.strip())) {
                throw new IllegalArgumentException("UNSAFE_LOGICAL_PATH");
            }
        }
        if (value.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("CONTROL_IN_PATH");
        SealHash.utf8(value);
        return value;
    }
}
