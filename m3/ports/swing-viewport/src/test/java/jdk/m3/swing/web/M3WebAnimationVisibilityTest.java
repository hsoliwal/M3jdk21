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
package jdk.m3.swing.web;

import java.lang.reflect.Field;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import jdk.m3.swing.web.model.M3AnimvalueModel;
import jdk.m3.swing.web.model.M3WebStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercises the existing component clock on the real EDT without painting or a window. */
public final class M3WebAnimationVisibilityTest {
 private M3AnimvalueModel model;
 private M3WebComponent<M3AnimvalueModel> widget;
 private Timer timer;
 private static int widgetsCreated, widgetsClosed, forcedStops;

 @FunctionalInterface private interface Checked { void run() throws Exception; }
 private static void need(boolean condition, String message) {
  if (!condition) throw new AssertionError(message);
 }
 private static void edt(Checked action) throws Exception {
  if (SwingUtilities.isEventDispatchThread()) { action.run(); return; }
  Throwable[] failure = { null };
  SwingUtilities.invokeAndWait(() -> {
   try { action.run(); } catch (Throwable problem) { failure[0] = problem; }
  });
  if (failure[0] instanceof Exception problem) throw problem;
  if (failure[0] instanceof Error problem) throw problem;
  if (failure[0] != null) throw new AssertionError(failure[0]);
 }
 private static Object field(M3WebComponent<?> component, String name) throws Exception {
  Field field = M3WebComponent.class.getDeclaredField(name);
  field.setAccessible(true);
  return field.get(component);
 }
 private void openWidget(int mode) throws Exception {
  edt(() -> {
   model = new M3AnimvalueModel("animation-proof", 0, mode, 0, 0);
   widget = new M3WebComponent<>(model);
   widgetsCreated++;
   widget.setSize(360, 240);
   widget.setPartVisible(true);
   timer = (Timer) field(widget, "timer");
  });
 }
 private int phase() throws Exception {
  int[] result = { 0 };
  edt(() -> result[0] = model.snapshot()[8]);
  return result[0];
 }
 private void startClock() throws Exception {
  edt(() -> {
   model.load("spinning");
   need(model.isAnimating() && timer.isRunning(), "SWING_START_PRECONDITION");
  });
  Thread.sleep(80);
  need(phase() != 0, "ACTUAL_EDT_CLOCK_PRECONDITION");
 }
 @BeforeEach public void setup() throws Exception { openWidget(M3AnimvalueModel.SPINNER); }
 @AfterEach public void closeWidget() throws Exception {
  edt(() -> {
   if (widget == null) return;
   try {
    widget.setPartVisible(false);
    boolean retained = timer.isRunning();
    if (retained) { forcedStops++; timer.stop(); }
    need(!retained, "OWNED_SWING_TIMER_RETAINED");
   } finally {
    widgetsClosed++;
    widget = null;
    model = null;
    timer = null;
   }
  });
 }
 @Test public void visibleClockAdvances() throws Exception {
  startClock();
  int before = phase();
  Thread.sleep(80);
  need(phase() != before, "VISIBLE_SWING_CLOCK_STALLED");
 }
 @Test public void hiddenClockStable() throws Exception {
  startClock();
  edt(() -> widget.setPartVisible(false));
  int before = phase();
  Thread.sleep(80);
  need(phase() == before, "HIDDEN_SWING_PHASE_CHANGED");
 }
 @Test public void idleHiddenIntervalResumes() throws Exception {
  startClock();
  edt(() -> widget.setPartVisible(false));
  int before = phase();
  Thread.sleep(80);
  edt(() -> {
   need(!(boolean) field(widget, "pending"), "IDLE_SWING_PENDING_PRECONDITION");
   widget.setPartVisible(true);
  });
  Thread.sleep(80);
  need(phase() != before, "IDLE_SWING_RESUME_NO_PHASE_ADVANCE");
 }
 @Test public void pendingHiddenDeltaResumes() throws Exception {
  startClock();
  edt(() -> widget.setPartVisible(false));
  Thread.sleep(80);
  int before = phase();
  edt(() -> {
   model.setState(-1, M3WebStates.ERROR, true);
   need((boolean) field(widget, "pending"), "HIDDEN_SWING_DELTA_PRECONDITION");
   widget.setPartVisible(true);
  });
  Thread.sleep(80);
  need(phase() != before, "PENDING_SWING_RESUME_STALLED");
 }
 @Test public void nonAnimatingNeverStarts() throws Exception {
  closeWidget();
  openWidget(M3AnimvalueModel.TRANSITION);
  edt(() -> {
   model.load("transition");
   widget.setPartVisible(false);
   widget.setPartVisible(true);
   need(!model.isAnimating() && !timer.isRunning(), "IDLE_SWING_STARTED");
  });
  Thread.sleep(80);
  need(phase() == 0, "IDLE_SWING_PHASE_CHANGED");
 }
 @Test public void repeatedVisibilityUsesSameTimer() throws Exception {
  startClock();
  edt(() -> {
   Timer original = timer;
   for (int i = 0; i < 20; i++) widget.setPartVisible(true);
   need(field(widget, "timer") == original && original.isRunning()
     && original.getActionListeners().length == 1, "SWING_TIMER_IDENTITY_OR_COUNT");
  });
 }
}
