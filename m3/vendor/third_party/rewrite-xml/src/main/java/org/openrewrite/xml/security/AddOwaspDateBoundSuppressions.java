// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/security/AddOwaspDateBoundSuppressions.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.xml.security;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class AddOwaspDateBoundSuppressions extends Recipe {
    @Option(displayName = "Until date", required = false, description = "Optional. The date to add to the suppression. Default will be 30 days from today.", example = "2023-01-01")
    @Nullable
    private final String untilDate;

    @Override
    public String getDisplayName() {
        return "Add date bounds to OWASP suppressions";
    }

    @Override
    public String getDescription() {
        return "Adds an expiration date to all OWASP suppressions in order to ensure that they are periodically reviewed. " + "For use with the OWASP `dependency-check` tool. " + "More details: https://jeremylong.github.io/DependencyCheck/general/suppression.html.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("untilDate", "Must be empty or a valid date of format yyyy-MM-dd", untilDate, date -> {
            if (date != null && !date.isEmpty()) {
                try {
                    LocalDate.parse(date);
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsOwaspSuppressionsFile(), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (new XPathMatcher("/suppressions/suppress").matches(getCursor())) {
                    boolean hasUntil = false;
                    List<Xml.Attribute> attributes = t.getAttributes();
                    for (Xml.Attribute attribute : attributes) {
                        if (attribute.getKeyAsString().equals("until")) {
                            hasUntil = true;
                        }
                    }
                    if (!hasUntil) {
                        String date = (untilDate != null && !untilDate.isEmpty()) ? untilDate : LocalDate.now().plus(30, ChronoUnit.DAYS).toString();
                        return t.withAttributes(ListUtils.concat(attributes, autoFormat(new Xml.Attribute(Tree.randomId(), "", Markers.EMPTY, new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, "until"), "", autoFormat(new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, date + "Z"), ctx)), ctx)));
                    }
                }
                return t;
            }
        });
    }

    public AddOwaspDateBoundSuppressions(@Nullable final String untilDate) {
        this.untilDate = untilDate;
    }

    @Nullable
    public String getUntilDate() {
        return this.untilDate;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "AddOwaspDateBoundSuppressions(untilDate=" + this.getUntilDate() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof AddOwaspDateBoundSuppressions)) return false;
        final AddOwaspDateBoundSuppressions other = (AddOwaspDateBoundSuppressions) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$untilDate = this.getUntilDate();
        final Object other$untilDate = other.getUntilDate();
        if (this$untilDate == null ? other$untilDate != null : !this$untilDate.equals(other$untilDate)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof AddOwaspDateBoundSuppressions;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $untilDate = this.getUntilDate();
        result = result * PRIME + ($untilDate == null ? 43 : $untilDate.hashCode());
        return result;
    }
}
