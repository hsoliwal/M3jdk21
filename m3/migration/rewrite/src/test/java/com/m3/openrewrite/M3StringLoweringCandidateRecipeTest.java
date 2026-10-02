/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.openrewrite;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class M3StringLoweringCandidateRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new M3StringLoweringCandidateRecipe());
    }

    @Test
    void marksTypedStructuralOperationsWithoutReplacingThem() {
        rewriteRun(
                java(
                        """
                        class Demo {
                          String f(String a, String b) {
                            return a.concat(b).substring(1).repeat(2);
                          }
                        }
                        """,
                        spec -> spec.afterRecipe(cu -> {
                            String printed = cu.printAll();
                            if (!printed.contains("a.concat(b)")
                                    || !printed.contains("substring(1)")
                                    || !printed.contains("repeat(2)")) {
                                throw new AssertionError("candidate inventory removed structural calls");
                            }
                        })));
    }

    @Test
    void leavesUnrelatedTypesAlone() {
        rewriteRun(
                java(
                        """
                        class Demo {
                          static final class X { X concat(X x) { return this; } }
                          X f(X a, X b) { return a.concat(b); }
                        }
                        """));
    }
}
