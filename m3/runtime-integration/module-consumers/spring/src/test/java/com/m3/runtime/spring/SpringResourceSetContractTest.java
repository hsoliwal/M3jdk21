/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.spring;

import com.m3.collections.M3Collections;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import static org.junit.jupiter.api.Assertions.*;

/** Actual values produced by the unchanged private Spring conversion helper. */
class SpringResourceSetContractTest {
    @Test
    void actualSpringFileAndUrlResourcesRetainOrderDedupAndSetBehavior() {
        List<Resource> values = List.of(
                new FileSystemResource("/fixture/one.class"),
                UrlResource.from(URI.create("jar:file:/fixture.jar!/two.class")),
                new FileSystemResource("/fixture/one.class"),
                UrlResource.from(URI.create("jrt:/fixture/three.class")),
                UrlResource.from(URI.create("jar:file:/fixture.jar!/two.class")));
        var donor = new LinkedHashSet<Resource>(64);
        var receiving = M3Collections.<Resource>linkedHashSet();
        for (Resource value : values) assertEquals(donor.add(value), receiving.add(value));
        assertEquals(new ArrayList<>(donor), new ArrayList<>(receiving));
        assertEquals(donor, receiving);
        assertEquals(donor.hashCode(), receiving.hashCode());
        assertEquals(donor.toString(), receiving.toString());
        assertArrayEquals(donor.toArray(), receiving.toArray());
        for (Resource value : values) assertEquals(donor.contains(value), receiving.contains(value));
        for (Resource value : values) assertEquals(donor.remove(value), receiving.remove(value));
        assertTrue(receiving.isEmpty());
    }
}
