// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/AddCommentToXmlTag.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.xml;

import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;
import java.util.ArrayList;
import java.util.List;
import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.24.0")
public final class AddCommentToXmlTag extends Recipe {
    @Option(displayName = "XPath", description = "An XPath expression used to find matching tags.", example = "/project/dependencies/dependency")
    private final String xPath;
    @Option(displayName = "Comment text", description = "The text to add as a comment..", example = "This is excluded due to CVE <X> and will be removed when we upgrade the next version is available.")
    private final String commentText;

    @Override
    public String getDisplayName() {
        return "Add a comment to an XML tag";
    }

    @Override
    public String getDescription() {
        return "Adds a comment as the first element in an XML tag.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            final XPathMatcher matcher = new XPathMatcher(xPath);
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (matcher.matches(getCursor())) {
                    if (tag.getContent() != null) {
                        List<Content> contents = new ArrayList<>(tag.getContent());
                        boolean containsComment = contents.stream().anyMatch(c -> c instanceof Xml.Comment && commentText.equals(((Xml.Comment) c).getText()));
                        if (!containsComment) {
                            int insertPos = 0;
                            Xml.Comment customComment = new Xml.Comment(randomId(), contents.get(insertPos).getPrefix(), Markers.EMPTY, commentText);
                            contents.add(insertPos, customComment);
                            t = t.withContent(contents);
                        }
                    }
                }
                return t;
            }
        };
    }

    public AddCommentToXmlTag(final String xPath, final String commentText) {
        this.xPath = xPath;
        this.commentText = commentText;
    }

    public String getXPath() {
        return this.xPath;
    }

    public String getCommentText() {
        return this.commentText;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "AddCommentToXmlTag(xPath=" + this.getXPath() + ", commentText=" + this.getCommentText() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof AddCommentToXmlTag)) return false;
        final AddCommentToXmlTag other = (AddCommentToXmlTag) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$xPath = this.getXPath();
        final Object other$xPath = other.getXPath();
        if (this$xPath == null ? other$xPath != null : !this$xPath.equals(other$xPath)) return false;
        final Object this$commentText = this.getCommentText();
        final Object other$commentText = other.getCommentText();
        if (this$commentText == null ? other$commentText != null : !this$commentText.equals(other$commentText)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof AddCommentToXmlTag;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $xPath = this.getXPath();
        result = result * PRIME + ($xPath == null ? 43 : $xPath.hashCode());
        final Object $commentText = this.getCommentText();
        result = result * PRIME + ($commentText == null ? 43 : $commentText.hashCode());
        return result;
    }
}
