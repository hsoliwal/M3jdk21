/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3InstanceIndexPrecompute;
import com.m3.text.M3InstanceIndexPrecompute.InstanceRecord;
import com.m3.text.M3InstanceIndexPrecompute.InstanceType;
import com.m3.text.M3Lexicons;

import java.util.Map;
import java.util.Set;

public final class M3LexiconsTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    public static void main(String[] args) {
        InstanceRecord paris = new InstanceRecord(
                "Paris", InstanceType.PLACE, 7L, Map.of("country", "France"));
        M3InstanceIndexPrecompute index = new M3InstanceIndexPrecompute(
                "unicodex-instance-index", "v1", "sha256:fixture",
                "ascii-fold-utf8-v1", Set.of(7L), Map.of("paris", paris));
        M3Lexicons lexicons = new M3Lexicons(index);

        check(lexicons.properNames() == index);
        check(lexicons.properNameCount() == 1);
        check(lexicons.findProperName("PARIS").equals(paris));
        check(lexicons.properNamesOf(7L).equals(java.util.List.of(paris)));
        check(lexicons.properNamesOf(99L).isEmpty());
        check(lexicons.appliesTo("unicodex-instance-index", "v1",
                "sha256:fixture", "ascii-fold-utf8-v1"));
        check(!lexicons.appliesTo("unicodex-instance-index", "v2",
                "sha256:fixture", "ascii-fold-utf8-v1"));

        try {
            new M3Lexicons(null);
            throw new AssertionError("missing null rejection");
        } catch (NullPointerException expected) {
            checks++;
        }

        System.out.println("M3JDK_LEXICONS_FACADE_PASS checks=" + checks);
    }
}
