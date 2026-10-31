// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.attribute.FileAttributeView;
import java.util.Objects;

/**
 * Optional NIO provider view for content generation observations. Providers
 * implement this view only when they can issue non-reused generation tokens.
 * It is requested with NOFOLLOW_LINKS. IO failures propagate; unsupported
 * providers return no view through the usual NIO provider contract.
 */
public interface M3FileVersionView extends FileAttributeView {
    @Override default String name() { return "m3version"; }

    /**
     * Observe a provider generation. Every content modification or replacement
     * must change the token, including same-length writes with restored times.
     * Tokens must never be recycled within a namespace, even after deletion,
     * restart or remount; change the namespace when continuity cannot be kept.
     * Equality is meaningful only for the same provider/path identity. Reading
     * a token is not a lock or a claim of an atomic multi-file snapshot.
     */
    Version readVersion() throws IOException;

    /** Immutable, opaque provider namespace and generation; neither is a timestamp. */
    record Version(String namespace, String generation) {
        public Version {
            Objects.requireNonNull(namespace, "namespace");
            Objects.requireNonNull(generation, "generation");
            if (namespace.isEmpty() || generation.isEmpty()) {
                throw new IllegalArgumentException("empty version identity");
            }
        }
    }
}
