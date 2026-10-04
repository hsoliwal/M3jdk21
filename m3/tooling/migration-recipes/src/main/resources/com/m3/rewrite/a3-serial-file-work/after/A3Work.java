// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Expands compatibility-queue features into deterministic FILE preparation atoms.
 *
 * <p>Every row retains the parent feature's honest recomposition scope. FILE preparation never
 * implies FILE promotion authority.</p>
 */
public final class A3Work {

    public enum TargetState {
        PRESENT,
        ABSENT,
        OUTSIDE_A3,
        FEATURE
    }

    public enum PrepareLane {
        A3_JAVA_ATOMIZE_PATTERNIZE,
        ADD_JAVA_REVIEW,
        NATIVE_ATOM_REVIEW,
        NATIVE_ADD_REVIEW,
        HASH_PINNED_TEXT_REVIEW,
        HASH_PINNED_ADD_REVIEW,
        OUTSIDE_A3_REVIEW,
        FEATURE_REVIEW
    }

    public record Row(
            int featureOrder,
            int fileOrder,
            int release,
            String commit,
            String jbsIds,
            String subject,
            String domain,
            String risk,
            String joinScope,
            String proofLane,
            String recipeStrategy,
            int priority,
            String compatibilityState,
            String nextAction,
            String path,
            TargetState targetState,
            A3Inv.Kind kind,
            String targetSha256,
            PrepareLane prepareLane,
            String atomScope) {

        public Row {
            A3WorkValues.validate(
                    featureOrder,
                    fileOrder,
                    release,
                    commit,
                    jbsIds,
                    subject,
                    domain,
                    risk,
                    joinScope,
                    proofLane,
                    recipeStrategy,
                    priority,
                    compatibilityState,
                    nextAction,
                    path,
                    targetState,
                    kind,
                    targetSha256,
                    prepareLane,
                    atomScope);
        }
    }

    private A3Work() {}

    public static List<Row> load(Path root, Path inventoryFile, Path queueFile)
            throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Map<String, A3Inv.Row> inventory =
                A3WorkTsv.inventory(A3Fs.source(checkedRoot, inventoryFile));
        List<A3WorkTsv.Feature> queue =
                A3WorkTsv.queue(A3Fs.source(checkedRoot, queueFile));
        ArrayList<Row> rows = new ArrayList<>();
        for (A3WorkTsv.Feature feature : queue) {
            appendFeature(rows, feature, inventory);
        }
        rows.sort(
                Comparator.comparingInt(Row::featureOrder)
                        .thenComparingInt(Row::fileOrder)
                        .thenComparing(Row::path));
        return List.copyOf(rows);
    }

    public static void write(
            Path root,
            Path inventoryFile,
            Path queueFile,
            Path out)
            throws IOException {
        A3Fs.write(
                root,
                out,
                A3WorkTsv.render(load(root, inventoryFile, queueFile)));
    }

    public static List<String> javaPreparationPaths(List<Row> rows) {
        return Objects.requireNonNull(rows, "rows").stream()
                .filter(A3Work::isJavaPreparation)
                .map(Row::path)
                .distinct()
                .toList();
    }

    private static boolean isJavaPreparation(Row row) {
        return row.prepareLane() == PrepareLane.A3_JAVA_ATOMIZE_PATTERNIZE;
    }

    private static void appendFeature(
            List<Row> rows,
            A3WorkTsv.Feature feature,
            Map<String, A3Inv.Row> inventory) {
        if (feature.paths().isEmpty()) {
            rows.add(featureRow(feature));
            return;
        }
        int fileOrder = 0;
        for (String path : feature.paths()) {
            rows.add(fileRow(feature, fileOrder++, path, inventory.get(path)));
        }
    }

    private static Row featureRow(A3WorkTsv.Feature feature) {
        return row(
                feature,
                0,
                "",
                TargetState.FEATURE,
                A3Inv.Kind.OTHER,
                "FEATURE",
                PrepareLane.FEATURE_REVIEW);
    }

    private static Row fileRow(
            A3WorkTsv.Feature feature,
            int fileOrder,
            String path,
            A3Inv.Row current) {
        if (current != null) {
            return row(
                    feature,
                    fileOrder,
                    path,
                    TargetState.PRESENT,
                    current.kind(),
                    current.sha256(),
                    presentLane(current.kind()));
        }
        A3Inv.Kind kind = A3Inv.kind(path);
        if (path.startsWith("src/") || path.startsWith("test/")) {
            return row(
                    feature,
                    fileOrder,
                    path,
                    TargetState.ABSENT,
                    kind,
                    "ABSENT",
                    absentLane(kind));
        }
        return row(
                feature,
                fileOrder,
                path,
                TargetState.OUTSIDE_A3,
                kind,
                "UNSCANNED",
                PrepareLane.OUTSIDE_A3_REVIEW);
    }

    private static Row row(
            A3WorkTsv.Feature feature,
            int fileOrder,
            String path,
            TargetState targetState,
            A3Inv.Kind kind,
            String targetSha256,
            PrepareLane prepareLane) {
        return new Row(
                feature.order(),
                fileOrder,
                feature.release(),
                feature.commit(),
                feature.jbsIds(),
                feature.subject(),
                feature.domain(),
                feature.risk(),
                feature.scope(),
                feature.proofLane(),
                feature.recipeStrategy(),
                feature.priority(),
                feature.compatibilityState(),
                feature.nextAction(),
                path,
                targetState,
                kind,
                targetSha256,
                prepareLane,
                "FILE");
    }

    private static PrepareLane presentLane(A3Inv.Kind kind) {
        return switch (kind) {
            case JAVA -> PrepareLane.A3_JAVA_ATOMIZE_PATTERNIZE;
            case NATIVE -> PrepareLane.NATIVE_ATOM_REVIEW;
            case RESOURCE, OTHER -> PrepareLane.HASH_PINNED_TEXT_REVIEW;
        };
    }

    private static PrepareLane absentLane(A3Inv.Kind kind) {
        return switch (kind) {
            case JAVA -> PrepareLane.ADD_JAVA_REVIEW;
            case NATIVE -> PrepareLane.NATIVE_ADD_REVIEW;
            case RESOURCE, OTHER -> PrepareLane.HASH_PINNED_ADD_REVIEW;
        };
    }
}
