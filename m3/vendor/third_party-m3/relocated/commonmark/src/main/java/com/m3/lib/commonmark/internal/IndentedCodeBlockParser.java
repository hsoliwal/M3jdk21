package com.m3.lib.commonmark.internal;

import java.util.ArrayList;
import java.util.List;
import com.m3.lib.commonmark.internal.util.Parsing;
import com.m3.lib.commonmark.node.Block;
import com.m3.lib.commonmark.node.IndentedCodeBlock;
import com.m3.lib.commonmark.node.Paragraph;
import com.m3.lib.commonmark.parser.SourceLine;
import com.m3.lib.commonmark.parser.block.*;
import com.m3.lib.commonmark.text.Characters;

public class IndentedCodeBlockParser extends AbstractBlockParser {

    private final IndentedCodeBlock block = new IndentedCodeBlock();
    private final List<CharSequence> lines = new ArrayList<>();

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        if (state.getIndent() >= Parsing.CODE_BLOCK_INDENT) {
            return BlockContinue.atColumn(state.getColumn() + Parsing.CODE_BLOCK_INDENT);
        } else if (state.isBlank()) {
            return BlockContinue.atIndex(state.getNextNonSpaceIndex());
        } else {
            return BlockContinue.none();
        }
    }

    @Override
    public void addLine(SourceLine line) {
        lines.add(line.getContent());
    }

    @Override
    public void closeBlock() {
        int lastNonBlank = lines.size() - 1;
        while (lastNonBlank >= 0) {
            if (!Characters.isBlank(lines.get(lastNonBlank))) {
                break;
            }
            lastNonBlank--;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lastNonBlank + 1; i++) {
            sb.append(lines.get(i));
            sb.append('\n');
        }

        String literal = sb.toString();
        block.setLiteral(literal);
    }

    public static class Factory extends AbstractBlockParserFactory {

        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            // An indented code block cannot interrupt a paragraph.
            if (state.getIndent() >= Parsing.CODE_BLOCK_INDENT
                    && !state.isBlank()
                    && !(state.getActiveBlockParser().getBlock() instanceof Paragraph)) {
                return BlockStart.of(new IndentedCodeBlockParser())
                        .atColumn(state.getColumn() + Parsing.CODE_BLOCK_INDENT);
            } else {
                return BlockStart.none();
            }
        }
    }
}
