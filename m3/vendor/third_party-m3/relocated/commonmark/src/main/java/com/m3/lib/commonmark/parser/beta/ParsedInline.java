package com.m3.lib.commonmark.parser.beta;

import java.util.Objects;
import com.m3.lib.commonmark.internal.inline.ParsedInlineImpl;
import com.m3.lib.commonmark.node.Node;

/**
 * The result of a single inline parser. Use the static methods to create instances.
 *
 * <p><em>This interface is not intended to be implemented by clients.</em>
 */
public interface ParsedInline {

    static ParsedInline none() {
        return null;
    }

    static ParsedInline of(Node node, Position position) {
        Objects.requireNonNull(node, "node must not be null");
        Objects.requireNonNull(position, "position must not be null");
        return new ParsedInlineImpl(node, position);
    }
}
