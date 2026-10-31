package jdk.m3.graphics.shaper;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public interface M3RunHandler {
    /**
     * Called when beginning a line.
     */
    void beginLine();

    /**
     * Called once for each run in a line. Can compute baselines and offsets.
     */
    void runInfo(M3RunInfo info);

    /**
     * Called after all {@link #runInfo} calls for a line.
     */
    void commitRunInfo();

    /**
     * Called for each run in a line after {@link #commitRunInfo}.
     *
     * @return  an offset to add to every position
     */
    M3Point runOffset(M3RunInfo info);

    /**
     * <p>Called for each run in a line after {@link #runOffset}.</p>
     *
     * <p>WARN positions are reported from the start of the line, not run, only in Shaper.makeCoreText https://bugs.chromium.org/p/skia/issues/detail?id=10898</p>
     *
     * @param positions  put glyphs[i] at positions[i]
     * @param clusters   clusters[i] is an utf-16 offset starting run which produced glyphs[i]
     */
    void commitRun(M3RunInfo info, short[] glyphs, M3Point[] positions, int[] clusters);

    /**
     * Called when ending a line.
     */
    void commitLine();
}