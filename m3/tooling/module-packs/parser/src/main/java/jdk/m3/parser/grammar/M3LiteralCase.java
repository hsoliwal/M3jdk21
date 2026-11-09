// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexLiteralCase.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

/** Case policy for one literal terminal in a frozen grammar image. */
public enum M3LiteralCase {
  PROFILE,
  SENSITIVE,
  INSENSITIVE
}
