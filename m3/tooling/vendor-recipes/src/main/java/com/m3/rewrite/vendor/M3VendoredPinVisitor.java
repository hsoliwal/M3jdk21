// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import java.util.Map;
import java.util.Optional;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

/**
 * Visits every {@code <dependency>} of one pom in document order. A dependency under a {@code <plugin>} is never
 * changed (one {@code PLUGIN_DEPENDENCY_UNTOUCHED} row when it names an artifactId); any other dependency with
 * groupId, artifactId and version gets the {@link PinPolicy} decision, and only {@code SWITCHED} edits the tree
 * (the version text becomes {@code strip(declared) + suffix}).
 */
final class M3VendoredPinVisitor extends XmlIsoVisitor<ExecutionContext> {

    private static final XPathMatcher DEPENDENCY = new XPathMatcher("//dependency");
    private static final String PROPS = "m3.vendored-pin.props";
    private static final String POM = "m3.vendored-pin.pom";

    private final PinPolicy policy;
    private final M3VendoredPinRows rows;

    M3VendoredPinVisitor(PinPolicy policy, M3VendoredPinRows rows) {
        this.policy = policy;
        this.rows = rows;
    }

    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
        getCursor().putMessage(PROPS, policy.properties(document.getRoot()));
        getCursor().putMessage(POM, PyText.slash(document.getSourcePath()));
        return super.visitDocument(document, ctx);
    }

    @Override
    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
        if (!DEPENDENCY.matches(getCursor()) || !SimpleTag.exactStart(tag)) {
            return super.visitTag(tag, ctx);
        }
        String pom = getCursor().getNearestMessage(POM, "");
        if (insidePlugin(getCursor())) {
            SimpleTag.first(tag, "artifactId").ifPresent(a -> rows.insertRow(ctx, PinPolicy.pluginRow(pom, PyText.strip(a.raw())).row()));
            return tag;
        }
        return pin(tag, pom, getCursor().getNearestMessage(PROPS, Map.of()), ctx);
    }

    private Xml.Tag pin(Xml.Tag dependency, String pom, Map<String, String> props, ExecutionContext ctx) {
        Optional<SimpleTag> g = SimpleTag.first(dependency, "groupId");
        Optional<SimpleTag> a = SimpleTag.first(dependency, "artifactId");
        Optional<SimpleTag> v = SimpleTag.first(dependency, "version");
        if (g.isEmpty() || a.isEmpty() || v.isEmpty()) {
            return dependency;
        }
        Optional<PinPolicy.Decision> decision = policy.decide(pom, PyText.strip(g.get().raw()), PyText.strip(a.get().raw()), v.get().raw(), props);
        decision.ifPresent(d -> rows.insertRow(ctx, d.row()));
        return decision.flatMap(PinPolicy.Decision::newVersion).map(nv -> v.get().replaceIn(dependency, nv)).orElse(dependency);
    }

    /** The oracle's plugin spans: a dependency whose start lies inside {@code <plugin>...</plugin>}. */
    private static boolean insidePlugin(Cursor cursor) {
        return cursor.getPathAsStream().skip(1).filter(Xml.Tag.class::isInstance).map(Xml.Tag.class::cast)
                .anyMatch(t -> "plugin".equals(t.getName()) && SimpleTag.exactStart(t));
    }
}
