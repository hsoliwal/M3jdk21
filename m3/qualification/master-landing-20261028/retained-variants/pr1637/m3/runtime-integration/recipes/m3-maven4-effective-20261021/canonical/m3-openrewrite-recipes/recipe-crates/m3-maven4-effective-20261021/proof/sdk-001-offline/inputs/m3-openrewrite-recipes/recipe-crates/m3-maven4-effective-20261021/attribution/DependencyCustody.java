// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package proof.dependencies;

/** Typed custody probe only; product behavior is qualified by the real providers. */
final class DependencyCustody {
    jdk.m3.parser.M3ASTIndex maven4(jdk.m3.parser.M3File source,
            org.apache.maven.api.services.ModelBuilderResult result,
            jdk.m3.parser.M3ASTIngress.Limits limits) {
        result.getEffectiveModel();
        return jdk.m3.parser.M3ASTAuthoritativeModels.maven4Effective(source, result, limits);
    }

    jdk.m3.parser.M3ASTIndex maven3(jdk.m3.parser.M3File source,
            org.apache.maven.model.building.ModelBuildingResult result,
            jdk.m3.parser.M3ASTIngress.Limits limits) {
        result.getEffectiveModel();
        return jdk.m3.parser.M3ASTAuthoritativeModels.mavenEffective(source, result, limits);
    }

    jdk.m3.parser.M3ASTIndex jdt(jdk.m3.parser.M3File source,
            org.eclipse.jdt.core.dom.CompilationUnit tree, CharSequence parsedSource,
            jdk.m3.parser.M3ASTIngress.Limits limits) {
        tree.getProblems();
        return jdk.m3.parser.M3ASTAuthoritativeModels.eclipseJdt(source, tree, parsedSource, limits);
    }
}
