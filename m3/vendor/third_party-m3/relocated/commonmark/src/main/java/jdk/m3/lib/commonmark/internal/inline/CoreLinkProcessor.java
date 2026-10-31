package jdk.m3.lib.commonmark.internal.inline;

import jdk.m3.lib.commonmark.node.Image;
import jdk.m3.lib.commonmark.node.Link;
import jdk.m3.lib.commonmark.node.LinkReferenceDefinition;
import jdk.m3.lib.commonmark.parser.InlineParserContext;
import jdk.m3.lib.commonmark.parser.beta.LinkInfo;
import jdk.m3.lib.commonmark.parser.beta.LinkProcessor;
import jdk.m3.lib.commonmark.parser.beta.LinkResult;
import jdk.m3.lib.commonmark.parser.beta.Scanner;

public class CoreLinkProcessor implements LinkProcessor {

    @Override
    public LinkResult process(LinkInfo linkInfo, Scanner scanner, InlineParserContext context) {
        if (linkInfo.destination() != null) {
            // Inline link
            return process(linkInfo, scanner, linkInfo.destination(), linkInfo.title());
        }

        var label = linkInfo.label();
        var ref = label != null && !label.isEmpty() ? label : linkInfo.text();
        var def = context.getDefinition(LinkReferenceDefinition.class, ref);
        if (def != null) {
            // Reference link
            return process(linkInfo, scanner, def.getDestination(), def.getTitle());
        }
        return LinkResult.none();
    }

    private static LinkResult process(
            LinkInfo linkInfo, Scanner scanner, String destination, String title) {
        if (linkInfo.marker() != null && linkInfo.marker().getLiteral().equals("!")) {
            return LinkResult.wrapTextIn(new Image(destination, title), scanner.position())
                    .includeMarker();
        }
        return LinkResult.wrapTextIn(new Link(destination, title), scanner.position());
    }
}
