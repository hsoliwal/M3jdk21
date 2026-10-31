/*******************************************************************************
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Planted-defect tool for DECORATOR_FILL negative controls: applies every NEGATIVE_CONTROLS_SPEC.tsv row of one control
 * id (columns: id, refutation, target, old, new, ...) as an exact literal replacement (no regex) that must hit exactly one
 * site, so a control either applies as written or fails loudly. {@code \n} in a literal stands for a newline.
 * Usage: {@code java M3Mutant <spec.tsv> <id> <in.java> <out.java>}. Exit 0 = applied, 2 = a site count is not 1.
 */
public final class M3Mutant {
    private static final Logger LOG = Logger.getLogger(M3Mutant.class.getName());

    private M3Mutant() {
    }

    public static void main(String[] args) throws IOException {
        String text = Files.readString(Path.of(args[2]), StandardCharsets.UTF_8).replace("\r\n", "\n");
        List<String[]> rows = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8).stream().map(l -> l.replace("\r", "").split("\t", -1))
                .filter(r -> r[0].equals(args[1])).toList();
        for (String[] r : rows) {
            String old = r[3].replace("\\n", "\n");
            int sites = text.split(java.util.regex.Pattern.quote(old), -1).length - 1;
            LOG.info(() -> "M3_MUTANT id=" + args[1] + " sites=" + sites);
            if (sites != 1) {
                System.exit(2);
            }
            text = text.replace(old, r[4].replace("\\n", "\n"));
        }
        if (rows.isEmpty()) {
            System.exit(2);
        }
        Files.writeString(Path.of(args[3]), text, StandardCharsets.UTF_8);
    }
}
