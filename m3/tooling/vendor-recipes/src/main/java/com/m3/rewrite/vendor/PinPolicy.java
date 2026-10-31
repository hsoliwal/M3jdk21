// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.openrewrite.xml.tree.Xml;

/**
 * The decision table of {@code switch_consumers.py} ({@code repl}), as a pure function of one dependency's
 * {@code groupId}/{@code artifactId}/{@code version} text and the pom's version properties. Order of checks is the
 * oracle's: suffixed declaration, unknown module, version not inlined, native-in-binary, switch.
 */
record PinPolicy(VendoredModules modules, String suffix, Set<String> nativeInBinary, Map<String, String> defaults) {

    static final String ALREADY_SOURCE = "ALREADY_SOURCE";
    static final String SWITCHED = "SWITCHED";
    static final String KEPT_NATIVE = "KEPT_BINARY_NATIVE_LIBRARIES";
    static final String PLUGIN_UNTOUCHED = "PLUGIN_DEPENDENCY_UNTOUCHED (plugins resolve from repositories, not the reactor)";

    private static final Pattern VERSION_PROPERTY = Pattern.compile("[A-Za-z0-9._-]+\\.version");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    /** One decision: the row to record and, for {@link #SWITCHED}, the new version text. */
    record Decision(M3VendoredPinRows.Row row, Optional<String> newVersion) {
    }

    /** Defaults, then every {@code <x.version>text</x.version>} of the document in order (later wins). */
    Map<String, String> properties(Xml.Tag root) {
        Map<String, String> props = new LinkedHashMap<>(defaults);
        Stream.concat(Stream.of(root), SimpleTag.descendants(root)).filter(t -> VERSION_PROPERTY.matcher(t.getName()).matches())
                .map(SimpleTag::of).flatMap(Optional::stream)
                .forEachOrdered(s -> props.put(s.tag().getName(), s.raw()));
        return props;
    }

    /** {@code resolve(v)}: a whole {@code ${name}} resolves through the properties (else the raw text is kept). */
    static String resolve(String raw, Map<String, String> props) {
        Matcher m = PLACEHOLDER.matcher(PyText.strip(raw));
        return m.matches() ? props.getOrDefault(m.group(1), raw) : PyText.strip(raw);
    }

    Optional<Decision> decide(String pom, String group, String artifact, String raw, Map<String, String> props) {
        String effective = resolve(raw, props);
        String declared = PyText.strip(raw);
        if (declared.endsWith(suffix)) {
            return keep(pom, group, artifact, declared, declared, ALREADY_SOURCE, effective);
        }
        return modules.inlined(group, artifact).flatMap(inlined -> !inlined.contains(effective)
                ? keep(pom, group, artifact, declared, effective, "VERSION_NOT_INLINED (inlined: " + PyText.sortedJoin(inlined) + ")", effective)
                : nativeInBinary.contains(artifact)
                ? keep(pom, group, artifact, declared, effective, KEPT_NATIVE, effective)
                : Optional.of(new Decision(new M3VendoredPinRows.Row(pom, group, artifact, declared, declared + suffix, SWITCHED, effective),
                        Optional.of(declared + suffix))));
    }

    static Decision pluginRow(String pom, String artifact) {
        return new Decision(new M3VendoredPinRows.Row(pom, "", artifact, "", "", PLUGIN_UNTOUCHED, ""), Optional.empty());
    }

    private static Optional<Decision> keep(String pom, String g, String a, String declared, String shown, String action, String effective) {
        return Optional.of(new Decision(new M3VendoredPinRows.Row(pom, g, a, declared, shown, action, effective), Optional.empty()));
    }
}
