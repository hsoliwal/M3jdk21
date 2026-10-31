/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.nlpsearch.spatialchecks.core;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.spatial.vector.PointVectorStrategy;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;

/** Exercises the original Spatial4j core and Lucene's real spatial strategy. */
public final class Spatial4jCoreChecks {
    private Spatial4jCoreChecks() { }

    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments expected");
        for (String optional : new String[] {"noggit", "org.locationtech.jts",
                "com.fasterxml.jackson.core", "com.fasterxml.jackson.databind",
                "com.fasterxml.jackson.annotation"}) {
            check(ModuleLayer.boot().findModule(optional).isEmpty(), "optional supplier not resolved: " + optional);
        }
        var context = SpatialContext.GEO;
        var point = (Point) context.getFormats().getWktReader().read("POINT (12 34)");
        check(point.getX() == 12 && point.getY() == 34, "actual WKT coordinates");
        check("spatial4j".equals(SpatialContext.class.getModule().getName()), "named Spatial4j owner");
        check("org.apache.lucene.spatial_extras".equals(PointVectorStrategy.class.getModule().getName()),
                "named Lucene spatial owner");
        var strategy = PointVectorStrategy.newInstance(context, "fixture_position");
        var document = new Document();
        for (var field : strategy.createIndexableFields(point)) document.add(field);
        try (var directory = new ByteBuffersDirectory(); var analyzer = new StandardAnalyzer();
                var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            writer.addDocument(document);
            try (var reader = DirectoryReader.open(writer)) {
                var searcher = new IndexSearcher(reader);
                var near = context.getShapeFactory().rect(10, 15, 30, 35);
                var far = context.getShapeFactory().rect(-15, -10, -35, -30);
                check(searcher.count(strategy.makeQuery(new SpatialArgs(SpatialOperation.Intersects, near))) == 1,
                        "real spatial query finds indexed point");
                check(searcher.count(strategy.makeQuery(new SpatialArgs(SpatialOperation.Intersects, far))) == 0,
                        "real spatial query excludes distant rectangle");
            }
        }
        System.out.println("M3_SPATIAL4J_CORE_PASS checks=10 optional_resolved=0 parser=WKT query=LUCENE_POINT_VECTOR");
    }
}
