/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.nlpsearch.spatialchecks.features;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.io.GeoJSONReader;
import org.locationtech.spatial4j.io.jackson.ShapesAsGeoJSONModule;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

/** Checks the actual optional Noggit, JTS, and Jackson paths using fixed local data. */
public final class Spatial4jFeatureChecks {
    private static int checks;

    private Spatial4jFeatureChecks() { }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments expected");
        var context = SpatialContext.GEO;
        var geoJson = context.getFormats().getGeoJsonReader();
        check(geoJson instanceof GeoJSONReader, "original Noggit-backed GeoJSON reader selected");
        var point = (Point) geoJson.read("{\"type\":\"Point\",\"coordinates\":[12,34]}");
        check(point.getX() == 12 && point.getY() == 34, "actual GeoJSON coordinates");

        var jts = JtsSpatialContext.GEO;
        var polygon = (JtsGeometry) jts.getFormats().getWktReader().read(
                "POLYGON ((0 0, 8 0, 4 8, 0 0))");
        polygon.validate();
        check(polygon.getGeom().getArea() > 0, "actual JTS geometry has area");
        check(polygon.relate(jts.getShapeFactory().pointXY(4, 2)) == SpatialRelation.CONTAINS,
                "JTS polygon contains interior point");
        check(polygon.relate(jts.getShapeFactory().pointXY(10, 10)) == SpatialRelation.DISJOINT,
                "JTS polygon excludes exterior point");

        var mapper = new ObjectMapper().registerModule(new ShapesAsGeoJSONModule());
        String encoded = mapper.writeValueAsString(point);
        var json = mapper.readTree(encoded);
        check("Point".equals(json.path("type").asText()), "original Jackson shape serializer selected");
        check(json.path("coordinates").get(0).asDouble() == 12
                        && json.path("coordinates").get(1).asDouble() == 34,
                "Jackson serialized exact point coordinates");
        var decoded = (Point) mapper.readValue(encoded, Shape.class);
        check(decoded.getX() == 12 && decoded.getY() == 34, "original Jackson shape deserializer round trip");
        check(ObjectMapper.class.getModule().isNamed() && polygon.getGeom().getClass().getModule().isNamed(),
                "actual named Jackson and JTS owners");
        System.out.println("M3_SPATIAL4J_FEATURES_PASS checks=" + checks + " features=NOGGIT_JTS_JACKSON");
    }
}
