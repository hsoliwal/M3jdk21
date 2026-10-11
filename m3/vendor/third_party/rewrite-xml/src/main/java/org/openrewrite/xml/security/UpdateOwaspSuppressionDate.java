// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/security/UpdateOwaspSuppressionDate.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class UpdateOwaspSuppressionDate extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update OWASP suppression date bounds";
    }

    @Override
    public String getDescription() {
        return "Updates the expiration date for OWASP suppressions having a matching cve tag. " + "For use with the OWASP `dependency-check` tool. " + "More details: https://jeremylong.github.io/DependencyCheck/general/suppression.html.";
    }

    @Option(displayName = "CVE List", description = "Update suppressions having any of the specified CVE tags.", example = "CVE-2022-1234")
    private final List<String> cveList;
    @Option(displayName = "Until date", required = false, description = "Optional. The date to add to the suppression. Default will be 30 days from today.", example = "2023-01-01")
    @Nullable
    private final String untilDate;

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
                    boolean hasCve = false;
                    List<Xml.Tag> cveTags = t.getChildren("cve");
                    for (Xml.Tag xml : cveTags) {
                        String cveNum = xml.getValue().orElse("");
                        for (String cve : cveList) {
                            if (!StringUtils.isNullOrEmpty(cve) && cve.equals(cveNum)) {
                                hasCve = true;
                                break;
                            }
                        }
                    }
                    if (hasCve) {
                        String date = (untilDate != null && !untilDate.isEmpty()) ? untilDate : LocalDate.now().plus(30, ChronoUnit.DAYS).toString();
                        final String zuluDate = date + "Z";
                        t = t.withAttributes(ListUtils.map(t.getAttributes(), attr -> {
                            if ("until".equals(attr.getKeyAsString())) {
                                if (!zuluDate.equals(attr.getValueAsString())) {
                                    attr = attr.withValue(attr.getValue().withValue(zuluDate));
                                }
                            }
                            return attr;
                        }));
                    }
                }
                return t;
            }
        });
    }

    public UpdateOwaspSuppressionDate(final List<String> cveList, @Nullable final String untilDate) {
        this.cveList = cveList;
        this.untilDate = untilDate;
    }

    public List<String> getCveList() {
        return this.cveList;
    }

    @Nullable
    public String getUntilDate() {
        return this.untilDate;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "UpdateOwaspSuppressionDate(cveList=" + this.getCveList() + ", untilDate=" + this.getUntilDate() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof UpdateOwaspSuppressionDate)) return false;
        final UpdateOwaspSuppressionDate other = (UpdateOwaspSuppressionDate) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$cveList = this.getCveList();
        final Object other$cveList = other.getCveList();
        if (this$cveList == null ? other$cveList != null : !this$cveList.equals(other$cveList)) return false;
        final Object this$untilDate = this.getUntilDate();
        final Object other$untilDate = other.getUntilDate();
        if (this$untilDate == null ? other$untilDate != null : !this$untilDate.equals(other$untilDate)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof UpdateOwaspSuppressionDate;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $cveList = this.getCveList();
        result = result * PRIME + ($cveList == null ? 43 : $cveList.hashCode());
        final Object $untilDate = this.getUntilDate();
        result = result * PRIME + ($untilDate == null ? 43 : $untilDate.hashCode());
        return result;
    }
}
