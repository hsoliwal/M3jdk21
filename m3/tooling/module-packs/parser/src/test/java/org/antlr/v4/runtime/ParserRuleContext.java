// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.antlr.v4.runtime;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
public class ParserRuleContext implements ParseTree {
 public Object exception;
 private final int id; private final Token start,stop; private final List<ParseTree> children=new ArrayList<>();
 public ParserRuleContext(int id,Token start,Token stop){this.id=id;this.start=start;this.stop=stop;}
 public int getRuleIndex(){return id;}
 public Token getStart(){return start;}
 public Token getStop(){return stop;}
 public void add(ParseTree child){children.add(child);}
 @Override public int getChildCount(){return children.size();}
 @Override public ParseTree getChild(int index){return children.get(index);}
}
