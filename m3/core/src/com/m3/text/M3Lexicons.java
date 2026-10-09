/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stable M3JDK umbrella over one immutable Synexia lexicon export.
 *
 * <p>This is an adapter over {@link SharedLexiconCatalog}; it does not copy
 * records, create another interner, or take ownership of the M3 String
 * runtime. Source IDs and mapping names remain Synexia-owned identities, while
 * image coordinates and precomputed facts remain target-owned projections.</p>
 */
public final class M3Lexicons implements AutoCloseable {
    private final SharedLexiconCatalog catalog;

    private M3Lexicons(SharedLexiconCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /** Open the complete validated export with eager shard admission. */
    public static M3Lexicons open(Path exportDirectory) throws IOException {
        return of(SharedLexiconCatalog.open(exportDirectory));
    }

    /** Open the same export with the catalog's lazy shard policy. */
    public static M3Lexicons openLazy(Path exportDirectory) throws IOException {
        return of(SharedLexiconCatalog.openLazy(exportDirectory));
    }

    /** Bind the M3JDK name to one already-admitted catalog without copying it. */
    public static M3Lexicons of(SharedLexiconCatalog catalog) {
        return new M3Lexicons(catalog);
    }

    /** Return the underlying read-only catalog without creating another owner. */
    public SharedLexiconCatalog catalog() {
        return catalog;
    }

    public int shardCount() {
        return catalog.shardCount();
    }

    public long recordCount() {
        return catalog.recordCount();
    }

    public List<String> shardFiles() {
        return catalog.shardFiles();
    }

    public List<SharedLexiconCatalog.SourceMapping> mappingsAt(
            SharedLexiconCatalog.Coordinate coordinate) {
        return catalog.mappingsAt(coordinate);
    }

    public Optional<SharedLexiconCatalog.SourceMapping> findMapping(
            String sourceId, String recordId) {
        return catalog.findMapping(sourceId, recordId);
    }

    public List<SharedLexiconCatalog.SourceMapping> findMappings(String text) {
        return catalog.findMappings(text);
    }

    public Optional<SharedLexiconCatalog.Coordinate> find(String text) {
        return catalog.find(text);
    }

    public List<SharedLexiconCatalog.Coordinate> prefix(String value, int limit) {
        return catalog.prefix(value, limit);
    }

    public SharedLexiconCatalog.PrecomputeFacts precomputeAt(
            SharedLexiconCatalog.Coordinate coordinate) {
        return catalog.precomputeAt(coordinate);
    }

    public List<SharedLexiconCatalog.PrecomputeProfile> precomputeProfiles() {
        return catalog.precomputeProfiles();
    }

    public Optional<SynexiaPrecomputePayload> findPrecomputePayload(
            String sourceId, String recordId) {
        return catalog.findPrecomputePayload(sourceId, recordId);
    }

    public Optional<M3LexiconPrecompute.SiUnitPrecompute> findSiUnitPrecompute(
            String sourceId, String recordId) {
        return catalog.findSiUnitPrecompute(sourceId, recordId);
    }

    public Optional<M3LexiconPrecompute.NumberPrecompute> findNumberPrecompute(
            String sourceId, String recordId) {
        return catalog.findNumberPrecompute(sourceId, recordId);
    }

    public Optional<SharedLexiconFamilySidecarCatalog> familyPrecompute() {
        return catalog.familyPrecompute();
    }

    public Optional<SharedRelatedLexemeCatalog> relatedLexemes() {
        return catalog.relatedLexemes();
    }

    public String textAt(SharedLexiconCatalog.Coordinate coordinate) {
        return catalog.textAt(coordinate);
    }

    public void warm() {
        catalog.warm();
    }

    @Override
    public void close() {
        catalog.close();
    }
}


