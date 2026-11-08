// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.companion;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read-only, target-independent admission candidates for a JDK API's optional M3 companion.
 * This is NOT a source generator, ABI proof, native binding or M3JDK 21 acceptance decision.
 * Resolution uses the running Java 21 image; the installed M3JDK 21 image must be checked again.
 */
public final class M3CompanionPlan {
    public static final String SCHEMA = "M3_COMPANION_CANDIDATE_V1";

    private M3CompanionPlan() { }

    public record Request(String moduleName, String binaryName) {
        public Request {
            Objects.requireNonNull(moduleName, "moduleName");
            Objects.requireNonNull(binaryName, "binaryName");
            if (moduleName.isBlank() || binaryName.isBlank()
                    || !moduleName.matches("[A-Za-z0-9_.]+")
                    || !binaryName.matches("[A-Za-z_$][A-Za-z0-9_$.]*")) {
                throw new IllegalArgumentException("invalid module/type identity");
            }
        }
    }

    public enum Decision {
        REVIEW, HOLD_MODULE_NOT_RESOLVED, REFUSED_MODULE, REFUSED_TYPE, REFUSED_NOT_PUBLIC,
        REFUSED_NOT_EXPORTED, REFUSED_COLLISION, REFUSED_EXISTING
    }

    public record Row(Request request, String companionName, Decision decision,
                      String cause, String imageFingerprint) {
        public Row {
            Objects.requireNonNull(request);
            Objects.requireNonNull(companionName);
            Objects.requireNonNull(decision);
            Objects.requireNonNull(cause);
            Objects.requireNonNull(imageFingerprint);
        }
    }

    /** Stable proposed public package; no java.*, javax.*, or JDK-internal export is introduced. */
    public static String companionName(String binaryName) {
        Objects.requireNonNull(binaryName, "binaryName");
        int dot = binaryName.lastIndexOf('.');
        if (dot < 1 || dot == binaryName.length() - 1) {
            throw new IllegalArgumentException("expected fully qualified binary type");
        }
        String pkg = binaryName.substring(0, dot);
        String leaf = binaryName.substring(dot + 1);
        if (!pkg.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")
                || !leaf.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\$[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            throw new IllegalArgumentException("invalid binary type");
        }
        // Human-readable nested names are not automatically injective. The global plan
        // refuses all name collisions, including clashes with reserved existing owners.
        return "jdk.m3.companion." + pkg + ".M3" + leaf.replace('$', '_') + "Ops";
    }

    /**
     * Deterministic regardless of request order or duplicate entries. Missing/inaccessible
     * API surfaces receive explicit refusals instead of speculative classes.
     */
    public static List<Row> inspect(Collection<Request> requests, Set<String> reservedCompanions) {
        Objects.requireNonNull(requests, "requests");
        Objects.requireNonNull(reservedCompanions, "reservedCompanions");
        Set<String> reserved = Set.copyOf(reservedCompanions);
        TreeSet<Request> unique = new TreeSet<>(Comparator.comparing(Request::moduleName)
                .thenComparing(Request::binaryName));
        unique.addAll(requests);
        List<Row> proposed = new ArrayList<>();
        Map<String, Integer> counts = new HashMap<>();
        for (Request request : unique) {
            Row row = evaluate(request);
            proposed.add(row);
            if (row.decision() == Decision.REVIEW) {
                counts.merge(row.companionName(), 1, Integer::sum);
            }
        }
        List<Row> checked = new ArrayList<>();
        for (Row row : proposed) {
            if (row.decision() != Decision.REVIEW) {
                checked.add(row);
            } else if (counts.get(row.companionName()) != 1) {
                checked.add(withDecision(row, Decision.REFUSED_COLLISION, "name-collision"));
            } else if (reserved.contains(row.companionName())) {
                checked.add(withDecision(row, Decision.REFUSED_EXISTING, "existing-owner-review"));
            } else {
                checked.add(row);
            }
        }
        return List.copyOf(checked);
    }

    private static Row withDecision(Row row, Decision decision, String cause) {
        return new Row(row.request(), row.companionName(), decision, cause, row.imageFingerprint());
    }

    private static Row evaluate(Request request) {
        var maybeModule = ModuleLayer.boot().findModule(request.moduleName());
        if (maybeModule.isEmpty()) {
            // Installed modules need not be resolved in the boot layer. Preserve
            // their candidates as HOLD without defining a new layer or classes.
            if (ModuleFinder.ofSystem().find(request.moduleName()).isPresent()) {
                return refused(request, Decision.HOLD_MODULE_NOT_RESOLVED,
                        "system-module-not-resolved-in-boot-layer");
            }
            return refused(request, Decision.REFUSED_MODULE, "module-not-installed");
        }
        Module module = maybeModule.get();
        Class<?> type;
        try {
            type = Class.forName(module, request.binaryName());
        } catch (LinkageError | SecurityException failure) {
            return refused(request, Decision.REFUSED_TYPE, "type-unresolvable");
        }
        if (type == null || type.getModule() != module || !type.getName().equals(request.binaryName())) {
            return refused(request, Decision.REFUSED_TYPE, "type-not-in-declared-module");
        }
        if (!Modifier.isPublic(type.getModifiers()) || type.isSynthetic()
                || type.isAnonymousClass() || type.isLocalClass() || !publicEnclosure(type)) {
            return refused(request, Decision.REFUSED_NOT_PUBLIC, "not-public-api-type");
        }
        if (!module.isExported(type.getPackageName())) {
            return refused(request, Decision.REFUSED_NOT_EXPORTED, "not-unconditionally-exported");
        }
        String companion = companionName(type.getName());
        ModuleDescriptor descriptor = module.getDescriptor();
        String fingerprint = sha256(System.getProperty("java.runtime.version", "unknown") + "\0"
                + descriptor.toNameAndVersion() + "\0" + request.binaryName());
        return new Row(request, companion, Decision.REVIEW, "new-capability-proof-required", fingerprint);
    }

    private static boolean publicEnclosure(Class<?> type) {
        for (Class<?> parent = type.getDeclaringClass(); parent != null;
             parent = parent.getDeclaringClass()) {
            if (!Modifier.isPublic(parent.getModifiers())) {
                return false;
            }
        }
        return true;
    }

    private static Row refused(Request request, Decision decision, String cause) {
        // No SHA of source bytes is invented for a refusal; this is not a source pin.
        return new Row(request, "", decision, cause, "");
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }

    /** Read-only CLI: module:binaryName arguments -> deterministic tabular review queue. */
    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("provide module:binaryName API requests");
        }
        List<Request> requested = new ArrayList<>();
        for (String arg : args) {
            int separator = arg.indexOf(':');
            if (separator < 1 || separator == arg.length() - 1) {
                throw new IllegalArgumentException("expected module:binaryName");
            }
            requested.add(new Request(arg.substring(0, separator), arg.substring(separator + 1)));
        }
        System.out.println("schema\tmodule\tjava_api\tcompanion_candidate\tdecision\tcause\thost_identity_sha256");
        for (Row row : inspect(requested, Set.of())) {
            System.out.println(String.join("\t", SCHEMA, row.request().moduleName(),
                    row.request().binaryName(), row.companionName(), row.decision().name(),
                    row.cause(), row.imageFingerprint()));
        }
    }
}
