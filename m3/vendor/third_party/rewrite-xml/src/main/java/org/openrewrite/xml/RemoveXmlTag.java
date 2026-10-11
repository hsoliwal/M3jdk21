// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/RemoveXmlTag.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.xml;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

public final class RemoveXmlTag extends Recipe {
    @Option(displayName = "XPath", description = "An XPath expression used to find matching tags.", example = "/project/dependencies/dependency")
    private final String xPath;
    @Option(displayName = "File matcher", description = "If provided only matching files will be modified. This is a glob expression.", required = false, example = "\'**/application-*.xml\'")
    @Nullable
    private final String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Remove XML tag";
    }

    @Override
    public String getDescription() {
        return "Removes XML tags matching the provided expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(fileMatcher), new XmlIsoVisitor<ExecutionContext>() {
            private final XPathMatcher xPathMatcher = new XPathMatcher(xPath);
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    doAfterVisit(new RemoveContentVisitor<>(tag, true));
                }
                return super.visitTag(tag, ctx);
            }
        });
    }

    public RemoveXmlTag(final String xPath, @Nullable final String fileMatcher) {
        this.xPath = xPath;
        this.fileMatcher = fileMatcher;
    }

    public String getXPath() {
        return this.xPath;
    }

    @Nullable
    public String getFileMatcher() {
        return this.fileMatcher;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RemoveXmlTag(xPath=" + this.getXPath() + ", fileMatcher=" + this.getFileMatcher() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RemoveXmlTag)) return false;
        final RemoveXmlTag other = (RemoveXmlTag) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$xPath = this.getXPath();
        final Object other$xPath = other.getXPath();
        if (this$xPath == null ? other$xPath != null : !this$xPath.equals(other$xPath)) return false;
        final Object this$fileMatcher = this.getFileMatcher();
        final Object other$fileMatcher = other.getFileMatcher();
        if (this$fileMatcher == null ? other$fileMatcher != null : !this$fileMatcher.equals(other$fileMatcher)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof RemoveXmlTag;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $xPath = this.getXPath();
        result = result * PRIME + ($xPath == null ? 43 : $xPath.hashCode());
        final Object $fileMatcher = this.getFileMatcher();
        result = result * PRIME + ($fileMatcher == null ? 43 : $fileMatcher.hashCode());
        return result;
    }
}
