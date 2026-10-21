// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package org.antlr.v4.runtime;
import org.antlr.v4.runtime.misc.Interval;
public interface CharStream { int size(); String getText(Interval interval); }
