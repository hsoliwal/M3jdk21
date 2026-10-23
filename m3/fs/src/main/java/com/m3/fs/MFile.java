/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.OptionalLong;

import com.m3.fs.vfs.FileObject;

/**
 * An immutable metadata projection of one entry encountered by an {@link MFiles}
 * traversal. Every encountered entry has a distinct object; this is not a mutable
 * iterator cursor. Metadata remains available after the owning stream closes.
 *
 * <p>Content is provider-backed and available only while that stream is open.
 * Metadata is captured when an entry is encountered. It is not a filesystem or
 * content snapshot, and an external change can affect a later content read.</p>
 */
public final class MFile implements MFileContent {
    private final String name;
    private final String relativeName;
    private final URI uri;
    private final String sourceAddress;
    private final boolean file;
    private final boolean directory;
    private final boolean symbolicLink;
    private final OptionalLong size;
    private final FileObject source;
    private final MFiles.Session owner;

    MFile(String name, String relativeName, URI uri, String sourceAddress,
            boolean file, boolean directory, boolean symbolicLink, OptionalLong size,
            FileObject source, MFiles.Session owner) {
        this.name = name;
        this.relativeName = relativeName;
        this.uri = uri;
        this.sourceAddress = sourceAddress;
        this.file = file;
        this.directory = directory;
        this.symbolicLink = symbolicLink;
        this.size = size;
        this.source = source;
        this.owner = owner;
    }

    /** Returns the decoded final name component. */
    public String name() {
        return name;
    }

    /**
     * Returns the decoded path relative to the selected directory, using {@code /}
     * separators. A selected root file uses its name. The un-emitted directory
     * root passed to a descent predicate uses the empty string. Automatic archive
     * boundaries use {@code !/}. Literal names containing {@code !} can collide
     * with that display notation; use {@link #sourceAddress()} for identity, not
     * this display path or an unchecked destination-path mapping.
     */
    public String relativeName() {
        return relativeName;
    }

    /** Returns the provider URI captured when this entry was encountered. */
    public URI uri() {
        return uri;
    }

    /**
     * Returns the provider's resolvable address. It can contain credentials when
     * the caller supplied them; callers should not publish it indiscriminately.
     */
    public String sourceAddress() {
        return sourceAddress;
    }

    /** Returns whether the provider's entry type permits content. */
    public boolean isFile() {
        return file;
    }

    /** Returns whether the provider's entry type permits children. */
    public boolean isDirectory() {
        return directory;
    }

    /** Returns the provider's symbolic-link classification. */
    public boolean isSymbolicLink() {
        return symbolicLink;
    }

    /** Returns the captured file size, or empty for an entry without content or an unknown size. */
    public OptionalLong size() {
        return size;
    }

    /**
     * Opens a provider input stream without buffering the complete file.
     * Close it promptly; closing the owning traversal also closes any outstanding
     * input streams. Each call opens a new reader, subject to provider capabilities.
     * If primary close fails, a later close reports the retained failure without
     * retrying provider cleanup, including when the owner performed the first close.
     * Reentrant close while primary cleanup is in progress reports checked IO.
     *
     * @return an owner-scoped content stream
     * @throws IOException if the owning stream has closed, this is not a file,
     *         or the provider cannot open its content
     */
    @Override
    public InputStream openInputStream() throws IOException {
        return owner.openInputStream(source, file);
    }

    /**
     * Explicitly opens provider output. {@code append == false} replaces content;
     * {@code true} appends when the provider supports it. Traversal never opens
     * output by itself. The writer is owned by this entry's traversal session and
     * closes when that session closes. Read-only providers report checked IO.
     * Captured metadata does not change when content is written.
     *
     * @throws IOException if the owner is closed, this is not a content entry,
     *         or the provider does not support the requested output
     */
    @Override
    public OutputStream openOutputStream(boolean append) throws IOException {
        return owner.openOutputStream(source, file, append);
    }

    /**
     * Explicitly refreshes provider metadata and captures a detached observation
     * for change comparison. This performs provider I/O while the owner is open;
     * errors remain checked and are never interpreted as deletion. The returned
     * observation and this entry's original metadata remain stable after close.
     * Provider refresh and individual metadata queries do not imply an atomic
     * filesystem snapshot or authoritative change token.
     *
     * @throws IOException if the owner is closed or metadata cannot be observed
     */
    @Override
    public MFileObservation observation() throws IOException {
        return owner.observe(source, sourceAddress);
    }

    @Override
    public String toString() {
        return relativeName;
    }
}
