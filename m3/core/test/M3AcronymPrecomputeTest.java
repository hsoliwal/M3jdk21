/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3AcronymPrecompute;

import java.util.List;
import java.util.Optional;

public final class M3AcronymPrecomputeTest {
    private static int checks;

    public static void main(String[] args) {
        M3AcronymPrecompute precompute = new M3AcronymPrecompute(
                "dictlang.acronyms", "synexia-r1", "sha256:acronyms",
                List.of(
                        new M3AcronymPrecompute.Entry(
                                "API", "application programming interface", "computing"),
                        new M3AcronymPrecompute.Entry(
                                "JDK", "java development kit", "java"),
                        new M3AcronymPrecompute.Entry(
                                "UTF", "unicode transformation format", null)));

        check(precompute.size() == 3);
        check(precompute.domainCount() == 2);
        check(precompute.appliesTo("dictlang.acronyms", "synexia-r1", "sha256:acronyms"));
        check(!precompute.appliesTo("dictlang.acronyms", "synexia-r2", "sha256:acronyms"));
        check(precompute.findByAcronym("API").orElseThrow().expansion()
                .equals("application programming interface"));
        check(precompute.findByAcronym("api").isEmpty());
        check(precompute.findByExpansion("APPLICATION PROGRAMMING INTERFACE")
                .orElseThrow().acronym().equals("API"));
        check(precompute.findByExpansion("missing").isEmpty());
        check(precompute.acronymsInDomain("computing").equals(List.of("API")));
        check(precompute.acronymsInDomain("missing").isEmpty());

        M3AcronymPrecompute.Entry api = precompute.findByAcronym("API").orElseThrow();
        check(api.domain().equals(Optional.of("computing")));
        check(api.acronymUtf16Length() == 3);
        check(api.expansionUtf16Length() == 35);
        check(api.acronymJavaHash() == javaHash("API"));
        check(api.expansionJavaHash() == javaHash("application programming interface"));
        check(precompute.entries().get(0) == api);

        expect(IllegalArgumentException.class, () -> new M3AcronymPrecompute.Entry(
                "api", "lowercase acronym", "computing"));
        expect(IllegalArgumentException.class, () -> new M3AcronymPrecompute.Entry(
                "BAD SPACE", "expansion", "computing"));
        expect(IllegalArgumentException.class, () -> new M3AcronymPrecompute.Entry(
                "BAD", "expansion", "bad domain"));
        expect(IllegalArgumentException.class, () -> new M3AcronymPrecompute(
                "dictlang.acronyms", "r1", "fp",
                List.of(new M3AcronymPrecompute.Entry("API", "one", "computing"),
                        new M3AcronymPrecompute.Entry("API", "two", "computing"))));

        System.out.println("M3JDK_ACRONYM_PRECOMPUTE_PASS checks=" + checks);
    }

    private static int javaHash(String value) {
        int hash = 0;
        for (int index = 0; index < value.length(); index++) {
            hash = 31 * hash + value.charAt(index);
        }
        return hash;
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
}
