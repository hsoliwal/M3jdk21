/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.eclipse.connect;

import com.m3.collections.M3Collections;
import com.m3.collections.M3IndexMaskCompactor.Backend;
import com.m3.file.M3ModuleResources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Manifest;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;

/**
 * OSGi Connect content whose directory and payload belong to M3ModuleResources.
 *
 * <p>Registration selects content for the next framework connect call. Updating
 * a registration never mutates a previously returned ConnectModule or revision.
 * The caller must keep each ModuleReference and its backing artifact immutable
 * for that revision, including any later reopen requested by the framework.
 *
 * <p>Original manifest main headers are supplied unchanged. No class loader is
 * supplied: the real OSGi framework retains resolution, imports, wiring, weaving,
 * class definition, services and start/stop authority. A module-path resource
 * reference is content, not permission to bypass JPMS or hot-load a JMOD.
 */
public final class M3ModuleConnector implements ModuleConnector {
    private final Backend backend;
    // Registration control plane, not a second map or object for every resource row.
    private final Map<String, ModuleReference> registrations = M3Collections.linkedHashMap();

    public M3ModuleConnector() { this(Backend.JAVA); }

    public M3ModuleConnector(Backend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    /** Register an immutable revision for a future install/update at this exact OSGi location. */
    public synchronized void register(String location, ModuleReference reference) {
        if (Objects.requireNonNull(location, "location").isEmpty()) {
            throw new IllegalArgumentException("empty Connect location");
        }
        registrations.put(location, Objects.requireNonNull(reference, "reference"));
    }

    /** Stop connecting future requests; already-issued framework revisions remain intact. */
    public synchronized void unregister(String location) {
        registrations.remove(Objects.requireNonNull(location, "location"));
    }

    @Override public void initialize(File storage, Map<String, String> configuration) {
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(configuration, "configuration");
    }

    @Override public synchronized Optional<ConnectModule> connect(String location) {
        ModuleReference reference = registrations.get(Objects.requireNonNull(location, "location"));
        if (reference == null) return Optional.empty();
        Content content = new Content(reference, backend);
        return Optional.of(() -> content);
    }

    @Override public Optional<BundleActivator> newBundleActivator() { return Optional.empty(); }

    /**
     * One framework revision. OSGi may close and reopen it repeatedly; each reopen
     * obtains a new reader of the same immutable reference. Existing entry handles
     * remain bound to their old opened owner and fail after that owner is closed.
     */
    public static final class Content implements ConnectContent {
        private final ModuleReference reference;
        private final Backend backend;
        private M3ModuleResources resources;
        private Optional<Map<String, String>> headers = Optional.empty();

        private Content(ModuleReference reference, Backend backend) {
            this.reference = reference;
            this.backend = backend;
        }

        /** Borrow while content is open, for a scoped Spring MetadataReader or another resource consumer. */
        public synchronized M3ModuleResources resources() {
            if (resources == null || !resources.isOpen()) throw new IllegalStateException("Connect content is closed");
            return resources;
        }

        @Override public synchronized void open() throws IOException {
            if (resources != null && resources.isOpen()) return;
            M3ModuleResources opened = M3ModuleResources.open(reference);
            try {
                Optional<Map<String, String>> manifestHeaders = Optional.empty();
                var manifestStream = opened.open("META-INF/MANIFEST.MF");
                if (manifestStream.isPresent()) {
                    try (InputStream input = manifestStream.get()) {
                        Map<String, String> original = M3Collections.linkedHashMap();
                        new Manifest(input).getMainAttributes().forEach(
                                (name, value) -> original.put(name.toString(), value.toString()));
                        manifestHeaders = Optional.of(java.util.Collections.unmodifiableMap(original));
                    }
                }
                headers = manifestHeaders;
                resources = opened;
            }
            catch (IOException | RuntimeException | Error failure) {
                try { opened.close(); }
                catch (Throwable closeFailure) {
                    if (closeFailure != failure) failure.addSuppressed(closeFailure);
                }
                throw failure;
            }
        }

        @Override public synchronized void close() throws IOException {
            M3ModuleResources closing = resources;
            resources = null;
            headers = Optional.empty();
            if (closing != null) closing.close();
        }

        @Override public synchronized Optional<Map<String, String>> getHeaders() {
            resources();
            return headers;
        }

        @Override public synchronized Iterable<String> getEntries() {
            M3ModuleResources current = resources();
            int[] selected = current.select("", M3ModuleResources.RESOURCE, 0, backend);
            // API requires Iterable<String>; do not retain per-row ConnectEntry objects.
            return () -> new java.util.Iterator<>() {
                private int position;
                @Override public boolean hasNext() { return position < selected.length; }
                @Override public String next() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    return current.nameAt(selected[position++]);
                }
            };
        }

        @Override public synchronized Optional<ConnectEntry> getEntry(String path) {
            M3ModuleResources current = resources();
            Objects.requireNonNull(path, "path");
            return current.canonicalName(path).map(name -> new Entry(current, name));
        }

        @Override public synchronized Optional<ClassLoader> getClassLoader() {
            resources();
            return Optional.empty();
        }
    }

    private static final class Entry implements ConnectContent.ConnectEntry {
        private final M3ModuleResources resources;
        private final String name;
        Entry(M3ModuleResources resources, String name) {
            this.resources = resources;
            this.name = name;
        }
        @Override public String getName() { return name; }
        @Override public long getContentLength() { return -1; }
        @Override public long getLastModified() { return 0; }
        @Override public InputStream getInputStream() throws IOException { return resources.openStream(name); }
    }
}
