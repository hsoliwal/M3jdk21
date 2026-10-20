/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.swing.event;

import java.util.EventListener;

/**
 * Told after each frame tick of {@link javax.swing.M3Synchronizer} (M3JDK, brief rule 28; the Swing side of SWT's
 * {@code FrameListener}): the model runnables queued at {@code M3Synchronizer.MODEL} have run, all together, and
 * the views will paint the result in the next RepaintManager pass.
 *
 * @see javax.swing.M3Synchronizer#addFrameListener
 */
public interface FrameListener extends EventListener {

    /**
     * A frame was applied. Runs on the event dispatch thread.
     *
     * @param generation the frame generation, {@link javax.swing.M3Synchronizer#getFrameGeneration()}
     */
    void frameApplied(long generation);
}
