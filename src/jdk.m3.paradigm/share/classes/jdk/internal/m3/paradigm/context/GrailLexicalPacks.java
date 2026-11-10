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

import java.util.List;
import java.util.Map;

/**
 * Typed, deterministic Java form of the supplied ContextGrail packs; no runtime Python or folder scanning.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/grail/GrailLexicalPacks.java}
 * blob {@code 5bda2fdfd2490bc05175305d4c6231b2de81f8aa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class GrailLexicalPacks {
    private GrailLexicalPacks() {}
    public static LexicalEngine.Pack named(String id) {
        return switch (id) {
            case "common" -> new LexicalEngine.Pack(id,
                    Map.ofEntries(Map.entry("rainy", "Rain"),
                        Map.entry("raining", "Rain"),
                        Map.entry("showers", "Rain"),
                        Map.entry("stormy", "Storm"),
                        Map.entry("noon", "Day"),
                        Map.entry("nighttime", "Night"),
                        Map.entry("sunrise", "Morning"),
                        Map.entry("sunset", "Evening"),
                        Map.entry("phone", "Mobile"),
                        Map.entry("handset", "Mobile"),
                        Map.entry("browser", "Web"),
                        Map.entry("india", "IN"),
                        Map.entry("usa", "US"),
                        Map.entry("europe", "EU"),
                        Map.entry("premium", "Gold"),
                        Map.entry("vip", "Gold"),
                        Map.entry("doctor", "Doctor"),
                        Map.entry("physician", "Doctor"),
                        Map.entry("nurse", "Nurse"),
                        Map.entry("patient", "Patient"),
                        Map.entry("citizen", "Citizen"),
                        Map.entry("clerk", "Clerk"),
                        Map.entry("officer", "Officer"),
                        Map.entry("buy", "Purchase"),
                        Map.entry("purchase", "Purchase")),
                    Map.ofEntries(Map.entry("elderly", ContextGuard.of(ContextGuard.between("Age", new java.math.BigDecimal("60"), new java.math.BigDecimal("200"), true))),
                        Map.entry("senior citizen", ContextGuard.of(ContextGuard.between("Age", new java.math.BigDecimal("60"), new java.math.BigDecimal("200"), true))),
                        Map.entry("rush hour", ContextGuard.of(ContextGuard.anyOf("TimeOfDay", "Morning", "Evening"))),
                        Map.entry("peak hours", ContextGuard.of(ContextGuard.anyOf("TimeOfDay", "Morning", "Evening"))),
                        Map.entry("festival season", ContextGuard.of(ContextGuard.anyOf("Season", "Diwali", "Holi", "Eid", "Christmas")))));
            case "domain_banking" -> new LexicalEngine.Pack(id,
                    Map.ofEntries(Map.entry("kyc", "KYC"),
                        Map.entry("aadhar", "Aadhaar"),
                        Map.entry("upi", "UPI")),
                    Map.ofEntries(Map.entry("high risk", ContextGuard.of(ContextGuard.eq("RiskTier", "L3")))));
            case "domain_hospital" -> new LexicalEngine.Pack(id,
                    Map.ofEntries(Map.entry("er", "Emergency"),
                        Map.entry("opd", "OPD")),
                    Map.ofEntries(Map.entry("night shift", ContextGuard.of(ContextGuard.eq("Shift", "Night")))));
            case "domain_traffic" -> new LexicalEngine.Pack(id,
                    Map.ofEntries(Map.entry("expressway", "Highway")),
                    Map.ofEntries(Map.entry("school zone", ContextGuard.of(ContextGuard.eq("RoadType", "SchoolZone")))));
            case "locale_en_IN" -> new LexicalEngine.Pack(id,
                    Map.ofEntries(Map.entry("petrol", "Gasoline"),
                        Map.entry("flat", "Apartment"),
                        Map.entry("lift", "Elevator")),
                    Map.ofEntries(Map.entry("bank working hours", ContextGuard.of(ContextGuard.anyOf("TimeOfDay", "Morning", "Day")))));
            case "locale_en_US" -> new LexicalEngine.Pack(id,
                    Map.ofEntries(Map.entry("gas", "Gasoline"),
                        Map.entry("apartment", "Apartment"),
                        Map.entry("elevator", "Elevator")),
                    Map.ofEntries());
            default -> throw new IllegalArgumentException("Unknown lexical pack: " + id);
        };
    }
    public static Map<String, List<String>> synsets() {
        return Map.ofEntries(
                Map.entry("automobile", List.of("car", "vehicle", "auto")),
                Map.entry("workplace", List.of("office", "plant", "factory")),
                Map.entry("home", List.of("house", "residence", "apartment")),
                Map.entry("motivate", List.of("drive", "energize", "inspire"))
        );
    }
    public static LexicalEngine engine(String localePack, String domainPack) {
        return new LexicalEngine(List.of(named("common"), named(localePack), named(domainPack)), synsets(), LexicalEngine.defaultSenses());
    }
}
