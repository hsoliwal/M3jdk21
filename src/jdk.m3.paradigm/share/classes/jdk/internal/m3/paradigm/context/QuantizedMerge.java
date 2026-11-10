/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * BASE/CURRENT/INCOMING value reconciliation. Conflict means no candidate, never an invented average.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/quantized/QuantizedMerge.java}
 * blob {@code 046a70b1b47647ae0464d14003ca17493271efef} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class QuantizedMerge {
    private QuantizedMerge() { }

    public static Result merge(QuantizedSchema schema, QuantizedContextValues base,
            QuantizedContextValues current, QuantizedContextValues incoming) {
        Objects.requireNonNull(schema, "schema").validate(base);
        schema.validate(current);
        schema.validate(incoming);
        int[] conflicts = new int[base.size()];
        byte[] combined = new byte[base.size()];
        int count = 0;
        for (int i = 0; i < base.size(); i++) {
            int b = base.strengthU8(i), c = current.strengthU8(i), n = incoming.strengthU8(i);
            if (c == n || n == b) combined[i] = (byte) c;
            else if (c == b) combined[i] = (byte) n;
            else conflicts[count++] = i;
        }
        // A Boolean bit has no third value: two changes away from the same base agree.
        long changed = (base.flags() ^ current.flags()) | (base.flags() ^ incoming.flags());
        QuantizedContextValues value = count == 0
                ? QuantizedContextValues.copyOf(base.flags() ^ changed, combined) : null;
        return new Result(schema, base, current, incoming, value, Arrays.copyOf(conflicts, count));
    }

    /** Retains all originals. This payload may be carried by the existing CompactContext&lt;Result>. */
    public static final class Result {
        private final QuantizedContextValues base;
        private final QuantizedContextValues current;
        private final QuantizedContextValues incoming;
        private final QuantizedContextValues merged;
        private final int[] conflicts;
        private final String root;

        private Result(QuantizedSchema schema, QuantizedContextValues base, QuantizedContextValues current,
                QuantizedContextValues incoming, QuantizedContextValues merged, int[] conflicts) {
            this.base = base;
            this.current = current;
            this.incoming = incoming;
            this.merged = merged;
            this.conflicts = conflicts;
            // The roots have a fixed 64-character alphabet; input roles are intentionally ordered.
            String canonical = "QMERGE1\n" + schema.root() + "\n" + QuantizedCodec.root(schema, base) + "\n"
                    + QuantizedCodec.root(schema, current) + "\n" + QuantizedCodec.root(schema, incoming);
            root = HexFormat.of().formatHex(QuantizedCodec.sha256().digest(
                    canonical.getBytes(StandardCharsets.US_ASCII)));
        }

        public boolean clean() { return conflicts.length == 0; }
        public Optional<QuantizedContextValues> merged() { return Optional.ofNullable(merged); }
        public QuantizedContextValues base() { return base; }
        public QuantizedContextValues current() { return current; }
        public QuantizedContextValues incoming() { return incoming; }
        public int[] conflictSlots() { return conflicts.clone(); }
        public String root() { return root; }
        public boolean promotionAuthorized() { return false; }
        public boolean semanticEquivalenceProven() { return false; }
    }
}
