// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.compiletime.M3RegexRequirement.Kind;
import com.m3.compiletime.M3RegexRequirement.Req;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class M3RegexRequirementTest {
    private static M3RegexRequirement.Analysis a(String regex, int flags) {
        return M3RegexRequirement.analyze(regex, flags);
    }

    @Test
    void pureLiteralsComeFromTheStockSliceOnly() {
        assertEquals(Kind.PURE_LITERAL, a("hello world", 0).kind());
        assertEquals(Req.lit("a.b"), a("a\\.b", 0).requirement());
        assertEquals(Req.lit("x*y"), a("x*y", Pattern.LITERAL).requirement());
        assertEquals(Req.lit("ab"), a("a b", Pattern.COMMENTS).requirement());
        assertEquals(Kind.REQUIRED, a("😀😁", 0).kind(), "surrogates never decide completely");
        assertEquals(Kind.UNCONSTRAINED, a("abc", Pattern.CASE_INSENSITIVE).kind());
        assertEquals(Kind.UNCONSTRAINED, a("", 0).kind());
    }

    @Test
    void walkerFollowsTheConservativeRules() {
        assertEquals(Req.and(List.of(Req.lit("foo"), Req.lit("bar"))), a("foo\\d+bar", 0).requirement());
        assertEquals(Req.or(List.of(Req.lit("cat"), Req.lit("dog"))), a("cat|dog", 0).requirement());
        assertEquals(Req.ALL, a("cat|dog|", 0).requirement());
        assertEquals(Req.lit("xy"), a("(?:abc)?xy", 0).requirement());
        assertEquals(Req.lit("abc"), a("(abc)+", 0).requirement());
        assertEquals(Req.lit("ab"), a("(?<=xyz)ab", 0).requirement());
        assertEquals(Kind.REQUIRED, a("^key=(\\w+)$", Pattern.MULTILINE).kind());
        assertFalse(a("^key=(\\w+)$", Pattern.MULTILINE).matcherStateAdmitted());
        assertTrue(a("key=\\w+", 0).matcherStateAdmitted());
    }

    @Test
    void refusalsAndInvalidPatternsStayWithTheEngine() {
        assertEquals(Kind.INVALID, a("(unclosed", 0).kind());
        assertEquals(Kind.REFUSED, a("abc", Pattern.CANON_EQ).kind());
    }

    @Test
    void splitFastpathMirrorsStringSplit() {
        assertTrue(M3SiteRecorder.splitFastpath(","));
        assertTrue(M3SiteRecorder.splitFastpath("\\."));
        assertFalse(M3SiteRecorder.splitFastpath("."));
        assertFalse(M3SiteRecorder.splitFastpath("\\d"));
        assertFalse(M3SiteRecorder.splitFastpath(", "));
    }
}
