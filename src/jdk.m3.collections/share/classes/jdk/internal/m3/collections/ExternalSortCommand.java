/* *****************************************************************************
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
package jdk.internal.m3.collections;

import java.nio.file.Path;

/**
 * Explicit-resource CLI for big-endian long-key/long-payload input. No implicit overwrite.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/ExternalSortCommand.java}
 * blob {@code ca785e91889b61f0f9fe4353b18ce032051f662c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ExternalSortCommand {
    private ExternalSortCommand() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 11 || (!args[0].equals("sort") && !args[0].equals("resume")))
            throw new IllegalArgumentException("sort|resume INPUT WORKSPACE RUN_RECORDS FAN_IN BUFFER_BYTES MAX_RECORDS MAX_RUN_BYTES MAX_ARRAY_BYTES signed|unsigned HEAP|TOURNAMENT|LOSER_TREE");
        if (!args[9].equals("signed") && !args[9].equals("unsigned")) throw new IllegalArgumentException("ordering");
        var options = new ExternalSortOptions(Integer.parseInt(args[3]), Integer.parseInt(args[4]),
                Integer.parseInt(args[5]), Long.parseLong(args[6]), Long.parseLong(args[7]), Long.parseLong(args[8]),
                args[9].equals("unsigned"), ExternalSortOptions.Selector.valueOf(args[10]));
        Path input = Path.of(args[1]).toAbsolutePath().normalize();
        Path workspace = Path.of(args[2]).toAbsolutePath().normalize();
        var result = args[0].equals("sort") ? ExternalLongPairSort.sort(input, workspace, options, () -> false)
                : ExternalLongPairSort.resume(input, workspace, options, () -> false);
        System.out.println("records=" + result.records());
        System.out.println("sha256=" + result.sha256());
        System.out.println("mergeRounds=" + result.mergeRounds());
        System.out.println("runBytes=" + result.runBytes());
        System.out.println("run=" + result.run());
    }
}
