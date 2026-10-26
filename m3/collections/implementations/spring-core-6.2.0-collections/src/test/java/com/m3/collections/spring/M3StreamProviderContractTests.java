/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Provider contract oracle for adapters over Spring's mutable multi-value views. */
class M3StreamProviderContractTests {
    @Test
    void liveMultiValueViewsAndCaseInsensitiveKeysRemainProviderOwned() {
        LinkedMultiValueMap<String, String> multi = new LinkedMultiValueMap<>();
        multi.add("X-Trace", "one");
        multi.add("X-Trace", "one");
        List<String> live = multi.get("X-Trace");
        List<String> detached = new ArrayList<>(live);

        multi.add("X-Trace", "two");

        assertEquals(List.of("one", "one"), detached);
        assertEquals(List.of("one", "one", "two"), live);
        assertNotSame(live, detached);

        LinkedCaseInsensitiveMap<String> headers = new LinkedCaseInsensitiveMap<>(Locale.ROOT);
        headers.put("X-Trace", "present");
        assertEquals("present", headers.get("x-trace"));
    }
}
