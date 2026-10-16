// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.file.Path;

/** Independent JNI admission oracle: an extra library may export a KMP name but is not pinned. */
public final class NativePinAdmissionChecks {
    private NativePinAdmissionChecks() { }

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("absolute test fixture library");
        System.load(Path.of(args[0]).toAbsolutePath().toString());
        SourceArtifact owner = SourceArtifact.copyOf("admission-negative", new byte[]{41});
        BytePatternSearch plan = BytePatternSearch.compile(new byte[]{42});
        try {
            plan.findAllNative(owner.whole(), 1);
        } catch (IllegalStateException refusal) {
            System.out.println("M3_NATIVE_KMP_PIN_REFUSAL_PASS");
            return;
        }
        throw new AssertionError("Unpinned foreign JNI symbol produced source evidence");
    }
}
