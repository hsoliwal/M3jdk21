// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;

/**
 * Inventories every OpenRewrite recipe descriptor actually visible on the current runtime classpath.
 *
 * <p>This is the executable denominator, distinct from the much larger public web catalogue.
 * Presence means discoverable/activatable by this runtime; it does not grant automatic mutation,
 * semantic-equivalence, license, or promotion authority.</p>
 */
public final class M3OpenRewriteRuntimeRecipeInventoryRecipe extends Recipe {
    private final transient RecipeTable table = new RecipeTable(this);

    @Override
    public String getDisplayName() {
        return "M3 OpenRewrite runtime recipe inventory";
    }

    @Override
    public String getDescription() {
        return "Emits all OpenRewrite recipe descriptors visible to the current recipe classpath "
                + "so reuse can precede synthesis without confusing catalogue presence with proof.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "openrewrite",
                "recipe-catalog",
                "inventory-first",
                "read-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                stopAfterPreVisit();
                String key =
                        M3OpenRewriteRuntimeRecipeInventoryRecipe.class.getName()
                                + ".emitted";
                synchronized (context) {
                    if (Boolean.TRUE.equals(context.getMessage(key))) {
                        return tree;
                    }
                    context.putMessage(key, Boolean.TRUE);
                    for (RecipeDescriptor descriptor : descriptors()) {
                        table.insertRow(
                                context,
                                new RecipeRow(
                                        descriptor.getName(),
                                        descriptor.getDisplayName(),
                                        source(descriptor.getSource()),
                                        descriptor.getTags().stream().sorted().toList(),
                                        descriptor.getOptions().size(),
                                        descriptor.getRecipeList().size(),
                                        descriptor.getDataTables().size(),
                                        false,
                                        false,
                                        false));
                    }
                }
                return tree;
            }
        };
    }

    public static List<RecipeDescriptor> descriptors() {
        Collection<RecipeDescriptor> raw =
                Environment.builder()
                        .scanRuntimeClasspath()
                        .build()
                        .listRecipeDescriptors();
        ArrayList<RecipeDescriptor> sorted = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        raw.stream()
                .sorted(Comparator.comparing(RecipeDescriptor::getName))
                .forEach(
                        descriptor -> {
                            if (seen.add(descriptor.getName())) {
                                sorted.add(descriptor);
                            }
                        });
        return List.copyOf(sorted);
    }

    public boolean mutationAuthority() {
        return false;
    }

    public boolean semanticEquivalenceAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static String source(URI source) {
        return source == null ? "" : source.toString();
    }

    public static final class RecipeTable extends DataTable<RecipeRow> {
        RecipeTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 OpenRewrite runtime recipes",
                    "Exact recipe descriptors visible on the current execution classpath.");
        }
    }

    public record RecipeRow(
            @Column(displayName = "Recipe name", description = "Fully-qualified recipe name.")
                    String recipeName,
            @Column(displayName = "Display name", description = "Recipe display name.")
                    String displayName,
            @Column(displayName = "Source", description = "Recipe source URI when available.")
                    String source,
            @Column(displayName = "Tags", description = "Sorted recipe tags.")
                    List<String> tags,
            @Column(displayName = "Options", description = "Configuration option count.")
                    int optionCount,
            @Column(displayName = "Children", description = "Composite child recipe count.")
                    int childRecipeCount,
            @Column(displayName = "Data tables", description = "Declared data-table count.")
                    int dataTableCount,
            @Column(displayName = "Mutation authority", description = "Always false here.")
                    boolean mutationAuthority,
            @Column(displayName = "Equivalence authority", description = "Always false here.")
                    boolean semanticEquivalenceAuthority,
            @Column(displayName = "Promotion authority", description = "Always false here.")
                    boolean promotionAuthority) {}
}
