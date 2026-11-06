/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.swing.testkit;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * Self-contained JUnit Platform entry point of the port (M3jdk21 is standalone): runs the Jupiter tests on the
 * Platform and Jupiter engine compiled from {@code m3/vendor/third_party} sources, no console launcher jar.
 * Prints the console launcher's summary lines ({@code [ N tests found ]}, successful, failed) and the failures.
 * <p>
 * usage: {@code M3JUnitLauncher [--select-package <name>]... [--select-class <name>]... [--scan <test classes dir>]}
 * </p>
 */
public final class M3JUnitLauncher {
	private M3JUnitLauncher() {
	}

	public static void main(String[] args) {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectors(args)).build();
		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.execute(request, listener);
		TestExecutionSummary s = listener.getSummary();
		PrintWriter out = new PrintWriter(System.out, true);
		s.printFailuresTo(out, 12);
		out.printf("[%10d tests found           ]%n[%10d tests successful      ]%n[%10d tests failed          ]%n", s.getTestsFoundCount(),
				s.getTestsSucceededCount(), s.getTotalFailureCount());
		out.flush();
		System.exit(s.getTotalFailureCount() == 0 && s.getTestsFoundCount() > 0 ? 0 : 1);
	}

	static List<DiscoverySelector> selectors(String[] args) {
		List<DiscoverySelector> out = new ArrayList<>();
		for (int i = 0; i + 1 < args.length; i += 2) {
			switch (args[i]) {
			case "--select-package": out.add(DiscoverySelectors.selectPackage(args[i + 1])); break;
			case "--select-class": out.add(DiscoverySelectors.selectClass(args[i + 1])); break;
			case "--scan": out.addAll(DiscoverySelectors.selectClasspathRoots(Set.of(Path.of(args[i + 1])))); break;
			default: throw new IllegalArgumentException("unknown option " + args[i]);
			}
		}
		return out;
	}
}
