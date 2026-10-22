/****************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.nebula.cwt.svg;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.nebula.cwt.test.AbstractVTestCase;
import org.eclipse.swt.graphics.Point;

public class SvgLoaderLengthTests extends AbstractVTestCase {

	private Method parseLength;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		parseLength = SvgLoader.class.getDeclaredMethod("parseLength", String.class, String.class);
		parseLength.setAccessible(true);
	}

	public void testPlainPixelsAndDefaultValueRemainUnchanged() throws Exception {
		assertEquals(12.5f, parse("12.5"), 0.0f);
		assertEquals(12.5f, parse("12.5px"), 0.0f);
		assertEquals(7.0f, parse(null, "7px"), 0.0f);
	}

	public void testAbsoluteDpiUnitsUseHorizontalDisplayDpi() throws Exception {
		final Point[] dpi = new Point[1];
		syncExec(() -> dpi[0] = getDisplay().getDPI());

		assertEquals(2.0f * dpi[0].x, parse("2in"), 0.0f);
		assertEquals(2.0f * dpi[0].x * 0.393700787f, parse("2cm"), 0.0f);
		assertEquals(2.0f * dpi[0].x * 0.0393700787f, parse("2mm"), 0.0f);
	}

	public void testUnsupportedContextDependentUnitsRemainUnsupported() throws Exception {
		assertUnsupported("50%", "TODO parseLength: %");
		assertUnsupported("2em", "TODO parseLength: em");
		assertUnsupported("2ex", "TODO parseLength: ex");
		assertUnsupported("2pc", "TODO parseLength: pc");
		assertUnsupported("2pt", "TODO parseLength: pt");
	}

	private float parse(String value) throws Exception {
		return parse(value, null);
	}

	private float parse(String value, String defaultValue) throws Exception {
		try {
			return ((Float) parseLength.invoke(null, value, defaultValue)).floatValue();
		} catch (InvocationTargetException failure) {
			throw unwrap(failure);
		}
	}

	private void assertUnsupported(String value, String message) throws Exception {
		try {
			parse(value);
			fail("Expected UnsupportedOperationException for " + value);
		} catch (UnsupportedOperationException expected) {
			assertEquals(message, expected.getMessage());
		}
	}

	private static Exception unwrap(InvocationTargetException failure) throws Exception {
		Throwable cause = failure.getCause();
		if (cause instanceof Exception exception) {
			return exception;
		}
		if (cause instanceof Error error) {
			throw error;
		}
		throw new AssertionError(cause);
	}
}
