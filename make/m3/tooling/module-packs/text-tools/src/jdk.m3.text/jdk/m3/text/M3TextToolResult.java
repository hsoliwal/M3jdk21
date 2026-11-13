// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Exact output contract and replay receipt for a structured text-tool invocation. */
public record M3TextToolResult(
        String tool,
        List<TextFrame> frames,
        List<List<String>> argumentBatches,
        String requestSha256,
        String catalogSha256,
        String outputSha256) {

    public M3TextToolResult {
        tool = Ids.id(tool);
        frames = List.copyOf(Objects.requireNonNull(frames, "frames"));
        frames.forEach(frame -> Objects.requireNonNull(frame, "frame"));
        Objects.requireNonNull(argumentBatches, "argumentBatches");
        List<List<String>> batches = new ArrayList<>(argumentBatches.size());
        for (List<String> batch : argumentBatches) {
            List<String> copy = List.copyOf(Objects.requireNonNull(batch, "batch"));
            copy.forEach(value -> {
                Objects.requireNonNull(value, "argument");
                Canonical.validUnicode(value);
            });
            batches.add(copy);
        }
        argumentBatches = List.copyOf(batches);
        requestSha256 = Ids.sha256(requestSha256);
        catalogSha256 = Ids.sha256(catalogSha256);
        outputSha256 = Ids.sha256(outputSha256);
        String actual = outputSha256(frames, argumentBatches);
        if (!actual.equals(outputSha256)) {
            throw new IllegalArgumentException("output receipt mismatch");
        }
    }

    static M3TextToolResult create(M3TextToolRequest request, List<TextFrame> frames,
                                   List<List<String>> argumentBatches) {
        return new M3TextToolResult(request.tool(), frames, argumentBatches,
                request.sha256(), M3TextToolCatalog.sha256(),
                outputSha256(frames, argumentBatches));
    }

    public String receiptSha256() {
        return new Canonical("m3-text-tool-result-v1")
                .text(tool).text(requestSha256).text(catalogSha256).text(outputSha256)
                .finish();
    }

    private static String outputSha256(List<TextFrame> frames,
                                       List<List<String>> argumentBatches) {
        Canonical hash = new Canonical("m3-text-tool-output-v1").number(frames.size());
        frames.forEach(frame -> hash.text(frame.sha256()));
        hash.number(argumentBatches.size());
        for (List<String> batch : argumentBatches) {
            hash.number(batch.size());
            batch.forEach(hash::text);
        }
        return hash.finish();
    }
}
