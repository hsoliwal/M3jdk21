// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarSource.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.net.URI;
import java.util.Objects;

/**
 * Attribution/provenance for a grammar source.
 *
 * <p>The runtime image does not require the source to be present. This metadata records where the
 * frozen rules came from and keeps donor/spec provenance separate from the compiled grammar.</p>
 */
public record M3GrammarSource(
    String name,
    URI uri,
    String revision,
    M3GrammarFormat format,
    String licenseOrTerms) {

  public M3GrammarSource {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(revision, "revision");
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(licenseOrTerms, "licenseOrTerms");
  }
}
