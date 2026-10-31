/*
 * Copyright (c) 2012-2017 The ANTLR Project. All rights reserved.
 * Use of this file is governed by the BSD 3-clause license that
 * can be found in the LICENSE.txt file in the project root.
 */

package com.m3.lib.runtime.tree.xpath;

import com.m3.lib.runtime.BaseErrorListener;
import com.m3.lib.runtime.RecognitionException;
import com.m3.lib.runtime.Recognizer;

public class XPathLexerErrorListener extends BaseErrorListener {
	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
							int line, int charPositionInLine, String msg,
							RecognitionException e)
	{
	}
}
