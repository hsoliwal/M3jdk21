package jdk.m3.lib.commonmark.internal.inline;

import jdk.m3.lib.commonmark.node.Node;
import jdk.m3.lib.commonmark.parser.beta.ParsedInline;
import jdk.m3.lib.commonmark.parser.beta.Position;

public class ParsedInlineImpl implements ParsedInline {
    private final Node node;
    private final Position position;

    public ParsedInlineImpl(Node node, Position position) {
        this.node = node;
        this.position = position;
    }

    public Node getNode() {
        return node;
    }

    public Position getPosition() {
        return position;
    }
}
