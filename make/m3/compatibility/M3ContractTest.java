/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.*;
import jdk.internal.m3.indexstring.FrozenChars;

/** Content/range comparison only; deliberately does not assert API or owner equivalence. */
public final class M3ContractTest {
    public static void main(String[] args) {
        LocalM3Arena arena = new LocalM3Arena();int checks = 0;
        for (int unit = 0; unit <= 65535; unit++) {
            char[] source = {'a', (char)unit, 'z'};
            FrozenChars established = FrozenChars.copyOf(source);
            LocalM3StringPiece proposed = arena.copyUtf16(source);
            source[1] = '!';
            if (!established.toString().equals(proposed.flatten())) throw new AssertionError(unit);
            if (established.hash32() != proposed.flatten().hashCode()) throw new AssertionError(unit);
            for (int from = 0; from <= 3; from++) for (int to = from; to <= 3; to++) {
                if (!established.subSequence(from,to).toString().equals(proposed.subSequence(from,to).flatten()))
                    throw new AssertionError(unit);
                checks++;
            }
            checks += 2;
        }
        System.out.println("M3_CONTENT_RANGE_PASS checks=" + checks + " allUtf16Units=65536 apiCompatibilityClaim=false");
    }
}
