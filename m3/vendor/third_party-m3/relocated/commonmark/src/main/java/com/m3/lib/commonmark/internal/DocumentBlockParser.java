package com.m3.lib.commonmark.internal;

import com.m3.lib.commonmark.node.Block;
import com.m3.lib.commonmark.node.Document;
import com.m3.lib.commonmark.parser.block.AbstractBlockParser;
import com.m3.lib.commonmark.parser.block.BlockContinue;
import com.m3.lib.commonmark.parser.block.ParserState;

public class DocumentBlockParser extends AbstractBlockParser {

    private final Document document = new Document();

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public boolean canContain(Block block) {
        return true;
    }

    @Override
    public Document getBlock() {
        return document;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        return BlockContinue.atIndex(state.getIndex());
    }
}
