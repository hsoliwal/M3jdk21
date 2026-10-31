/*
 * Copyright (c) 2012-2017 The ANTLR Project. All rights reserved.
 * Use of this file is governed by the BSD 3-clause license that
 * can be found in the LICENSE.txt file in the project root.
 */

package com.m3.lib.runtime.tree;

import com.m3.lib.runtime.Token;

public interface TerminalNode extends ParseTree {
	Token getSymbol();
}
