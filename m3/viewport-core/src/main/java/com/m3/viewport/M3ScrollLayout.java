/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Derived by recipe (m3/docs/viewport-everywhere/recipe) from hsoliwal/eclipse.platform.swt 553eefd41d9961d303c41e0e4e181712e64a1e45,
 * ViewportScrollLayout.java (EPL-2.0 at source; authority: m3/docs/viewport-everywhere/RELICENSE_RECORD.md).
 */
package com.m3.viewport;

/**
 * Package-local compatibility adapter for the canonical viewport scrollbar
 * fixed-point solver in {@link M3ViewportRuntime}.
 */
public final class M3ScrollLayout {
	static final int AUTO = M3ViewportRuntime.AUTO;
	static final int ALWAYS = M3ViewportRuntime.ALWAYS;
	static final int NEVER = M3ViewportRuntime.NEVER;

	public record Result (
			boolean horizontalVisible,
			boolean verticalVisible,
			boolean cornerVisible,
			int bodyWidth,
			int bodyHeight,
			int headerWidth,
			int headerHeight,
			int visibleRows,
			long logicalRows,
			long estimatedContentHeight,
			long logicalContentWidth) {
	}

	private M3ScrollLayout () {
	}

	public static Result solve (
			int outerWidth,
			int outerHeight,
			int headerHeight,
			long logicalRows,
			int sampleRowHeight,
			long logicalContentWidth,
			int horizontalBarHeight,
			int verticalBarWidth,
			int horizontalPolicy,
			int verticalPolicy) {
		M3ViewportRuntime.RowLayout layout = M3ViewportRuntime.solveRows (
				outerWidth,
				outerHeight,
				headerHeight,
				logicalRows,
				sampleRowHeight,
				logicalContentWidth,
				horizontalBarHeight,
				verticalBarWidth,
				horizontalPolicy,
				verticalPolicy);
		return new Result (
				layout.horizontalVisible (),
				layout.verticalVisible (),
				layout.cornerVisible (),
				layout.bodyWidth (),
				layout.bodyHeight (),
				layout.headerWidth (),
				layout.headerHeight (),
				layout.visibleRows (),
				layout.logicalRows (),
				layout.estimatedContentHeight (),
				layout.logicalContentWidth ());
	}
}
