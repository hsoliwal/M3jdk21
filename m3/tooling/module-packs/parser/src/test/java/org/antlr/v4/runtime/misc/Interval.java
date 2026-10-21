// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.antlr.v4.runtime.misc;
public final class Interval { public final int a,b; private Interval(int a,int b){this.a=a;this.b=b;} public static Interval of(int a,int b){return new Interval(a,b);} }
