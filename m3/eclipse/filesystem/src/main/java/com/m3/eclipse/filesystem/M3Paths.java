// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Namespace validation without host path interpretation or retained entry objects. */
final class M3Paths {
    private M3Paths() { }

    static String mount(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty() || name.length() > 64) throw new IllegalArgumentException("Invalid M3FS mount name");
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9')
                    && !(i > 0 && (c == '-' || c == '.'))) {
                throw new IllegalArgumentException("Invalid M3FS mount name");
            }
        }
        return name;
    }

    static String uriMount(URI uri) {
        Objects.requireNonNull(uri, "uri");
if (!"m3fs".equals(uri.getScheme()) || uri.isOpaque() || uri.getRawAuthority() == null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || uri.getRawUserInfo() != null || uri.getPort() != -1) {
            throw new IllegalArgumentException("Expected m3fs://mount/path");
        }
        return mount(uri.getRawAuthority());
    }

    /** URI.getPath replaces malformed UTF-8; validate escapes before accepting that decoded path. */
    static String uriPath(URI uri) {
        String raw = uri.getRawPath();
        if (raw == null) throw new IllegalArgumentException("Absolute M3FS URI path required");
        for (int at = 0; at < raw.length();) {
            if (raw.charAt(at) != '%') { at++; continue; }
            int end = at;
            while (end < raw.length() && raw.charAt(end) == '%') end += 3;
            byte[] octets = new byte[(end - at) / 3];
            for (int i = 0; i < octets.length; i++) {
                int digit = at + i * 3 + 1;
                octets[i] = (byte) (Character.digit(raw.charAt(digit), 16) * 16
                        + Character.digit(raw.charAt(digit + 1), 16));
            }
            try {
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(octets));
            } catch (CharacterCodingException invalid) {
                throw new IllegalArgumentException("Invalid UTF-8 escape in M3FS URI", invalid);
            }
            at = end;
        }
        return path(uri.getPath());
    }

    static String path(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isEmpty() || path.charAt(0) != '/') throw new IllegalArgumentException("Absolute M3FS path required");
        if (path.equals("/")) return path;
        int component = 1;
        for (int i = 1; i <= path.length(); i++) {
            if (i == path.length() || path.charAt(i) == '/') {
                name(path.substring(component, i));
                component = i + 1;
            }
        }
        if (utf8Bytes(path) > 4096) throw new IllegalArgumentException("M3FS path exceeds 4096 UTF-8 bytes");
        return path;
    }

    static String name(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty() || name.equals(".") || name.equals("..") || name.indexOf('/') >= 0
                || utf8Bytes(name) > 255) throw new IllegalArgumentException("Invalid M3FS path component");
        return name;
    }

    private static int utf8Bytes(String value) {
        int bytes = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 0) throw new IllegalArgumentException("NUL in M3FS path");
            if (Character.isHighSurrogate(c)) {
                if (++i == value.length() || !Character.isLowSurrogate(value.charAt(i))) {
                    throw new IllegalArgumentException("Unpaired surrogate in M3FS path");
                }
                bytes = Math.addExact(bytes, 4);
            } else if (Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("Unpaired surrogate in M3FS path");
            } else bytes = Math.addExact(bytes, c < 128 ? 1 : c < 2048 ? 2 : 3);
        }
        return bytes;
    }

    static String child(String parent, String name) {
        return path((parent.equals("/") ? "/" : parent + "/") + name(name));
    }

    static String parent(String path) {
        if (path.equals("/")) return null;
        int at = path.lastIndexOf('/');
        return at == 0 ? "/" : path.substring(0, at);
    }

    static String basename(String path) {
        return path.equals("/") ? "" : path.substring(path.lastIndexOf('/') + 1);
    }

    static URI uri(String mount, String path) {
        try { return new URI("m3fs", mount, path, null, null); }
        catch (URISyntaxException impossible) { throw new IllegalArgumentException(impossible); }
    }

    static String linkTarget(String parent, String target) {
        if (target.isEmpty()) throw new IllegalArgumentException("Empty symbolic link target");
        String current = target.charAt(0) == '/' ? "/" : parent;
        int start = target.charAt(0) == '/' ? 1 : 0;
        for (int i = start; i <= target.length(); i++) {
            if (i != target.length() && target.charAt(i) != '/') continue;
            String part = target.substring(start, i);
            start = i + 1;
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                current = parent(current);
                if (current == null) throw new IllegalArgumentException("Symbolic link escapes M3FS root");
            } else current = child(current, part);
        }
        return path(current);
    }
}
