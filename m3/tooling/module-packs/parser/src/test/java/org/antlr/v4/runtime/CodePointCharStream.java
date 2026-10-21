// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.antlr.v4.runtime;
import org.antlr.v4.runtime.misc.Interval;
public class CodePointCharStream implements CharStream {
 private final String value; public CodePointCharStream(String value){this.value=value;}
 @Override public int size(){return value.codePointCount(0,value.length());}
 @Override public String getText(Interval interval){return value.substring(value.offsetByCodePoints(0,interval.a),value.offsetByCodePoints(0,interval.b+1));}
}
