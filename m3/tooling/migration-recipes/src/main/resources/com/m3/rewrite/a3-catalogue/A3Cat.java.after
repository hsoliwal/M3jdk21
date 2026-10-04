// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Frozen recipe/provenance catalogue for A3.
 *
 * <p>The catalogue separates executable M3 recipes from official OpenRewrite reference candidates.
 * A row is evidence, not implicit activation. In particular, non-Apache donor sources are forced
 * to {@link Activation#REFERENCE_ONLY} and can never become executable through this catalogue.
 */
public final class A3Cat {

    private static final String RESOURCE = "/com/m3/a3/OPENREWRITE_RECIPES.tsv";
    private static final String HEADER =
            "origin\tcategory\trecipe\trole\tactivation\tlicense\trepository\trevision"
                    + "\treference\treason";

    public enum Origin {
        M3,
        OPENREWRITE
    }

    public enum Activation {
        ADMITTED,
        OPT_IN,
        CONTROL_ONLY,
        REFERENCE_ONLY
    }

    public record Row(
            Origin origin,
            String category,
            String recipe,
            String role,
            Activation activation,
            String license,
            String repository,
            String revision,
            String reference,
            String reason) {

        public Row {
            origin = Objects.requireNonNull(origin, "origin");
            category = text(category, "category");
            recipe = text(recipe, "recipe");
            role = text(role, "role");
            activation = Objects.requireNonNull(activation, "activation");
            license = text(license, "license");
            repository = text(repository, "repository");
            revision = text(revision, "revision").toLowerCase(Locale.ROOT);
            reference = text(reference, "reference");
            reason = text(reason, "reason");

            if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
                throw new IllegalArgumentException("repository");
            }
            if (!revision.matches("[0-9a-f]{40}")) {
                throw new IllegalArgumentException("revision");
            }
            if (!license.equals("Apache-2.0")
                    && !license.equals("Moderne-Source-Available")) {
                throw new IllegalArgumentException("license");
            }
            if (license.equals("Moderne-Source-Available")
                    && activation != Activation.REFERENCE_ONLY) {
                throw new IllegalArgumentException(
                        "source-available donor must remain REFERENCE_ONLY");
            }
            if (origin == Origin.M3) {
                if (!repository.equals("hsoliwal/M3jdk21")
                        || !license.equals("Apache-2.0")
                        || activation != Activation.ADMITTED
                        || !reference.startsWith("m3/")) {
                    throw new IllegalArgumentException("invalid M3 recipe authority row");
                }
            } else if (!repository.startsWith("openrewrite/")
                    || !reference.startsWith("https://docs.openrewrite.org/")) {
                throw new IllegalArgumentException("invalid OpenRewrite provenance row");
            }
            if (activation == Activation.CONTROL_ONLY
                    && !"CONTROL_PLANE".equals(role)) {
                throw new IllegalArgumentException("CONTROL_ONLY row is not control-plane");
            }
        }

        public boolean executableByA3() {
            return origin == Origin.M3 && activation == Activation.ADMITTED;
        }
    }

    private A3Cat() {
    }

    public static List<Row> load() throws IOException {
        String text;
        try (InputStream input = A3Cat.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("missing A3 recipe catalogue resource");
            }
            text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        String[] lines = text.split("\\R", -1);
        if (lines.length < 2 || !HEADER.equals(lines[0])) {
            throw new IOException("invalid A3 recipe catalogue header");
        }

        ArrayList<Row> rows = new ArrayList<>();
        Set<String> recipes = new HashSet<>();
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            String[] fields = lines[index].split("\\t", -1);
            if (fields.length != 10) {
                throw new IOException("malformed A3 recipe catalogue row " + (index + 1));
            }
            Row row =
                    new Row(
                            parse(Origin.class, fields[0], "origin"),
                            fields[1],
                            fields[2],
                            fields[3],
                            parse(Activation.class, fields[4], "activation"),
                            fields[5],
                            fields[6],
                            fields[7],
                            fields[8],
                            fields[9]);
            if (!recipes.add(row.recipe())) {
                throw new IOException("duplicate A3 recipe catalogue row: " + row.recipe());
            }
            rows.add(row);
        }
        rows.sort(
                Comparator.comparing((Row row) -> row.origin().ordinal())
                        .thenComparing(Row::category)
                        .thenComparing(Row::recipe));
        return List.copyOf(rows);
    }

    public static String root(List<Row> rows) {
        StringBuilder canonical = new StringBuilder("A3-RECIPE-CATALOGUE/1\n");
        ArrayList<Row> ordered =
                new ArrayList<>(Objects.requireNonNull(rows, "rows"));
        ordered.sort(
                Comparator.comparing((Row row) -> row.origin().ordinal())
                        .thenComparing(Row::category)
                        .thenComparing(Row::recipe));
        for (Row row : ordered) {
            canonical.append(row.origin())
                    .append('\t')
                    .append(A3Fs.cell(row.category()))
                    .append('\t')
                    .append(A3Fs.cell(row.recipe()))
                    .append('\t')
                    .append(A3Fs.cell(row.role()))
                    .append('\t')
                    .append(row.activation())
                    .append('\t')
                    .append(A3Fs.cell(row.license()))
                    .append('\t')
                    .append(A3Fs.cell(row.repository()))
                    .append('\t')
                    .append(row.revision())
                    .append('\t')
                    .append(A3Fs.cell(row.reference()))
                    .append('\t')
                    .append(A3Fs.cell(row.reason()))
                    .append('\n');
        }
        return A3Fs.sha(canonical.toString());
    }

    public static void write(Path root, Path out) throws IOException {
        List<Row> rows = load();
        String catalogRoot = root(rows);
        StringBuilder tsv =
                new StringBuilder(
                        "catalogRoot\torigin\tcategory\trecipe\trole\tactivation\tlicense"
                                + "\trepository\trevision\treference\treason\n");
        for (Row row : rows) {
            tsv.append(catalogRoot)
                    .append('\t')
                    .append(row.origin())
                    .append('\t')
                    .append(A3Fs.cell(row.category()))
                    .append('\t')
                    .append(A3Fs.cell(row.recipe()))
                    .append('\t')
                    .append(A3Fs.cell(row.role()))
                    .append('\t')
                    .append(row.activation())
                    .append('\t')
                    .append(A3Fs.cell(row.license()))
                    .append('\t')
                    .append(A3Fs.cell(row.repository()))
                    .append('\t')
                    .append(row.revision())
                    .append('\t')
                    .append(A3Fs.cell(row.reference()))
                    .append('\t')
                    .append(A3Fs.cell(row.reason()))
                    .append('\n');
        }
        A3Fs.write(root, out, tsv.toString());
    }

    public static Row require(List<Row> rows, String recipe) {
        String checked = text(recipe, "recipe");
        return List.copyOf(Objects.requireNonNull(rows, "rows")).stream()
                .filter(row -> row.recipe().equals(checked))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("uncatalogued recipe: " + checked));
    }

    private static <E extends Enum<E>> E parse(
            Class<E> type,
            String value,
            String field) {
        try {
            return Enum.valueOf(type, text(value, field));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(field, invalid);
        }
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
