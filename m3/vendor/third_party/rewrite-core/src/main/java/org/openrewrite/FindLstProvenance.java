// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/FindLstProvenance.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.openrewrite.marker.LstProvenance;
import org.openrewrite.table.LstProvenanceTable;
import java.util.HashSet;
import java.util.Set;
import static java.time.ZoneOffset.UTC;

public final class FindLstProvenance extends ScanningRecipe<FindLstProvenance.Accumulator> {
    @Override
    public String getDisplayName() {
        return "Find LST provenance";
    }

    @Override
    public String getDescription() {
        return "Produces a data table showing what versions of OpenRewrite/Moderne tooling was used to produce a given LST.";
    }

    private final transient LstProvenanceTable provenanceTable = new LstProvenanceTable(this);


    public static class Accumulator {
        Set<LstProvenance> seenProvenance = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                LstProvenance lstProvenance = tree.getMarkers().findFirst(LstProvenance.class).orElse(null);
                if (lstProvenance == null) {
                    return tree;
                }
                if (acc.seenProvenance.add(lstProvenance)) {
                    provenanceTable.insertRow(ctx, new LstProvenanceTable.Row(lstProvenance.getBuildToolType(), lstProvenance.getBuildToolVersion(), lstProvenance.getLstSerializerVersion(), lstProvenance.getTimestampUtc() == null ? null : lstProvenance.getTimestampUtc().toEpochMilli(), lstProvenance.getTimestampUtc() == null ? null : lstProvenance.getTimestampUtc().atZone(UTC).toString()));
                }
                return tree;
            }
        };
    }

    public FindLstProvenance() {
    }

    public LstProvenanceTable getProvenanceTable() {
        return this.provenanceTable;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindLstProvenance(provenanceTable=" + this.getProvenanceTable() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindLstProvenance)) return false;
        final FindLstProvenance other = (FindLstProvenance) o;
        if (!other.canEqual((Object) this)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindLstProvenance;
    }

    @Override
    public int hashCode() {
        final int result = 1;
        return result;
    }
}
