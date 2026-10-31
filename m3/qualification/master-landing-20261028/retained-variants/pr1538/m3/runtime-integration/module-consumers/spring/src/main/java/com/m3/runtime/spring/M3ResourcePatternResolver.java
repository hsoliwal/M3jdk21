/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.spring;

import com.m3.collections.M3Collections;
import com.m3.collections.M3IndexMaskCompactor.Backend;
import com.m3.file.M3ModuleResources;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Supplies an explicit set of already-open module resource planes to genuine
 * Spring resource scanning and metadata readers.
 *
 * <p>The caller owns and closes each resource plane. This resolver borrows it,
 * never creates a BeanFactory, never loads classes to select rows and never
 * changes Spring's scanner, conditions, scopes, proxies or lifecycle algorithms.
 * Class-path fallback is inherited from Spring; module selection is confined to
 * the supplied immutable revisions. No JMOD is loaded or inserted into a layer.
 *
 * <p>A custom PathMatcher is opaque. Every admitted resource name is passed to
 * it in original ModuleReader encounter order; wildcard prefix or class-suffix
 * pruning would be incorrect for configurable or application-defined matchers.
 */
public final class M3ResourcePatternResolver extends PathMatchingResourcePatternResolver {
    private final List<M3ModuleResources> modules;
    private final Backend backend;

    public M3ResourcePatternResolver(Collection<? extends M3ModuleResources> modules,
            ClassLoader classLoader, Backend backend) {
        super(classLoader);
        this.modules = List.copyOf(Objects.requireNonNull(modules, "modules"));
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    /** Explicit indexed-module query; ordinary getResources also keeps Spring's class-path fallback. */
    public Resource[] moduleResources(String locationPattern) throws IOException {
        return findAllModulePathResources(locationPattern).toArray(Resource[]::new);
    }

    @Override
    protected Set<Resource> findAllModulePathResources(String locationPattern) throws IOException {
        Objects.requireNonNull(locationPattern, "locationPattern");
        String pattern = locationPattern.startsWith("/") ? locationPattern.substring(1) : locationPattern;
        boolean isPattern = getPathMatcher().isPattern(pattern);
        Predicate<String> matches = isPattern ? path -> getPathMatcher().match(pattern, path) : pattern::equals;
        Set<Resource> result = M3Collections.linkedHashSet();
        for (M3ModuleResources module : modules) {
            // Do not classify or parse bytecode here: Spring's real MetadataReader owns that work.
            int[] selected = module.select(isPattern ? "" : pattern, M3ModuleResources.RESOURCE, 0, backend);
            for (int row : selected) {
                String name = module.nameAt(row);
                if (matches.test(name)) {
                    var uri = module.find(name);
                    if (uri.isPresent()) result.add(new IndexedResource(module, name, uri.get()));
                }
            }
        }
        return result;
    }

    /** A resource is a borrowed handle to one immutable revision, never a global class-loader cache. */
    private static final class IndexedResource extends AbstractResource {
        private final M3ModuleResources module;
        private final String name;
        private final URI uri;

        IndexedResource(M3ModuleResources module, String name, URI uri) {
            this.module = module;
            this.name = name;
            this.uri = uri;
        }

        @Override public InputStream getInputStream() throws IOException { return module.openStream(name); }
        @Override public URI getURI() { return uri; }
        @Override public URL getURL() throws IOException { return uri.toURL(); }
        @Override public String getFilename() { return name.substring(name.lastIndexOf('/') + 1); }
        @Override public String getDescription() { return "M3 module resource [" + uri + "]"; }

        @Override public Resource createRelative(String relativePath) throws IOException {
            int slash = name.lastIndexOf('/');
            String relativeName = org.springframework.util.StringUtils.cleanPath(
                    (slash < 0 ? "" : name.substring(0, slash + 1)) + relativePath);
            URI relativeUri = module.find(relativeName).orElseThrow(() -> new FileNotFoundException(relativeName));
            return new IndexedResource(module, relativeName, relativeUri);
        }

        @Override public boolean equals(Object other) {
            return this == other || other instanceof IndexedResource resource
                    && module == resource.module && name.equals(resource.name);
        }
        @Override public int hashCode() { return 31 * System.identityHashCode(module) + name.hashCode(); }
    }
}
