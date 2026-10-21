// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.antlr.v4.runtime;
public final class CommonToken implements Token {
 private final int type,start,stop; private final CharStream source;
 public CommonToken(int type,int start,int stop,CharStream source){this.type=type;this.start=start;this.stop=stop;this.source=source;}
 @Override public int getStartIndex(){return start;}
 @Override public int getStopIndex(){return stop;}
 @Override public int getType(){return type;}
 @Override public CharStream getInputStream(){return source;}
}
