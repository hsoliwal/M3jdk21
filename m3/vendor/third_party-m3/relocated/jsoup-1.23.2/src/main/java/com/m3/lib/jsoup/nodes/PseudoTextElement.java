package com.m3.lib.jsoup.nodes;

import com.m3.lib.jsoup.internal.QuietAppendable;
import com.m3.lib.jsoup.parser.Tag;

/**
 * Represents a {@link TextNode} as an {@link Element}, to enable text nodes to be selected with
 * the {@link com.m3.lib.jsoup.select.Selector} {@code :matchText} syntax.
 * @deprecated use {@link Element#selectNodes(String, Class)} instead, with selector of <code>::textnode</code> and class <code>TextNode</code>;
 * will be removed in jsoup 1.24.1.
 */
@Deprecated
public class PseudoTextElement extends Element {

    public PseudoTextElement(Tag tag, String baseUri, Attributes attributes) {
        super(tag, baseUri, attributes);
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
    }

    @Override
    void outerHtmlTail(QuietAppendable accum, Document.OutputSettings out) {
    }
}
