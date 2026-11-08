// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors

/**
 * javac reference-flow facts: source-local reference origins and escape evidence (M3ReferenceFlow), source and
 * flow liveness (M3SourceLiveness, M3Liveness, M3LoopFacts), the read-only alias count census over flow edges
 * (M3CompilerReferenceCensus), path-sensitive MAY/MUST local alias certainty (M3CRefPathCertainty) and the
 * compiler facts audit (M3CompilerFactsAudit). Every result is evidence only: no result authorizes reclamation,
 * reference-count elision or source mutation.
 */
//FLLDCIM: IOP - census2 P2-132 + P2-136; relocated by recipe/relocate.py from the indexstring compiler module of hsoliwal/com.synexia (PROVENANCE.tsv)
module org.m3jdk.compiler.reference {
    requires transitive java.compiler;
    requires transitive jdk.compiler;
    requires transitive jdk.m3.collections;
    exports jdk.m3.compiler.reference;
}
