/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.openrewrite;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;

final class M3LiteralAdmissionLoweringRecipeTest implements RewriteTest {
    private static Path owner;
    @BeforeAll static void verifiedFixtureRequired() throws Exception {
        String configured = System.getProperty("m3.ownerClasses");
        assertNotNull(configured, "Run the pinned core build, then pass -Dm3.ownerClasses=/absolute/owner-classes");
        owner = Path.of(configured);
        assertEquals(TypedLiteralAdmissionLowering.OWNER_CLASS_ROOT, TypedLiteralAdmissionLowering.ownerClassRoot(owner));
    }
    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new M3LiteralAdmissionLoweringRecipe(owner.toString()));
        spec.parser(JavaParser.fromJavaVersion().classpath(List.of(owner)));
    }
    @Test void realCompilerDifferentialCorpus() throws Exception { TypedLiteralAdmissionLoweringTest.main(new String[]{owner.toString()}); }
    @Test void lowersExplicitConstants() {
        rewriteRun(java("""
                import com.m3.text.M3Text;
                class Example {
                    static final String A = "a", B = "b";
                    M3Text get() { return M3Text.fromString(A+B); }
                }
                """, """
                import com.m3.text.M3Text;
                class Example {
                    static final String A = "a", B = "b";
                    M3Text get() { return com.m3.text.M3Text.fromString(A).concat(com.m3.text.M3Text.fromString(B)); }
                }
                """, spec -> spec.path("Example.java")));
    }
    @Test void leavesSideEffectsAndOrdinaryStringsAlone() {
        rewriteRun(java("""
                import com.m3.text.M3Text;
                class Example {
                    String a() { return "a"; }
                    String b() { return "b"; }
                    M3Text get() { return M3Text.fromString(a()+b()); }
                    String ordinary() { return a()+b(); }
                }
                """, spec -> spec.path("Example.java")));
    }
    @Test void fixedPoint() {
        rewriteRun(java("""
                import com.m3.text.M3Text;
                class Example {
                    static final String A = "a", B = "b";
                    M3Text get() { return M3Text.fromString(A).concat(M3Text.fromString(B)); }
                }
                """, spec -> spec.path("Example.java")));
    }
}
