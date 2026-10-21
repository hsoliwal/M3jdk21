// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.antlr.v4.runtime;
import org.antlr.v4.runtime.misc.Interval;
public class ANTLRInputStream implements CharStream {
 private final String value; public ANTLRInputStream(String value){this.value=value;}
 @Override public int size(){return value.length();}
 @Override public String getText(Interval interval){return value.substring(interval.a,interval.b+1);}
}
