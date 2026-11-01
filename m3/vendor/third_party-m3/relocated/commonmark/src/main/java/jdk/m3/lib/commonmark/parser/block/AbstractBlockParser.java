package jdk.m3.lib.commonmark.parser.block;

import java.util.List;
import jdk.m3.lib.commonmark.node.Block;
import jdk.m3.lib.commonmark.node.DefinitionMap;
import jdk.m3.lib.commonmark.node.SourceSpan;
import jdk.m3.lib.commonmark.parser.InlineParser;
import jdk.m3.lib.commonmark.parser.SourceLine;

public abstract class AbstractBlockParser implements BlockParser {

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    public boolean canHaveLazyContinuationLines() {
        return false;
    }

    @Override
    public boolean canContain(Block childBlock) {
        return false;
    }

    @Override
    public void addLine(SourceLine line) {}

    @Override
    public void addSourceSpan(SourceSpan sourceSpan) {
        getBlock().addSourceSpan(sourceSpan);
    }

    @Override
    public List<DefinitionMap<?>> getDefinitions() {
        return List.of();
    }

    @Override
    public void closeBlock() {}

    @Override
    public void parseInlines(InlineParser inlineParser) {}
}
