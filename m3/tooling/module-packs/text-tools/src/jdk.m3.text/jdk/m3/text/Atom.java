/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.function.Consumer;

/** Host-owned computation. A candidate plan cannot instantiate implementations. */
@FunctionalInterface
public interface Atom {
  void execute(TextFrame input, Consumer<TextLine> output);
}
