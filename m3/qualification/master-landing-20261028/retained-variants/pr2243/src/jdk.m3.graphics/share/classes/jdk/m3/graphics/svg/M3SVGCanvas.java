package jdk.m3.graphics.svg;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3SVGCanvas {
    static { M3Library.staticLoad(); }
    /**
     *  Returns a new canvas that will generate SVG commands from its draw calls, and send
     *  them to the provided stream. Ownership of the stream is not transfered, and it must
     *  remain valid for the lifetime of the returned canvas.
     *
     *  The canvas may buffer some drawing calls, so the output is not guaranteed to be valid
     *  or complete until the canvas instance is deleted.
     *
     * @param bounds              defines an initial SVG viewport (viewBox attribute on the root SVG element).
     * @param out                 stream SVG commands will be written to
     * @return                    new Canvas
     */
    public static M3Canvas make(M3Rect bounds, M3WStream out) {
        return make(bounds, out, false, true);
    }

    /**
     *  Returns a new canvas that will generate SVG commands from its draw calls, and send
     *  them to the provided stream. Ownership of the stream is not transfered, and it must
     *  remain valid for the lifetime of the returned canvas.
     *
     *  The canvas may buffer some drawing calls, so the output is not guaranteed to be valid
     *  or complete until the canvas instance is deleted.
     *
     * @param bounds              defines an initial SVG viewport (viewBox attribute on the root SVG element).
     * @param out                 stream SVG commands will be written to
     * @param convertTextToPaths  emit text as &lt;path&gt;s
     * @param prettyXML           add newlines and tabs in output
     * @return                    new Canvas
     */
    public static M3Canvas make(M3Rect bounds, M3WStream out, boolean convertTextToPaths, boolean prettyXML) {
        M3Stats.onNativeCall();
        long ptr = _nMake(bounds._left, bounds._top, bounds._right, bounds._bottom, M3Native.getPtr(out), 0 | (convertTextToPaths ? 1 : 0) | (prettyXML ? 0 : 2));
        return new M3Canvas(ptr, true, out);
    }

    public static native long _nMake(float left, float top, float right, float bottom, long wstreamPtr, int flags);
}
