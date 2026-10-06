// SPDX-License-Identifier: Apache-2.0

/**
 * Internal M3 coordinate, search, and precompute support for {@code java.base}.
 *
 * <p>This package is an implementation detail. It is not a public Java API and must not become a
 * second owner of canonical text. Search, regex, posting, graph, formula, and probability images
 * retain primitive coordinates and derived facts only. Canonical spelling remains owned by the M3
 * String owner/backing selected by the runtime.</p>
 *
 * <p>Synexia is the donor/reference world. M3JDK uses M3 target names and JDK-internal packaging;
 * donor identifiers are retained only in provenance and mapping documents. Third-party projects
 * such as Lucene, RE2/J, and TweetyProject are architecture/reference donors only for the internal
 * projections documented by each class; {@code java.base} has no runtime dependency on them.</p>
 */
package jdk.internal.mindex;
