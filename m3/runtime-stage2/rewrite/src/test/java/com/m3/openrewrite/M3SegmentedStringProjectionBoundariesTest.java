/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class M3SegmentedStringProjectionBoundariesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("com.m3.openrewrite.M3SegmentedStringProjectionBoundaries");
    }

    @Test
    void rewritesStringValueProjection() {
        rewriteRun(
            text(
                """
                byte[] value() {
                        return value;
                    }
                """,
                """
                byte[] value() {
                        return m3Storage == null ? value : m3Storage.materialize();
                    }
                """,
                source -> source.path("src/java.base/share/classes/java/lang/String.java")
            )
        );
    }

    @Test
    void valueProjectionIsIdempotent() {
        rewriteRun(
            text(
                """
                byte[] value() {
                        return m3Storage == null ? value : m3Storage.materialize();
                    }
                """,
                source -> source.path("src/java.base/share/classes/java/lang/String.java")
            )
        );
    }

    @Test
    void rewritesPairConcatWithoutArrayAllocation() {
        rewriteRun(
            text(
                """
                if (s2.isEmpty()) {
                            // newly created string required, see JLS 15.18.1
                            return new String(s1);
                        }
                        // start "mixing" in length and coder or arguments, order is not
                """,
                """
                if (s2.isEmpty()) {
                            // newly created string required, see JLS 15.18.1
                            return new String(s1);
                        }
                        if (String.m3JoinedStringsEnabled()) {
                            String joined = String.m3Concat(s1, s2);
                            if (joined != null) {
                                return joined;
                            }
                        }
                        // start "mixing" in length and coder or arguments, order is not
                """,
                source -> source.path("src/java.base/share/classes/java/lang/StringConcatHelper.java")
            )
        );
    }

    @Test
    void rewritesComplexIndyConcatToSegmentedFallback() {
        rewriteRun(
            text(
                """
                if (paramCount == 2 && !mt.hasPrimitives() && suffix == null
                        && constants[0] == null && constants[1] == null) {
                    // Two reference arguments, no surrounding constants
                    return simpleConcat();
                }
                // else... fall-through to slow-path
                """,
                """
                if (paramCount == 2 && !mt.hasPrimitives() && suffix == null
                        && constants[0] == null && constants[1] == null) {
                    // Two reference arguments, no surrounding constants
                    return simpleConcat();
                }
                // Correctness-first segmented fallback. The existing fast paths above
                // remain allocation-minimal; complex indy shapes avoid the flat byte[]
                // builder while M3 storage is enabled. Specialized segment combinators
                // can replace this collector without changing the call-site contract.
                if (JLA.stringConcatUsesM3Storage()) {
                    return generateM3Concat(mt, constants);
                }
                // else... fall-through to stock inline-copy slow-path
                """,
                source -> source.path("src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java")
            )
        );
    }

    @Test
    void rewritesRegexSearchToUseNecessaryConditionPrefilter() {
        rewriteRun(
            text(
                """
                acceptMode = NOANCHOR;
                boolean result = parentPattern.root.match(this, from, text);
                """,
                """
                acceptMode = NOANCHOR;
                if (!parentPattern.m3MayMatch(text)) {
                    this.first = -1;
                    this.oldLast = this.last;
                    this.modCount++;
                    return false;
                }
                boolean result = parentPattern.root.match(this, from, text);
                """,
                source -> source.path("src/java.base/share/classes/java/util/regex/Matcher.java")
            )
        );
    }

    @Test
    void rewritesJniProjectionToSegmentTraversal() {
        rewriteRun(
            text(
                """
                if (s_len > 0) {
                        if (!is_latin1) {
                          ArrayAccess<>::arraycopy_to_native(s_value, (size_t) typeArrayOopDesc::element_offset<jchar>(0),
                                                             buf, s_len);
                        } else {
                          for (int i = 0; i < s_len; i++) {
                            buf[i] = ((jchar) s_value->byte_at(i)) & 0xff;
                          }
                        }
                      }
                """,
                """
                if (s_len > 0) {
                        if (java_lang_String::is_m3_joined(s)) {
                          for (int i = 0; i < s_len; i++) {
                            buf[i] = java_lang_String::char_at(s, i);
                          }
                        } else if (!is_latin1) {
                          ArrayAccess<>::arraycopy_to_native(s_value, (size_t) typeArrayOopDesc::element_offset<jchar>(0),
                                                             buf, s_len);
                        } else {
                          for (int i = 0; i < s_len; i++) {
                            buf[i] = ((jchar) s_value->byte_at(i)) & 0xff;
                          }
                        }
                      }
                """,
                source -> source.path("src/hotspot/share/prims/jni.cpp")
            )
        );
    }
}
