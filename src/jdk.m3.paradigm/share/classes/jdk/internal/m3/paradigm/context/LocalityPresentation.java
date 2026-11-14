/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Local presentation, capability and silence contracts.
 *
 * <p>Presentation is deliberately downstream of locality. Theme, screenshot,
 * color, layout and affordance choices never certify identity, motive,
 * relationship state, or historical-person interior state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/LocalityPresentation.java}
 * blob {@code 8a13664cfb6a42b2f47bf0423af912ff36a47100} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LocalityPresentation {
    public enum Visibility {
        PRIVATE,
        PUBLIC
    }

    public record ThemeReceipt(
            String locality,
            String theme,
            Map<String, String> cues,
            boolean selectedByLocality,
            RepresentationAudit.RenderedAffordanceReceipt audit,
            boolean identityInferred,
            boolean motiveInferred,
            boolean relationInferred,
            boolean semanticVerdict) {
        public ThemeReceipt {
            cues = Collections.unmodifiableMap(new TreeMap<>(cues == null ? Map.of() : cues));
        }
    }

    public record CapabilityReceipt(
            String locality,
            String capability,
            boolean available,
            boolean invoked,
            boolean obligation,
            boolean permissionIsObligation,
            boolean rootAuthority,
            boolean semanticVerdict) {
    }

    public record SilenceReceipt(
            String locality,
            boolean connected,
            boolean responseRequired,
            boolean rejectionInferred,
            boolean disconnectInferred,
            boolean semanticVerdict) {
    }

    public record VisibilityReceipt(
            String locality,
            Visibility visibility,
            boolean globalVisibility,
            boolean hiddenStateMerge,
            boolean crossLocalityReadAuthority,
            boolean semanticVerdict) {
    }

    private LocalityPresentation() {
    }

    public static ThemeReceipt theme(
            String locality,
            String theme,
            Map<String, String> cues,
            boolean selectedByLocality) {
        String loc = require(locality, "locality");
        String name = require(theme, "theme");
        RepresentationAudit.RenderedAffordanceReceipt audit =
                RepresentationAudit.auditRenderedAffordance(
                        loc,
                        name,
                        selectedByLocality,
                        false,
                        false,
                        false,
                        false);
        return new ThemeReceipt(
                loc,
                name,
                cues,
                selectedByLocality,
                audit,
                false,
                false,
                false,
                false);
    }

    public static CapabilityReceipt capability(
            String locality, String capability, boolean available, boolean invoked) {
        String loc = require(locality, "locality");
        String name = require(capability, "capability");
        if (invoked && !available) {
            throw new IllegalStateException("CAPABILITY_NOT_AVAILABLE");
        }
        return new CapabilityReceipt(
                loc,
                name,
                available,
                invoked,
                false,
                false,
                false,
                false);
    }

    public static SilenceReceipt silence(String locality, boolean connected) {
        return new SilenceReceipt(
                require(locality, "locality"),
                connected,
                false,
                false,
                false,
                false);
    }

    public static VisibilityReceipt visibility(String locality, Visibility visibility) {
        if (visibility == null) {
            throw new IllegalArgumentException("visibility");
        }
        return new VisibilityReceipt(
                require(locality, "locality"),
                visibility,
                visibility == Visibility.PUBLIC,
                false,
                false,
                false);
    }

    private static String require(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field);
        }
        return normalized;
    }
}
