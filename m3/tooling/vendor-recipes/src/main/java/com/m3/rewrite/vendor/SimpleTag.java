// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

/**
 * A tag the oracle's {@code <name>([^<]*)</name>} regex would match: start tag printed exactly {@code <name>} (no
 * attributes, no space before {@code >}), end tag printed exactly {@code </name>}, and content made only of
 * non-CDATA character data. {@link #raw()} is the printed content, i.e. the regex group, whitespace and entity
 * references included.
 */
record SimpleTag(Xml.Tag tag, String raw) {

    /** Start tag printed exactly {@code <name>} and an end tag present (the regex literal {@code <dependency>}). */
    static boolean exactStart(Xml.Tag tag) {
        return tag.getAttributes().isEmpty() && tag.getBeforeTagDelimiterPrefix().isEmpty() && tag.getClosing() != null;
    }

    static Optional<SimpleTag> of(Xml.Tag tag) {
        if (!exactStart(tag) || !tag.getClosing().getBeforeTagDelimiterPrefix().isEmpty()) {
            return Optional.empty();
        }
        List<? extends Content> content = tag.getContent() == null ? List.of() : tag.getContent();
        if (!content.stream().allMatch(c -> c instanceof Xml.CharData d && !d.isCdata())) {
            return Optional.empty();
        }
        String raw = content.stream().map(Xml.CharData.class::cast)
                .map(d -> d.getPrefix() + d.getText() + d.getAfterText())
                .collect(Collectors.joining()) + tag.getClosing().getPrefix();
        return raw.indexOf('<') >= 0 ? Optional.empty() : Optional.of(new SimpleTag(tag, raw));
    }

    /** Descendant tags of {@code tag} in document order (pre-order), excluding {@code tag} itself. */
    static Stream<Xml.Tag> descendants(Xml.Tag tag) {
        return tag.getChildren().stream().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }

    /** {@code re.search(r"<name>([^<]*)</name>", block)}: the first regex-matchable descendant with that name. */
    static Optional<SimpleTag> first(Xml.Tag block, String name) {
        return descendants(block).filter(t -> t.getName().equals(name)).map(SimpleTag::of)
                .flatMap(Optional::stream).findFirst();
    }

    /** {@code block.replace("<version>raw</version>", "<version>value</version>", 1)} on the tree. */
    Xml.Tag replaceIn(Xml.Tag block, String value) {
        Xml.Tag replacement = tag.withContent(List.of(new Xml.CharData(Tree.randomId(), "", Markers.EMPTY, false, value, "")))
                .withClosing(tag.getClosing().withPrefix(""));
        return (Xml.Tag) new XmlIsoVisitor<Integer>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag t, Integer p) {
                return t.getId().equals(tag.getId()) ? replacement : super.visitTag(t, p);
            }
        }.visitNonNull(block, 0);
    }
}
