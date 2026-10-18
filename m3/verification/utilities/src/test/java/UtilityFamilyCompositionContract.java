/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary Compose the existing Strings, Arrays and Collections owners with new utilities
 * @run main UtilityFamilyCompositionContract
 */

import java.io.ByteArrayInputStream;
import java.io.IOUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Streams;
import java.util.Strings;

public final class UtilityFamilyCompositionContract {
    public static void main(String[] args) throws Exception {
        byte[] encoded = "  beta  \nalpha\n \nbeta\n".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(encoded);
        String decoded = IOUtils.toString(input, StandardCharsets.UTF_8);
        List<String> words = Streams.stream(Arrays.asList(decoded.split("\n")))
                .map(Strings::stripToEmpty).filter(Strings::hasText).sorted().toList();
        if (!Objects.equals(words, List.of("alpha", "beta", "beta"))) {
            throw new AssertionError("stable utility family composition: " + words);
        }
        ArrayList<String> owner = new ArrayList<>(words);
        List<String> view = Collections.unmodifiableList(owner);
        owner.set(0, "changed");
        if (!view.get(0).equals("changed") || !words.get(0).equals("alpha")) {
            throw new AssertionError("existing live view and stream snapshot contracts");
        }
        try {
            view.add("forbidden");
            throw new AssertionError("unmodifiable view admitted a write");
        } catch (UnsupportedOperationException expected) {
            // Existing Collections owner retains its own mutation policy.
        }
        if (!Strings.nullToEmpty(null).isEmpty() || input.read() != -1) {
            throw new AssertionError("borrowed input and existing null policy");
        }
        System.out.println("UTILITY_FAMILY_COMPOSITION_PASS checks=4");
    }
}
