// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic fan-in from compatibility queue to the A3 FILE preparation frontier.
 *
 * <p>This class creates evidence only under {@code m3/build/**}. It does not write product source
 * and does not convert FILE preparation into broader-scope promotion authority.</p>
 */
public final class A3Complete {
    public static final String SCHEMA = "M3_A3_BACKPORT_COMPLETION_FANIN_V1";

    public record Result(
            int workRows,
            int javaPreparationPaths,
            int broaderScopeHolds,
            String workRoot,
            String javaPathsRoot,
            String holdsRoot,
            String receiptRoot) {
        public Result {
            if (workRows < 0 || javaPreparationPaths < 0 || broaderScopeHolds < 0) {
                throw new IllegalArgumentException("negative completion count");
            }
            workRoot = sha(workRoot, "workRoot");
            javaPathsRoot = sha(javaPathsRoot, "javaPathsRoot");
            holdsRoot = sha(holdsRoot, "holdsRoot");
            receiptRoot = sha(receiptRoot, "receiptRoot");
        }
    }

    private A3Complete() {}

    public static Result write(
            Path root,
            Path inventory,
            Path queue,
            Path outDirectory) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        List<A3Work.Row> rows = A3Work.load(checkedRoot, inventory, queue);

        String work = A3WorkTsv.render(rows);
        List<String> javaPaths = A3Work.javaPreparationPaths(rows);
        String javaList = javaPaths.isEmpty() ? "" : String.join("\n", javaPaths) + "\n";
        String holds = renderHolds(rows);

        String workRoot = A3Fs.sha(work);
        String javaRoot = A3Fs.sha(javaList);
        String holdsRoot = A3Fs.sha(holds);
        int holdCount = countDataRows(holds);

        String receipt =
                "schema\twork_rows\tjava_preparation_paths\tbroader_scope_holds\t"
                        + "work_root\tjava_paths_root\tholds_root\tproduct_write_authority\t"
                        + "promotion_authority\n"
                        + SCHEMA
                        + "\t"
                        + rows.size()
                        + "\t"
                        + javaPaths.size()
                        + "\t"
                        + holdCount
                        + "\t"
                        + workRoot
                        + "\t"
                        + javaRoot
                        + "\t"
                        + holdsRoot
                        + "\tfalse\tfalse\n";
        String receiptRoot = A3Fs.sha(receipt);

        Path out = Objects.requireNonNull(outDirectory, "outDirectory");
        A3Fs.write(checkedRoot, out.resolve("work.tsv"), work);
        A3Fs.write(checkedRoot, out.resolve("java-preparation-paths.txt"), javaList);
        A3Fs.write(checkedRoot, out.resolve("scope-holds.tsv"), holds);
        A3Fs.write(
                checkedRoot,
                out.resolve("receipt.tsv"),
                receipt + "receipt_root\t" + receiptRoot + "\n");

        return new Result(
                rows.size(),
                javaPaths.size(),
                holdCount,
                workRoot,
                javaRoot,
                holdsRoot,
                receiptRoot);
    }

    static String renderHolds(List<A3Work.Row> rows) {
        Objects.requireNonNull(rows, "rows");
        Map<Integer, A3Work.Row> holds = new LinkedHashMap<>();
        for (A3Work.Row row : rows) {
            if (!"FILE".equals(row.joinScope())) {
                holds.putIfAbsent(row.featureOrder(), row);
            }
        }
        StringBuilder out =
                new StringBuilder(
                        "feature_order\trelease\tcommit\tjoin_scope\tproof_lane\t"
                                + "compatibility_state\tnext_action\n");
        for (A3Work.Row row : holds.values()) {
            out.append(row.featureOrder())
                    .append('\t')
                    .append(row.release())
                    .append('\t')
                    .append(row.commit())
                    .append('\t')
                    .append(row.joinScope())
                    .append('\t')
                    .append(row.proofLane())
                    .append('\t')
                    .append(row.compatibilityState())
                    .append('\t')
                    .append(A3Fs.cell(row.nextAction()))
                    .append('\n');
        }
        return out.toString();
    }

    private static int countDataRows(String tsv) {
        return Math.max(0, (int) tsv.lines().count() - 1);
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }
}
