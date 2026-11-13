// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.List;
import java.util.Objects;

/** Immutable, content-addressed input contract for one structured text-tool invocation. */
public record M3TextToolRequest(String tool, List<TextFrame> inputs, List<String> arguments) {
    public M3TextToolRequest {
        tool = Ids.id(tool);
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        inputs.forEach(value -> Objects.requireNonNull(value, "input"));
        arguments.forEach(value -> {
            Objects.requireNonNull(value, "argument");
            Canonical.validUnicode(value);
        });
    }

    public static M3TextToolRequest of(String tool, TextFrame input, String... arguments) {
        return new M3TextToolRequest(tool, List.of(input), List.of(arguments));
    }

    public String sha256() {
        Canonical hash = new Canonical("m3-text-tool-request-v1")
                .text(tool).number(inputs.size());
        inputs.forEach(frame -> hash.text(frame.sha256()));
        hash.number(arguments.size());
        arguments.forEach(hash::text);
        return hash.finish();
    }
}
