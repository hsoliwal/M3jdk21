// SPDX-License-Identifier: Apache-2.0

/**
 * Internal M3 coordinate and precompute support for {@code java.base}.
 *
 * <p>This package is an implementation detail. It is not a public Java API and must not become a
 * second owner of canonical text. Posting, regex-result, relation, formula, truth, and probability
 * images retain primitive M3 coordinates and derived facts only. Canonical spelling remains owned
 * by the M3 String owner/backing selected by the runtime.</p>
 *
 * <p>No project owns a generic domain such as language, reasoning, search, regular expressions,
 * logic, probability, parsing, or indexing. Provenance in this package is mechanism-specific:
 * for example Synexia's Lucene-backed numeric token/pair projection informed
 * {@link jdk.internal.mindex.M3PostingIndex}; Synexia's RE2/J finite-domain compiled-result lane
 * informed {@link jdk.internal.mindex.M3RegexDomainIndex}; and particular primitive
 * formula/bitplane/CSR/attack-parity implementations in Synexia informed the corresponding
 * M3JDK-internal classes. Those are implementation lineages, not ownership claims.</p>
 *
 * <p>Synexia is the donor/reference world for these target adaptations. M3JDK uses M3 target names
 * and JDK-internal packaging. Third-party object graphs and framework dependencies are not pulled
 * into {@code java.base}; the target keeps only independently expressed primitive mechanics that
 * have a concrete JDK consumer. Implementations are open-ended and may coexist, evolve, or be
 * replaced while M3 coordinate identity and JDK contracts remain stable.</p>
 */
package jdk.internal.mindex;
