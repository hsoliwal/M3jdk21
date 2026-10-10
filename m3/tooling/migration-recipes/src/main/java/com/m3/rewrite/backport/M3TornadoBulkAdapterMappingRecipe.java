// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Records the optional TornadoVM provider under the existing M3 bulk-scheduler family. */
public final class M3TornadoBulkAdapterMappingRecipe extends Recipe {
    static final String MAP = "m3/docs/name-mapping.json";
    static final String CATALOGUE = "m3/tooling/recipe-catalogue.tsv";
    static final String FAMILY =
            "\"source_family\": \"Synexia BulkTask/BulkExecutor/M3BulkExecutor"
                    + " + TornadoVM verified RV32IM bulk provider\"";
    static final String OWNER = "\"com.m3.tornado.Rv32iBulkExecutorAdapter\"";
    static final String CATALOGUE_ROW =
            "m3-tornadovm-bulk-adapter\tcom.m3.rewrite.backport.M3TornadoBulkAdapterPortRecipe"
                    + "\tMODULE\tOPTIONAL_ACCELERATOR_PROVIDER_CPU_PARITY"
                    + "\tCANDIDATE_MATERIALIZED_UNVERIFIED"
                    + "\tPENDING_RECIPE_CPU_ADAPTER_PHYSICAL_GPU";

    @Override
    public String getDisplayName() {
        return "Map M3 TornadoVM bulk adapter";
    }

    @Override
    public String getDescription() {
        return "Adds the optional provider owner to the existing internal bulk-scheduler mapping "
                + "and records its recipe catalogue state without promoting hardware execution.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof PlainText text)) return tree;
                stopAfterPreVisit();
                String path = text.getSourcePath().toString().replace('\\', '/');
                if (MAP.equals(path)) return map(text);
                if (CATALOGUE.equals(path)) return catalogue(text);
                return text;
            }
        };
    }

    private static PlainText map(PlainText text) {
        String source = text.getText();
        if (!source.contains(FAMILY)) {
            throw new IllegalStateException("bulk scheduler mapping missing");
        }
        if (source.contains(OWNER)) return text;

        int family = source.indexOf(FAMILY);
        int owners = source.indexOf("      \"target_owners\": [", family);
        int close = source.indexOf("      ],", owners);
        if (owners < 0 || close < 0 || close > source.indexOf("      \"rule\":", owners)) {
            throw new IllegalStateException("bulk scheduler target owner anchor drift");
        }
        String beforeClose = source.substring(owners, close);
        String last = "        \"jdk.internal.vm.parallel.BulkExecution\"\n";
        if (!beforeClose.endsWith(last)) {
            throw new IllegalStateException("bulk scheduler owner tail drift");
        }
        String replacement =
                beforeClose.substring(0, beforeClose.length() - 1)
                        + ",\n        "
                        + OWNER
                        + "\n";
        return text.withText(source.substring(0, owners) + replacement + source.substring(close));
    }

    private static PlainText catalogue(PlainText text) {
        String source = text.getText();
        if (source.lines().anyMatch(CATALOGUE_ROW::equals)) return text;
        if (!source.startsWith("recipe_id\tclass\tscope\tcontract\tstatus\tverification\n")) {
            throw new IllegalStateException("recipe catalogue header drift");
        }
        String suffix = source.endsWith("\n") ? "" : "\n";
        return text.withText(source + suffix + CATALOGUE_ROW + "\n");
    }

    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}
