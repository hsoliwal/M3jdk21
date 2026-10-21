// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarFamily.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

/** Broad language families sharing the same frozen grammar mechanics. */
public enum M3GrammarFamily {
  PROGRAMMING,
  SQL,
  NOSQL_EXPRESSION,
  JSON_QUERY,
  DATA_FORMAT,
  LOGIC,
  FREE_LANGUAGE,
  CUSTOM
}
