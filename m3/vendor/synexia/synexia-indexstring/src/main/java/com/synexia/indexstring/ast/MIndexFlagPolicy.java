// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

/** Interpretation of the compact 56-bit flags lane for one AST rule. */
public enum MIndexFlagPolicy {
  NONE,
  MODIFIER_BITS,
  BOOLEAN,
  ENUM_ORDINAL_PLUS_ONE
}
