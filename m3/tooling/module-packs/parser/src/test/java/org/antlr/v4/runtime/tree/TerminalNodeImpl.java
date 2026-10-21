// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.antlr.v4.runtime.tree;
import org.antlr.v4.runtime.Token;
public class TerminalNodeImpl implements TerminalNode {private final Token symbol;
 public TerminalNodeImpl(Token symbol){this.symbol=symbol;}
 @Override public Token getSymbol(){return symbol;}
 @Override public int getChildCount(){return 0;}
 @Override public ParseTree getChild(int i){throw new IndexOutOfBoundsException(i);}
}
