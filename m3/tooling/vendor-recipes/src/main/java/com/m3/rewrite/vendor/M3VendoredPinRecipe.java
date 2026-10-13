// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/**
 * Points consumer poms at the source-built coordinate of every vendored module: an XML-level port of
 * {@code switch_consumers.py} (C6 DEPENDENCY-SOURCE-INLINE, phase COMPOSE). Contract, per {@code <dependency>}
 * in document order:
 * <ul>
 *   <li>under a {@code <plugin>}: untouched, row {@code PLUGIN_DEPENDENCY_UNTOUCHED} when it has an artifactId;</li>
 *   <li>declared version already ending in the suffix: untouched, row {@code ALREADY_SOURCE};</li>
 *   <li>group:artifact not a {@code kind=MODULE} row of the pinned modules table: untouched, no row;</li>
 *   <li>effective version (literal, or a whole {@code ${p}} resolved from the pom's {@code <*.version>} tags over
 *       the default properties) not an inlined version: untouched, row {@code VERSION_NOT_INLINED (inlined: ...)};</li>
 *   <li>artifact in {@code nativeInBinary}: untouched, row {@code KEPT_BINARY_NATIVE_LIBRARIES};</li>
 *   <li>otherwise the version text becomes {@code strip(declared) + suffix} (a property reference stays a reference:
 *       {@code ${p}} becomes {@code ${p}-suffix}), row {@code SWITCHED}.</li>
 * </ul>
 * The modules table is hash-pinned (drifted table: refused). The result is a fixed point: every switched version
 * ends in the suffix, so a second run changes nothing. Rows go to the {@link M3VendoredPinRows} data table.
 */
public final class M3VendoredPinRecipe extends Recipe {

    @Option(displayName = "Modules table", description = "Path of THIRD_PARTY_MODULES.tsv (columns kind, group, artifact, version).",
            example = "M3_THIRD_PARTY_MODULES.tsv")
    private final String modules;

    @Option(displayName = "Modules table SHA-256", description = "Pin of the modules table bytes; a different table is refused.",
            example = "0f1e...")
    private final String modulesSha256;

    @Option(displayName = "Suffix", description = "Source-build version suffix carried by every module version.",
            example = "-m3-source-1")
    private final String suffix;

    @Option(displayName = "Native-in-binary artifacts", description = "artifactIds whose natives live in the binary jar; never switched.",
            example = "jna", required = false)
    private final List<String> nativeInBinary;

    @Option(displayName = "Default properties", description = "k=v,... used when a pom inherits a version property.",
            example = "rewrite.version=8.17.1", required = false)
    private final String defaultProps;

    private final transient M3VendoredPinRows rows = new M3VendoredPinRows(this);

    @JsonCreator
    public M3VendoredPinRecipe(@JsonProperty("modules") String modules, @JsonProperty("modulesSha256") String modulesSha256,
                               @JsonProperty("suffix") String suffix, @JsonProperty("nativeInBinary") List<String> nativeInBinary,
                               @JsonProperty("defaultProps") String defaultProps) {
        this.modules = modules;
        this.modulesSha256 = modulesSha256;
        this.suffix = suffix;
        this.nativeInBinary = nativeInBinary;
        this.defaultProps = defaultProps;
    }

    @Override
    public String getDisplayName() {
        return "Pin consumers to vendored source-built modules";
    }

    @Override
    public String getDescription() {
        return "Suffixes the version of every consumer dependency on an inlined vendored module, exactly as switch_consumers.py does.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VendoredModules table = VendoredModules.load(Path.of(modules), modulesSha256, suffix);
        Set<String> natives = Set.copyOf(nativeInBinary == null ? List.of() : nativeInBinary);
        return new M3VendoredPinVisitor(new PinPolicy(table, suffix, natives, PyText.defaults(defaultProps == null ? "" : defaultProps)), rows);
    }

    public M3VendoredPinRows getRows() {
        return rows;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof M3VendoredPinRecipe r && Objects.equals(modules, r.modules) && Objects.equals(modulesSha256, r.modulesSha256)
                && Objects.equals(suffix, r.suffix) && Objects.equals(nativeInBinary, r.nativeInBinary) && Objects.equals(defaultProps, r.defaultProps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modules, modulesSha256, suffix, nativeInBinary, defaultProps);
    }
}
