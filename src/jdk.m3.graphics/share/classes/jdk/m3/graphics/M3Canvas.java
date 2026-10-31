package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

public class M3Canvas extends M3Managed {
    static { M3Library.staticLoad(); }

    public final Object _owner;
    public M3Canvas(long ptr, boolean managed, Object owner) {
        super(ptr, _FinalizerHolder.PTR, managed);
        this._owner = owner;
    }

    /**
     * <p>Constructs a canvas that draws into bitmap.
     * Sets default pixel geometry in constructed Surface.</p>
     *
     * <p>Bitmap is copied so that subsequently editing bitmap will not affect
     * constructed Canvas.</p>
     *
     * <p>May be deprecated in the future.</p>
     *
     * @param bitmap  width, height, ColorType, ColorAlphaType, and pixel
     *                storage of raster surface
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_copy_const_SkBitmap">https://fiddle.skia.org/c/@Canvas_copy_const_SkBitmap</a>
     */
    public M3Canvas(M3Bitmap bitmap) {
        this(bitmap, new M3SurfaceProps());
    }

    /**
     * <p>Constructs a canvas that draws into bitmap.
     * Use props to match the device characteristics, like LCD striping.</p>
     *
     * <p>Bitmap is copied so that subsequently editing bitmap will not affect
     * constructed Canvas.</p>
     *
     * @param bitmap  width, height, ColorType, ColorAlphaType, and pixel
     *                storage of raster surface
     * @param surfaceProps   order and orientation of RGB striping; and whether to use
     *                       device independent fonts
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_const_SkBitmap_const_SkSurfaceProps">https://fiddle.skia.org/c/@Canvas_const_SkBitmap_const_SkSurfaceProps</a>
     */
    public M3Canvas(M3Bitmap bitmap, M3SurfaceProps surfaceProps) {
        this(_nMakeFromBitmap(bitmap._ptr, surfaceProps._getFlags(), surfaceProps._pixelGeometry.ordinal()), true, bitmap);
        M3Stats.onNativeCall();
        M3ReferenceUtil.reachabilityFence(bitmap);
    }

    /**
     * Returns the SurfaceProps associated with the canvas (i.e., at the base of the layer
     * stack).
     *
     * @return  SurfaceProps
     */
    public M3SurfaceProps getBaseProps() {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            return _nGetBaseProps(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns the SurfaceProps associated with the canvas that are currently active (i.e., at
     * the top of the layer stack). This can differ from {getBaseProps()} depending on the flags
     * passed to saveLayer.
     *
     * @return  SurfaceProps active in the current/top layer
     */
    public M3SurfaceProps getTopProps() {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            return _nGetTopProps(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns bounds of clip, unaffected by Matrix.</p>
     *
     * <p>If clip is empty, return empty bounds, where all sides equal zero.</p>
     *
     * @return bounds of clip in base device coordinates
     * @see <a href="https://fiddle.skia.org/c/@Canvas_getDeviceClipBounds">https://fiddle.skia.org/c/@Canvas_getDeviceClipBounds</a>
     */
    public M3IRect getDeviceClipBounds() {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            return _nGetDeviceClipBounds(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns bounds of clip, transformed by inverse of Matrix.</p>
     *
     * <p>If clip is empty, return empty bounds, where all sides equal zero.</p>
     *
     * <p>Bounds returned is outset by one to account for partial pixel coverage if clip is anti-aliased.</p>
     *
     * @return bounds of clip in local coordinates
     * @see <a href="https://fiddle.skia.org/c/@Canvas_getLocalClipBounds">https://fiddle.skia.org/c/@Canvas_getLocalClipBounds</a>
     */
    public M3Rect getLocalClipBounds() {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            return _nGetLocalClipBounds(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Surface getSurface() {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            long ptr = _nGetSurface(_ptr);
            return ptr == 0 ? null : new M3Surface(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Canvas drawPoint(float x, float y, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert paint != null : "Can’t drawPoint with paint == null";
        M3Stats.onNativeCall();
        _nDrawPoint(_ptr, x, y, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>Draws pts using clip, Matrix and Paint paint.</p>
     *
     * <p>The shape of point drawn depends on paint
     * PaintStrokeCap. If paint is set to {@link PaintStrokeCap#ROUND}, each point draws a
     * circle of diameter Paint stroke width. If paint is set to {@link PaintStrokeCap#SQUARE}
     * or {@link PaintStrokeCap#BUTT}, each point draws a square of width and height
     * Paint stroke width.</p>
     *
     * <p>Each line segment respects paint PaintStrokeCap and Paint stroke width.
     * PaintMode is ignored, as if were set to {@link PaintMode#STROKE}.</p>
     *
     * <p>Always draws each element one at a time; is not affected by
     * PaintStrokeJoin, and unlike drawPath(), does not create a mask from all points
     * and lines before drawing.</p>
     *
     * @param coords array of points to draw
     * @param paint  stroke, blend, color, and so on, used to draw
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawPoints">https://fiddle.skia.org/c/@Canvas_drawPoints</a>
     */
    public M3Canvas drawPoints(M3Point[] coords, M3Paint paint) {
        return drawPoints(M3Point.flattenArray(coords), paint);
    }

    /**
     * <p>Draws pts using clip, Matrix and Paint paint.</p>
     *
     * <p>The shape of point drawn depends on paint
     * PaintStrokeCap. If paint is set to {@link PaintStrokeCap#ROUND}, each point draws a
     * circle of diameter Paint stroke width. If paint is set to {@link PaintStrokeCap#SQUARE}
     * or {@link PaintStrokeCap#BUTT}, each point draws a square of width and height
     * Paint stroke width.</p>
     *
     * <p>Each line segment respects paint PaintStrokeCap and Paint stroke width.
     * PaintMode is ignored, as if were set to {@link PaintMode#STROKE}.</p>
     *
     * <p>Always draws each element one at a time; is not affected by
     * PaintStrokeJoin, and unlike drawPath(), does not create a mask from all points
     * and lines before drawing.</p>
     *
     * @param coords array of points to draw
     * @param paint  stroke, blend, color, and so on, used to draw
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawPoints">https://fiddle.skia.org/c/@Canvas_drawPoints</a>
     */
    public M3Canvas drawPoints(float[] coords, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert coords != null : "Can’t drawPoints with coords == null";
        assert paint != null : "Can’t drawPoints with paint == null";
        M3Stats.onNativeCall();
        _nDrawPoints(_ptr, 0 /* SkCanvas::PointMode::kPoints_PointMode */, coords, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>Draws pts using clip, Matrix and Paint paint.</p>
     *
     * <p>Each pair of points draws a line segment.
     * One line is drawn for every two points; each point is used once. If count is odd,
     * the final point is ignored.</p>
     *
     * <p>Each line segment respects paint PaintStrokeCap and Paint stroke width.
     * PaintMode is ignored, as if were set to {@link PaintMode#STROKE}.</p>
     *
     * <p>Always draws each element one at a time; is not affected by
     * PaintStrokeJoin, and unlike drawPath(), does not create a mask from all points
     * and lines before drawing.</p>
     *
     * @param coords array of points to draw
     * @param paint  stroke, blend, color, and so on, used to draw
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawPoints">https://fiddle.skia.org/c/@Canvas_drawPoints</a>
     */
    public M3Canvas drawLines(M3Point[] coords, M3Paint paint) {
        return drawLines(M3Point.flattenArray(coords), paint);
    }

    /**
     * <p>Draws pts using clip, Matrix and Paint paint.</p>
     *
     * <p>Each pair of points draws a line segment.
     * One line is drawn for every two points; each point is used once. If count is odd,
     * the final point is ignored.</p>
     *
     * <p>Each line segment respects paint PaintStrokeCap and Paint stroke width.
     * PaintMode is ignored, as if were set to {@link PaintMode#STROKE}.</p>
     *
     * <p>Always draws each element one at a time; is not affected by
     * PaintStrokeJoin, and unlike drawPath(), does not create a mask from all points
     * and lines before drawing.</p>
     *
     * @param coords array of points to draw
     * @param paint  stroke, blend, color, and so on, used to draw
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawPoints">https://fiddle.skia.org/c/@Canvas_drawPoints</a>
     */
    public M3Canvas drawLines(float[] coords, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert coords != null : "Can’t drawLines with coords == null";
        assert paint != null : "Can’t drawLines with paint == null";
        M3Stats.onNativeCall();
        _nDrawPoints(_ptr, 1 /* SkCanvas::PointMode::kLines_PointMode */, coords, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>Draws pts using clip, Matrix and Paint paint.</p>
     *
     * <p>Each adjacent pair of points draws a line segment.
     * count minus one lines are drawn; the first and last point are used once.</p>
     *
     * <p>Each line segment respects paint PaintStrokeCap and Paint stroke width.
     * PaintMode is ignored, as if were set to {@link PaintMode#STROKE}.</p>
     *
     * <p>Always draws each element one at a time; is not affected by
     * PaintStrokeJoin, and unlike drawPath(), does not create a mask from all points
     * and lines before drawing.</p>
     *
     * @param coords array of points to draw
     * @param paint  stroke, blend, color, and so on, used to draw
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawPoints">https://fiddle.skia.org/c/@Canvas_drawPoints</a>
     */
    public M3Canvas drawPolygon(M3Point[] coords, M3Paint paint) {
        return drawPolygon(M3Point.flattenArray(coords), paint);
    }

    /**
     * <p>Draws pts using clip, Matrix and Paint paint.</p>
     *
     * <p>Each adjacent pair of points draws a line segment.
     * count minus one lines are drawn; the first and last point are used once.</p>
     *
     * <p>Each line segment respects paint PaintStrokeCap and Paint stroke width.
     * PaintMode is ignored, as if were set to {@link PaintMode#STROKE}.</p>
     *
     * <p>Always draws each element one at a time; is not affected by
     * PaintStrokeJoin, and unlike drawPath(), does not create a mask from all points
     * and lines before drawing.</p>
     *
     * @param coords array of points to draw
     * @param paint  stroke, blend, color, and so on, used to draw
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawPoints">https://fiddle.skia.org/c/@Canvas_drawPoints</a>
     */
    public M3Canvas drawPolygon(float[] coords, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert coords != null : "Can’t drawPolygon with coords == null";
        assert paint != null : "Can’t drawPolygon with paint == null";
        M3Stats.onNativeCall();
        _nDrawPoints(_ptr, 2 /* SkCanvas::PointMode::kPolygon_PointMode */, coords, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawLine(float x0, float y0, float x1, float y1, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert paint != null : "Can’t drawLine with paint == null";
        M3Stats.onNativeCall();
        _nDrawLine(_ptr, x0, y0, x1, y1, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean includeCenter, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert paint != null : "Can’t drawArc with paint == null";
        M3Stats.onNativeCall();
        _nDrawArc(_ptr, left, top, right, bottom, startAngle, sweepAngle, includeCenter, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawRect(M3Rect r, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t drawRect with r == null";
        assert paint != null : "Can’t drawRect with paint == null";
        M3Stats.onNativeCall();
        _nDrawRect(_ptr, r._left, r._top, r._right, r._bottom, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawOval(M3Rect r, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t drawOval with r == null";
        assert paint != null : "Can’t drawOval with paint == null";
        M3Stats.onNativeCall();
        _nDrawOval(_ptr, r._left, r._top, r._right, r._bottom, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawCircle(float x, float y, float radius, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert paint != null : "Can’t drawCircle with paint == null";
        M3Stats.onNativeCall();
        _nDrawOval(_ptr, x - radius, y - radius, x + radius, y + radius, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawRRect(M3RRect r, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t drawRRect with r == null";
        assert paint != null : "Can’t drawRRect with paint == null";
        M3Stats.onNativeCall();
        _nDrawRRect(_ptr, r._left, r._top, r._right, r._bottom, r._radii, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public boolean quickReject(M3Rect r) {
        try {
            assert _ptr != 0 : "Canvas is closed";
            assert r != null : "Can’t quickReject with r == null";
            M3Stats.onNativeCall();
            return _nQuickReject(_ptr, r._left, r._top, r._right, r._bottom);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean quickReject(M3Path path) {
        try {
            assert _ptr != 0 : "Canvas is closed";
            assert path != null : "Can’t quickReject with path == null";
            M3Stats.onNativeCall();
            return _nQuickRejectPath(_ptr, M3Native.getPtr(path));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(path);
        }
    }

    public M3Canvas drawDRRect(M3RRect outer, M3RRect inner, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert outer != null : "Can’t drawDRRect with outer == null";
        assert inner != null : "Can’t drawDRRect with inner == null";
        assert paint != null : "Can’t drawDRRect with paint == null";
        M3Stats.onNativeCall();
        _nDrawDRRect(_ptr, outer._left, outer._top, outer._right, outer._bottom, outer._radii, inner._left, inner._top, inner._right, inner._bottom, inner._radii, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawRectShadow(M3Rect r, float dx, float dy, float blur, int color) {
        return drawRectShadow(r, dx, dy, blur, 0f, color);
    }

    public M3Canvas drawRectShadow(M3Rect r, float dx, float dy, float blur, float spread, int color) {
        assert r != null : "Can’t drawRectShadow with r == null";
        M3Rect insides = r.inflate(-1);
        if (!insides.isEmpty()) {
            save();
            if (insides instanceof M3RRect)
                clipRRect((M3RRect) insides, M3ClipMode.DIFFERENCE);
            else
                clipRect(insides, M3ClipMode.DIFFERENCE);
            drawRectShadowNoclip(r, dx, dy, blur, spread, color);
            restore();
        } else
            drawRectShadowNoclip(r, dx, dy, blur, spread, color);
        return this;
    }

    public M3Canvas drawRectShadowNoclip(M3Rect r, float dx, float dy, float blur, float spread, int color) {
        assert r != null : "Can’t drawRectShadow with r == null";
        M3Rect outline = r.inflate(spread);
        try (M3ImageFilter f = M3ImageFilter.makeDropShadowOnly(dx, dy, blur / 2f, blur / 2f, color);
             M3Paint p = new M3Paint();) {
            p.setImageFilter(f);
            if (outline instanceof M3RRect)
                drawRRect((M3RRect) outline, p);
            else
                drawRect(outline, p);
        }
        return this;
    }

    public M3Canvas drawPath(M3Path path, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert path != null : "Can’t drawPath with path == null";
        assert paint != null : "Can’t drawPath with paint == null";
        M3Stats.onNativeCall();
        _nDrawPath(_ptr, M3Native.getPtr(path), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(path);
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * Draws path and closes it immediately
     */
    public M3Canvas drawPathOnce(M3Path path, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert path != null : "Can’t drawPath with path == null";
        assert paint != null : "Can’t drawPath with paint == null";
        path.setVolatile(true);
        M3Stats.onNativeCall();
        _nDrawPath(_ptr, M3Native.getPtr(path), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        path.close();
        return this;
    }

    public M3Canvas drawImage(M3Image image, float left, float top) {
        return drawImageRect(image, M3Rect.makeWH(image.getWidth(), image.getHeight()), M3Rect.makeXYWH(left, top, image.getWidth(), image.getHeight()), M3SamplingMode.DEFAULT, null, true);
    }

    public M3Canvas drawImage(M3Image image, float left, float top, M3Paint paint) {
        return drawImageRect(image, M3Rect.makeWH(image.getWidth(), image.getHeight()), M3Rect.makeXYWH(left, top, image.getWidth(), image.getHeight()), M3SamplingMode.DEFAULT, paint, true);
    }

    public M3Canvas drawImageRect(M3Image image, M3Rect dst) {
        return drawImageRect(image, M3Rect.makeWH(image.getWidth(), image.getHeight()), dst, M3SamplingMode.DEFAULT, null, true);
    }

    public M3Canvas drawImageRect(M3Image image, M3Rect dst, M3Paint paint) {
        return drawImageRect(image, M3Rect.makeWH(image.getWidth(), image.getHeight()), dst, M3SamplingMode.DEFAULT, paint, true);
    }

    public M3Canvas drawImageRect(M3Image image, M3Rect src, M3Rect dst) {
        return drawImageRect(image, src, dst, M3SamplingMode.DEFAULT, null, true);
    }

    public M3Canvas drawImageRect(M3Image image, M3Rect src, M3Rect dst, M3Paint paint) {
        return drawImageRect(image, src, dst, M3SamplingMode.DEFAULT, paint, true);
    }

    public M3Canvas drawImageRect(M3Image image, M3Rect src, M3Rect dst, M3Paint paint, boolean strict) {
        return drawImageRect(image, src, dst, M3SamplingMode.DEFAULT, paint, strict);
    }

    public M3Canvas drawImageRect(M3Image image, M3Rect src, M3Rect dst, M3SamplingMode samplingMode, M3Paint paint, boolean strict) {
        assert _ptr != 0 : "Canvas is closed";
        assert image != null : "Can’t drawImageRect with image == null";
        assert src != null : "Can’t drawImageRect with src == null";
        assert dst != null : "Can’t drawImageRect with dst == null";
        assert samplingMode != null : "Can’t drawImageRect with samplingMode == null";
        M3Stats.onNativeCall();
        _nDrawImageRect(_ptr, M3Native.getPtr(image), src._left, src._top, src._right, src._bottom, dst._left, dst._top, dst._right, dst._bottom, samplingMode._pack(), M3Native.getPtr(paint), strict);
        M3ReferenceUtil.reachabilityFence(image);
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawImageNine(M3Image image, M3IRect center, M3Rect dst, M3FilterMode filterMode, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert image != null : "Can’t drawImageNine with image == null";
        assert center != null : "Can’t drawImageNine with center == null";
        assert dst != null : "Can’t drawImageNine with dst == null";
        assert filterMode != null : "Can’t drawImageNine with filterMode == null";
        M3Stats.onNativeCall();
        _nDrawImageNine(_ptr, M3Native.getPtr(image), center._left, center._top, center._right, center._bottom, dst._left, dst._top, dst._right, dst._bottom, filterMode.ordinal(), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(image);
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawRegion(M3Region r, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t drawRegion with r == null";
        assert paint != null : "Can’t drawRegion with paint == null";
        M3Stats.onNativeCall();
        _nDrawRegion(_ptr, M3Native.getPtr(r), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(r);
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawString(String s, float x, float y, M3Font font, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert s != null : "Can’t drawString with s == null";
        assert paint != null : "Can’t drawString with paint == null";
        M3Stats.onNativeCall();
        _nDrawString(_ptr, s, x, y, M3Native.getPtr(font), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(font);
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawTextBlob(M3TextBlob blob, float x, float y, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert blob != null : "Can’t drawTextBlob with blob == null";
        assert paint != null : "Can’t drawTextBlob with paint == null";
        M3Stats.onNativeCall();
        _nDrawTextBlob(_ptr, M3Native.getPtr(blob), x, y, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(blob);
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    public M3Canvas drawTextLine(M3TextLine line, float x, float y, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert line != null : "Can’t drawTextLine with line == null";
        assert paint != null : "Can’t drawTextLine with paint == null";
        try (M3TextBlob blob = line.getTextBlob();) {
            if (blob != null)
                drawTextBlob(blob, x, y, paint);
        }
        return this;
    }

    public M3Canvas drawPicture(M3Picture picture) {
        return drawPicture(picture, null, null);
    }

    public M3Canvas drawPicture(M3Picture picture, M3Matrix33 matrix, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert picture != null : "Can’t drawPicture with picture == null";
        M3Stats.onNativeCall();
        _nDrawPicture(_ptr, M3Native.getPtr(picture), matrix == null ? null : matrix._mat, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(picture);
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>Draws a triangle mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader, the shader is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangles(M3Point[] positions, int[] colors, M3Paint paint) {
        return drawTriangles(positions, colors, null, null, M3BlendMode.MODULATE, paint);
    }

    /**
     * <p>Draws a triangle mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader and vertices does not contain texCoords, the shader
     * is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param texCoords  Point array of texture coordinates, mapping Shader to corners; may be null
     * @param indices    with which indices points should be drawn; may be null
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangles(M3Point[] positions, int[] colors, M3Point[] texCoords, short[] indices, M3Paint paint) {
        return drawTriangles(positions, colors, texCoords, indices, M3BlendMode.MODULATE, paint);
    }

    /**
     * <p>Draws a triangle mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader and vertices does not contain texCoords, the shader
     * is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param texCoords  Point array of texture coordinates, mapping Shader to corners; may be null
     * @param indices    with which indices points should be drawn; may be null
     * @param mode       combines vertices colors with Shader, if both are present
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangles(M3Point[] positions, int[] colors, M3Point[] texCoords, short[] indices, M3BlendMode mode, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert positions != null : "Can’t drawTriangles with positions == null";
        assert positions.length % 3 == 0 : "Expected positions.length % 3 == 0, got: " + positions.length;
        assert colors == null || colors.length == positions.length : "Expected colors.length == positions.length, got: " + colors.length + " != " + positions.length;
        assert texCoords == null || texCoords.length == positions.length : "Expected texCoords.length == positions.length, got: " + texCoords.length + " != " + positions.length;
        assert paint != null : "Can’t drawTriangles with paint == null";
        M3Stats.onNativeCall();
        _nDrawVertices(_ptr, 0 /* kTriangles_VertexMode */, M3Point.flattenArray(positions), colors, M3Point.flattenArray(texCoords), indices, mode.ordinal(), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>Draws a triangle strip mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader, the shader is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangleStrip(M3Point[] positions, int[] colors, M3Paint paint) {
        return drawTriangleStrip(positions, colors, null, null, M3BlendMode.MODULATE, paint);
    }

    /**
     * <p>Draws a triangle strip mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader and vertices does not contain texCoords, the shader
     * is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param texCoords  Point array of texture coordinates, mapping Shader to corners; may be null
     * @param indices    with which indices points should be drawn; may be null
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangleStrip(M3Point[] positions, int[] colors, M3Point[] texCoords, short[] indices, M3Paint paint) {
        return drawTriangleStrip(positions, colors, texCoords, indices, M3BlendMode.MODULATE, paint);
    }

    /**
     * <p>Draws a triangle strip mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader and vertices does not contain texCoords, the shader
     * is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param texCoords  Point array of texture coordinates, mapping Shader to corners; may be null
     * @param indices    with which indices points should be drawn; may be null
     * @param mode       combines vertices colors with Shader, if both are present
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangleStrip(M3Point[] positions, int[] colors, M3Point[] texCoords, short[] indices, M3BlendMode mode, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert positions != null : "Can’t drawTriangleStrip with positions == null";
        assert colors == null || colors.length == positions.length : "Expected colors.length == positions.length, got: " + colors.length + " != " + positions.length;
        assert texCoords == null || texCoords.length == positions.length : "Expected texCoords.length == positions.length, got: " + texCoords.length + " != " + positions.length;
        assert mode != null : "Can’t drawTriangles with mode == null";
        assert paint != null : "Can’t drawTriangles with paint == null";
        M3Stats.onNativeCall();
        _nDrawVertices(_ptr, 1 /* kTriangleStrip_VertexMode */, M3Point.flattenArray(positions), colors, M3Point.flattenArray(texCoords), indices, mode.ordinal(), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>Draws a triangle fan mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader, the shader is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangleFan(M3Point[] positions, int[] colors, M3Paint paint) {
        return drawTriangleFan(positions, colors, null, null, M3BlendMode.MODULATE, paint);
    }

    /**
     * <p>Draws a triangle fan mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader and vertices does not contain texCoords, the shader
     * is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param texCoords  Point array of texture coordinates, mapping Shader to corners; may be null
     * @param indices    with which indices points should be drawn; may be null
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangleFan(M3Point[] positions, int[] colors, M3Point[] texCoords, short[] indices, M3Paint paint) {
        return drawTriangleFan(positions, colors, texCoords, indices, M3BlendMode.MODULATE, paint);
    }

    /**
     * <p>Draws a triangle fan mesh, using clip and Matrix.</p>
     *
     * <p>If paint contains an Shader and vertices does not contain texCoords, the shader
     * is mapped using the vertices' positions.</p>
     *
     * <p>If vertices colors are defined in vertices, and Paint paint contains Shader,
     * BlendMode mode combines vertices colors with Shader.</p>
     *
     * @param positions  triangle mesh to draw
     * @param colors     color array, one for each corner; may be null
     * @param texCoords  Point array of texture coordinates, mapping Shader to corners; may be null
     * @param indices    with which indices points should be drawn; may be null
     * @param mode       combines vertices colors with Shader, if both are present
     * @param paint      specifies the Shader, used as Vertices texture
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices">https://fiddle.skia.org/c/@Canvas_drawVertices</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawVertices_2">https://fiddle.skia.org/c/@Canvas_drawVertices_2</a>
     */
    public M3Canvas drawTriangleFan(M3Point[] positions, int[] colors, M3Point[] texCoords, short[] indices, M3BlendMode mode, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert positions != null : "Can’t drawTriangleFan with positions == null";
        assert colors == null || colors.length == positions.length : "Expected colors.length == positions.length, got: " + colors.length + " != " + positions.length;
        assert texCoords == null || texCoords.length == positions.length : "Expected texCoords.length == positions.length, got: " + texCoords.length + " != " + positions.length;
        assert mode != null : "Can’t drawTriangleFan with mode == null";
        assert paint != null : "Can’t drawTriangleFan with paint == null";
        M3Stats.onNativeCall();
        _nDrawVertices(_ptr, 2 /* kTriangleFan_VertexMode */, M3Point.flattenArray(positions), colors, M3Point.flattenArray(texCoords), indices, mode.ordinal(), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>Draws a Coons patch: the interpolation of four cubics with shared corners,
     * associating a color, and optionally a texture SkPoint, with each corner.</p>
     *
     * <p>Coons patch uses clip and Matrix, paint Shader, ColorFilter,
     * alpha, ImageFilter, and BlendMode. If Shader is provided it is treated
     * as Coons patch texture.</p>
     *
     * <p>Point array cubics specifies four Path cubic starting at the top-left corner,
     * in clockwise order, sharing every fourth point. The last Path cubic ends at the
     * first point.</p>
     *
     * <p>Color array color associates colors with corners in top-left, top-right,
     * bottom-right, bottom-left order.</p>
     *
     *
     * @param cubics     Path cubic array, sharing common points
     * @param colors     color array, one for each corner
     * @param paint      Shader, ColorFilter, BlendMode, used to draw
     * @return           this
     *
     * @see <a href="https://fiddle.skia.org/c/4cf70f8d194867d053d7e177e5088445">https://fiddle.skia.org/c/4cf70f8d194867d053d7e177e5088445</a>
     */
    public M3Canvas drawPatch(M3Point[] cubics, int[] colors, M3Paint paint) {
        return drawPatch(cubics, colors, null, paint);
    }

    /**
     * <p>Draws a Coons patch: the interpolation of four cubics with shared corners,
     * associating a color, and optionally a texture SkPoint, with each corner.</p>
     *
     * <p>Coons patch uses clip and Matrix, paint Shader, ColorFilter,
     * alpha, ImageFilter, and BlendMode. If Shader is provided it is treated
     * as Coons patch texture.</p>
     *
     * <p>Point array cubics specifies four Path cubic starting at the top-left corner,
     * in clockwise order, sharing every fourth point. The last Path cubic ends at the
     * first point.</p>
     *
     * <p>Color array color associates colors with corners in top-left, top-right,
     * bottom-right, bottom-left order.</p>
     *
     * <p>If paint contains Shader, Point array texCoords maps Shader as texture to
     * corners in top-left, top-right, bottom-right, bottom-left order. If texCoords is
     * nullptr, Shader is mapped using positions (derived from cubics).</p>
     *
     * @param cubics     Path cubic array, sharing common points
     * @param colors     color array, one for each corner
     * @param texCoords  Point array of texture coordinates, mapping Shader to corners;
     *                   may be null
     * @param paint      Shader, ColorFilter, BlendMode, used to draw
     * @return           this
     *
     * @see <a href="https://fiddle.skia.org/c/4cf70f8d194867d053d7e177e5088445">https://fiddle.skia.org/c/4cf70f8d194867d053d7e177e5088445</a>
     */
    public M3Canvas drawPatch(M3Point[] cubics, int[] colors, M3Point[] texCoords, M3Paint paint) {
        return drawPatch(cubics, colors, texCoords, M3BlendMode.MODULATE, paint);
    }

    /**
     * <p>Draws a Coons patch: the interpolation of four cubics with shared corners,
     * associating a color, and optionally a texture SkPoint, with each corner.</p>
     *
     * <p>Coons patch uses clip and Matrix, paint Shader, ColorFilter,
     * alpha, ImageFilter, and BlendMode. If Shader is provided it is treated
     * as Coons patch texture; BlendMode mode combines color colors and Shader if
     * both are provided.</p>
     *
     * <p>Point array cubics specifies four Path cubic starting at the top-left corner,
     * in clockwise order, sharing every fourth point. The last Path cubic ends at the
     * first point.</p>
     *
     * <p>Color array color associates colors with corners in top-left, top-right,
     * bottom-right, bottom-left order.</p>
     *
     * <p>If paint contains Shader, Point array texCoords maps Shader as texture to
     * corners in top-left, top-right, bottom-right, bottom-left order. If texCoords is
     * nullptr, Shader is mapped using positions (derived from cubics).</p>
     *
     * @param cubics     Path cubic array, sharing common points
     * @param colors     color array, one for each corner
     * @param texCoords  Point array of texture coordinates, mapping Shader to corners;
     *                   may be null
     * @param mode       BlendMode for colors, and for Shader if paint has one
     * @param paint      Shader, ColorFilter, BlendMode, used to draw
     * @return           this
     *
     * @see <a href="https://fiddle.skia.org/c/4cf70f8d194867d053d7e177e5088445">https://fiddle.skia.org/c/4cf70f8d194867d053d7e177e5088445</a>
     */
    public M3Canvas drawPatch(M3Point[] cubics, int[] colors, M3Point[] texCoords, M3BlendMode mode, M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert cubics != null : "Can’t drawPatch with cubics == null";
        assert cubics.length == 12 : "Expected cubics.length == 12, got: " + cubics.length;
        assert colors != null : "Can’t drawPatch with colors == null";
        assert colors.length == 4 : "Expected colors.length == 4, got: " + colors.length;
        assert texCoords == null || texCoords.length == 4 : "Expected texCoords.length == 4, got: " + texCoords.length;
        assert mode != null : "Can’t drawPatch with mode == null";
        assert paint != null : "Can’t drawPatch with paint == null";
        M3Stats.onNativeCall();
        _nDrawPatch(_ptr, M3Point.flattenArray(cubics), colors, M3Point.flattenArray(texCoords), mode.ordinal(), M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>Draws Drawable drawable using clip and matrix.</p>
     *
     * <p>If Canvas has an asynchronous implementation, as is the case
     * when it is recording into Picture, then drawable will be referenced,
     * so that Drawable::draw() can be called when the operation is finalized. To force
     * immediate drawing, call Drawable::draw() instead.</p>
     *
     * @param drawable  custom struct encapsulating drawing commands
     * @return          this
     */
    public M3Canvas drawDrawable(M3Drawable drawable) {
        return drawDrawable(drawable, null);
    }

    /**
     * <p>Draws Drawable drawable using clip and matrix, offset by (x, y).</p>
     *
     * <p>If Canvas has an asynchronous implementation, as is the case
     * when it is recording into Picture, then drawable will be referenced,
     * so that Drawable::draw() can be called when the operation is finalized. To force
     * immediate drawing, call Drawable::draw() instead.</p>
     *
     * @param drawable  custom struct encapsulating drawing commands
     * @param x         offset into Canvas writable pixels on x-axis
     * @param y         offset into Canvas writable pixels on y-axis
     * @return          this
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawDrawable_2">https://fiddle.skia.org/c/@Canvas_drawDrawable_2</a>
     */
    public M3Canvas drawDrawable(M3Drawable drawable, float x, float y) {
        return drawDrawable(drawable, M3Matrix33.makeTranslate(x, y));
    }

    /**
     * <p>Draws Drawable drawable using clip and matrix, concatenated with
     * optional matrix.</p>
     *
     * <p>If Canvas has an asynchronous implementation, as is the case
     * when it is recording into Picture, then drawable will be referenced,
     * so that Drawable::draw() can be called when the operation is finalized. To force
     * immediate drawing, call Drawable::draw() instead.</p>
     *
     * @param drawable  custom struct encapsulating drawing commands
     * @param matrix    transformation applied to drawing; may be null
     * @return          this
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_drawDrawable">https://fiddle.skia.org/c/@Canvas_drawDrawable</a>
     */
    public M3Canvas drawDrawable(M3Drawable drawable, M3Matrix33 matrix) {
        assert _ptr != 0 : "Canvas is closed";
        assert drawable != null : "Can’t drawDrawable with drawable == null";
        M3Stats.onNativeCall();
        _nDrawDrawable(_ptr, M3Native.getPtr(drawable), matrix == null ? null : matrix._mat);
        M3ReferenceUtil.reachabilityFence(drawable);
        return this;
    }

    public M3Canvas drawColor(int color) {
        return drawColor(color, M3BlendMode.SRC_OVER);
    }

    public M3Canvas drawColor(int color, M3BlendMode mode) {
        assert _ptr != 0 : "Canvas is closed";
        M3Stats.onNativeCall();
        _nDrawColor(_ptr, color, mode.ordinal());
        return this;
    }

    public M3Canvas drawColor(M3Color4f color) {
        return drawColor(color, M3BlendMode.SRC_OVER);
    }

    public M3Canvas drawColor(M3Color4f color, M3BlendMode mode) {
        assert _ptr != 0 : "Canvas is closed";
        M3Stats.onNativeCall();
        _nDrawColor4f(_ptr, color._r, color._g, color._b, color._a, mode.ordinal());
        return this;
    }


    public M3Canvas clear(int color) {
        assert _ptr != 0 : "Canvas is closed";
        M3Stats.onNativeCall();
        _nClear(_ptr, color);
        return this;
    }

    public M3Canvas drawPaint(M3Paint paint) {
        assert _ptr != 0 : "Canvas is closed";
        assert paint != null : "Can’t drawPaint with paint == null";
        M3Stats.onNativeCall();
        _nDrawPaint(_ptr, M3Native.getPtr(paint));
        M3ReferenceUtil.reachabilityFence(paint);
        return this;
    }

    /**
     * <p>For SVG and PDF backends, makes a link over the rectangle to the specified URL.
     * Does nothing on other backends.</p>
     *
     * <p>The URL should be properly escaped and be valid 7-bit ASCII.</p>
     */
    public M3Canvas annotateRectWithURL(M3Rect r, String url) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t annotateRectWithURL with r == null";
        assert url != null : "Can’t annotateRectWithURL with url == null";
        M3Stats.onNativeCall();
        _nAnnotateRectWithURL(_ptr, r._left, r._top, r._right, r._bottom, url);
        return this;
    }

    /**
     * <p>For the PDF backend, associates the point with a name that {@link Canvas#annotateLinkToDestination} can make a jump to.</p>
     * <p>For the SVG backend, functions like {@link Canvas#annotateRectWithURL}.</p>
     * Does nothing on other backends.
     */
    public M3Canvas annotateNamedDestination(float x, float y, String name) {
        assert _ptr != 0 : "Canvas is closed";
        assert name != null : "Can’t annotateNamedDestination with name == null";
        M3Stats.onNativeCall();
        _nAnnotateNamedDestination(_ptr, x, y, name);
        return this;
    }

    /**
     * For the PDF backend, makes a link over the given rectangle that goes to the respective {@link Canvas#annotateNamedDestination}.
     * Does nothing on other backends.
     */
    public M3Canvas annotateLinkToDestination(M3Rect r, String name) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t annotateLinkToDestination with r == null";
        assert name != null : "Can’t annotateLinkToDestination with name == null";
        M3Stats.onNativeCall();
        _nAnnotateLinkToDestination(_ptr, r._left, r._top, r._right, r._bottom, name);
        return this;
    }

    /**
     * Replaces Matrix with matrix.
     * Unlike concat(), any prior matrix state is overwritten.
     *
     * @param matrix  matrix to copy, replacing existing Matrix
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_setMatrix">https://fiddle.skia.org/c/@Canvas_setMatrix</a>
     */
    public M3Canvas setMatrix(M3Matrix33 matrix) {
        assert _ptr != 0 : "Canvas is closed";
        assert matrix != null : "Can’t setMatrix with matrix == null";
        M3Stats.onNativeCall();
        _nSetMatrix(_ptr, matrix._mat);
        return this;
    }

    /**
     * Replaces Matrix with matrix.
     * Unlike concat(), any prior matrix state is overwritten.
     *
     * @param matrix  matrix to copy, replacing existing Matrix
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_setMatrix">https://fiddle.skia.org/c/@Canvas_setMatrix</a>
     */
    public M3Canvas setMatrix(M3Matrix44 matrix) {
        assert _ptr != 0 : "Canvas is closed";
        assert matrix != null : "Can’t setMatrix with matrix == null";
        M3Stats.onNativeCall();
        _nSetMatrix44(_ptr, matrix._mat);
        return this;
    }

    /**
     * Sets SkMatrix to the identity matrix.
     * Any prior matrix state is overwritten.
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_resetMatrix">https://fiddle.skia.org/c/@Canvas_resetMatrix</a>
     */
    public M3Canvas resetMatrix() {
        assert _ptr != 0 : "Canvas is closed";
        M3Stats.onNativeCall();
        _nResetMatrix(_ptr);
        return this;
    }

    /**
     * Returns the total transformation matrix for the canvas.
     */
    public M3Matrix44 getLocalToDevice() {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            float[] mat = _nGetLocalToDevice(_ptr);
            return new M3Matrix44(mat);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Matrix33 getLocalToDeviceAsMatrix33() {
        return getLocalToDevice().asMatrix33();
    }

    public M3Canvas clipRect(M3Rect r, M3ClipMode mode, boolean antiAlias) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t clipRect with r == null";
        assert mode != null : "Can’t clipRect with mode == null";
        M3Stats.onNativeCall();
        _nClipRect(_ptr, r._left, r._top, r._right, r._bottom, mode.ordinal(), antiAlias);
        return this;
    }

    public M3Canvas clipRect(M3Rect r, M3ClipMode mode) {
        return clipRect(r, mode, false);
    }

    public M3Canvas clipRect(M3Rect r, boolean antiAlias) {
        return clipRect(r, M3ClipMode.INTERSECT, antiAlias);
    }

    public M3Canvas clipRect(M3Rect r) {
        return clipRect(r, M3ClipMode.INTERSECT, false);
    }

    public M3Canvas clipRRect(M3RRect r, M3ClipMode mode, boolean antiAlias) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t clipRRect with r == null";
        assert mode != null : "Can’t clipRRect with mode == null";
        M3Stats.onNativeCall();
        _nClipRRect(_ptr, r._left, r._top, r._right, r._bottom, r._radii, mode.ordinal(), antiAlias);
        return this;
    }

    public M3Canvas clipRRect(M3RRect r, M3ClipMode mode) {
        return clipRRect(r, mode, false);
    }

    public M3Canvas clipRRect(M3RRect r, boolean antiAlias) {
        return clipRRect(r, M3ClipMode.INTERSECT, antiAlias);
    }

    public M3Canvas clipRRect(M3RRect r) {
        return clipRRect(r, M3ClipMode.INTERSECT, false);
    }

    public M3Canvas clipPath(M3Path p, M3ClipMode mode, boolean antiAlias) {
        assert _ptr != 0 : "Canvas is closed";
        assert p != null : "Can’t clipPath with p == null";
        assert mode != null : "Can’t clipPath with mode == null";
        M3Stats.onNativeCall();
        _nClipPath(_ptr, M3Native.getPtr(p), mode.ordinal(), antiAlias);
        M3ReferenceUtil.reachabilityFence(p);
        return this;
    }

    public M3Canvas clipPath(M3Path p, M3ClipMode mode) {
        return clipPath(p, mode, false);
    }

    public M3Canvas clipPath(M3Path p, boolean antiAlias) {
        return clipPath(p, M3ClipMode.INTERSECT, antiAlias);
    }

    public M3Canvas clipPath(M3Path p) {
        return clipPath(p, M3ClipMode.INTERSECT, false);
    }

    public M3Canvas clipRegion(M3Region r, M3ClipMode mode) {
        assert _ptr != 0 : "Canvas is closed";
        assert r != null : "Can’t clipRegion with r == null";
        assert mode != null : "Can’t clipRegion with mode == null";
        M3Stats.onNativeCall();
        _nClipRegion(_ptr, M3Native.getPtr(r), mode.ordinal());
        M3ReferenceUtil.reachabilityFence(r);
        return this;
    }

    public M3Canvas clipRegion(M3Region r) {
        return clipRegion(r, M3ClipMode.INTERSECT);
    }

    public M3Canvas translate(float dx, float dy) {
        return concat(M3Matrix33.makeTranslate(dx, dy));
    }

    public M3Canvas scale(float sx, float sy) {
        return concat(M3Matrix33.makeScale(sx, sy));
    }

    /**
     * @param deg  angle in degrees
     * @return     this
     */
    public M3Canvas rotate(float deg) {
        return concat(M3Matrix33.makeRotate(deg));
    }

    public M3Canvas skew(float sx, float sy) {
        return concat(M3Matrix33.makeSkew(sx, sy));
    }

    public M3Canvas concat(M3Matrix33 matrix) {
        assert _ptr != 0 : "Canvas is closed";
        assert matrix != null : "Can’t concat with matrix == null";
        M3Stats.onNativeCall();
        _nConcat(_ptr, matrix.getMat());
        return this;
    }

    public M3Canvas concat(M3Matrix44 matrix) {
        assert _ptr != 0 : "Canvas is closed";
        assert matrix != null : "Can’t concat with matrix == null";
        M3Stats.onNativeCall();
        _nConcat44(_ptr, matrix.getMat());
        return this;
    }

    /**
     * <p>Copies Rect of pixels from Canvas into bitmap. Matrix and clip are
     * ignored.</p>
     *
     * <p>Source Rect corners are (srcX, srcY) and (imageInfo().width(), imageInfo().height()).
     * Destination Rect corners are (0, 0) and (bitmap.width(), bitmap.height()).
     * Copies each readable pixel intersecting both rectangles, without scaling,
     * converting to bitmap.colorType() and bitmap.alphaType() if required.</p>
     *
     * <p>Pixels are readable when BaseDevice is raster, or backed by a GPU.
     * Pixels are not readable when Canvas is returned by Document::beginPage,
     * returned by PictureRecorder::beginRecording, or Canvas is the base of a utility
     * class like DebugCanvas.</p>
     *
     * <p>Caller must allocate pixel storage in bitmap if needed.</p>
     *
     * <p>SkBitmap values are converted only if ColorType and AlphaType
     * do not match. Only pixels within both source and destination rectangles
     * are copied. Bitmap pixels outside Rect intersection are unchanged.</p>
     *
     * <p>Pass negative values for srcX or srcY to offset pixels across or down bitmap.</p>
     *
     * <p>Does not copy, and returns false if:
     * <ul>
     *   <li>Source and destination rectangles do not intersect.</li>
     *   <li>SkCanvas pixels could not be converted to bitmap.colorType() or bitmap.alphaType().</li>
     *   <li>SkCanvas pixels are not readable; for instance, Canvas is document-based.</li>
     *   <li>bitmap pixels could not be allocated.</li>
     *   <li>bitmap.rowBytes() is too small to contain one row of pixels.</li>
     * </ul>
     *
     * @param bitmap  storage for pixels copied from Canvas
     * @param srcX    offset into readable pixels on x-axis; may be negative
     * @param srcY    offset into readable pixels on y-axis; may be negative
     * @return        true if pixels were copied
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_readPixels_3">https://fiddle.skia.org/c/@Canvas_readPixels_3</a>
     */
    public boolean readPixels(M3Bitmap bitmap, int srcX, int srcY) {
        try {
            assert _ptr != 0 : "Canvas is closed";
            assert bitmap != null : "Can’t readPixels with bitmap == null";
            M3Stats.onNativeCall();
            return _nReadPixels(_ptr, M3Native.getPtr(bitmap), srcX, srcY);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(bitmap);
        }
    }

    /**
     * <p>Copies Rect from pixels to Canvas. Matrix and clip are ignored.
     * Source Rect corners are (0, 0) and (bitmap.width(), bitmap.height()).</p>
     *
     * <p>Destination Rect corners are (x, y) and
     * (imageInfo().width(), imageInfo().height()).</p>
     *
     * <p>Copies each readable pixel intersecting both rectangles, without scaling,
     * converting to getImageInfo().getColorType() and getImageInfo().getAlphaType() if required.</p>
     *
     * <p>Pixels are writable when BaseDevice is raster, or backed by a GPU.
     * Pixels are not writable when Canvas is returned by Document::beginPage,
     * returned by PictureRecorder::beginRecording, or Canvas is the base of a utility
     * class like DebugCanvas.</p>
     *
     * <p>Pixel values are converted only if ColorType and AlphaType
     * do not match. Only pixels within both source and destination rectangles
     * are copied. Canvas pixels outside Rect intersection are unchanged.</p>
     *
     * <p>Pass negative values for x or y to offset pixels to the left or
     * above Canvas pixels.</p>
     *
     * <p>Does not copy, and returns false if:
     * <ul>
     *   <li>Source and destination rectangles do not intersect.</li>
     *   <li>bitmap does not have allocated pixels.</li>
     *   <li>bitmap pixels could not be converted to Canvas getImageInfo().getColorType() or
     * getImageInfo().getAlphaType().</li>
     *   <li>Canvas pixels are not writable; for instance, Canvas is document based.</li>
     *   <li>bitmap pixels are inaccessible; for instance, bitmap wraps a texture.</li>
     * </ul>
     *
     * @param bitmap  contains pixels copied to Canvas
     * @param x       offset into Canvas writable pixels on x-axis; may be negative
     * @param y       offset into Canvas writable pixels on y-axis; may be negative
     * @return        true if pixels were written to Canvas
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_writePixels_2">https://fiddle.skia.org/c/@Canvas_writePixels_2</a>
     * @see <a href="https://fiddle.skia.org/c/@State_Stack_a">https://fiddle.skia.org/c/@State_Stack_a</a>
     * @see <a href="https://fiddle.skia.org/c/@State_Stack_b">https://fiddle.skia.org/c/@State_Stack_b</a>
    */
    public boolean writePixels(M3Bitmap bitmap, int x, int y) {
        try {
            assert _ptr != 0 : "Canvas is closed";
            assert bitmap != null : "Can’t writePixels with bitmap == null";
            M3Stats.onNativeCall();
            return _nWritePixels(_ptr, M3Native.getPtr(bitmap), x, y);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(bitmap);
        }
    }

    public int save() {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            return _nSave(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public int saveLayer(float left, float top, float right, float bottom, M3Paint paint) {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            return _nSaveLayerRect(_ptr, left, top, right, bottom, M3Native.getPtr(paint));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    /**
     * <p>Saves Matrix and clip, and allocates a Bitmap for subsequent drawing.
     * Calling restore() discards changes to Matrix and clip, and draws the Bitmap.</p>
     *
     * <p>Matrix may be changed by translate(), scale(), rotate(), skew(), concat(),
     * setMatrix(), and resetMatrix(). Clip may be changed by clipRect(), clipRRect(),
     * clipPath(), clipRegion().</p>
     *
     * <p>Rect bounds suggests but does not define the Bitmap size. To clip drawing to
     * a specific rectangle, use clipRect().</p>
     *
     * <p>Optional Paint paint applies alpha, ColorFilter, ImageFilter, and
     * BlendMode when restore() is called.</p>
     *
     * <p>Call restoreToCount() with returned value to restore this and subsequent saves.</p>
     *
     * @param bounds  hint to limit the size of the layer
     * @param paint   graphics state for layer; may be null
     * @return        depth of saved stack
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_saveLayer">https://fiddle.skia.org/c/@Canvas_saveLayer</a>
     * @see <a href="https://fiddle.skia.org/c/@Canvas_saveLayer_4">https://fiddle.skia.org/c/@Canvas_saveLayer_4</a>
    */
    public int saveLayer(M3Rect bounds, M3Paint paint) {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            if (bounds == null)
                return _nSaveLayer(_ptr, M3Native.getPtr(paint));
            else
                return _nSaveLayerRect(_ptr, bounds._left, bounds._top, bounds._right, bounds._bottom, M3Native.getPtr(paint));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    /**
     * <p>Saves matrix and clip, and allocates bitmap for subsequent drawing.</p>
     *
     * <p>Calling restore() discards changes to matrix and clip,
     * and blends layer with alpha opacity onto prior layer.</p>
     *
     * <p>matrix may be changed by translate(), scale(), rotate(), skew(), concat(),
     * setMatrix(), and resetMatrix(). Clip may be changed by clipRect(), clipRRect(),
     * clipPath(), clipRegion().</p>
     *
     * <p>Bounds suggests but does not define layer size. To clip drawing to
     * a specific rectangle, use clipRect().</p>
     *
     * <p>Call restoreToCount() with returned value to restore this and subsequent saves.</p>
     *
     * @param bounds  hint to limit the size of layer; may be null
     * @param alpha   opacity of layer, zero is fully transparent, 255 is fully opaque.
     * @return        depth of saved stack
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_saveLayerAlpha">https://fiddle.skia.org/c/@Canvas_saveLayerAlpha</a>
     */
    public int saveLayerAlpha(M3Rect bounds, int alpha) {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            if (bounds == null)
                return _nSaveLayerAlpha(_ptr, alpha);
            else
                return _nSaveLayerAlphaRect(_ptr, bounds._left, bounds._top, bounds._right, bounds._bottom, alpha);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Saves matrix and clip, and allocates Bitmap for subsequent drawing.</p>
     *
     * <p>Calling restore() discards changes to matrix and clip,
     * and blends Bitmap with alpha opacity onto the prior layer.</p>
     *
     * <p>matrix may be changed by translate(), scale(), rotate(), skew(), concat(),
     * setMatrix(), and resetMatrix(). Clip may be changed by clipRect(), clipRRect(),
     * clipPath(), clipRegion().</p>
     *
     * <p>SaveLayerRec contains the state used to create the layer.</p>
     *
     * <p>Call restoreToCount() with returned value to restore this and subsequent saves.</p>
     *
     * @param layerRec  layer state
     * @return          depth of save state stack before this call was made.
     *
     * @see <a href="https://fiddle.skia.org/c/@Canvas_saveLayer_3">https://fiddle.skia.org/c/@Canvas_saveLayer_3</a>
     */
    public int saveLayer(M3SaveLayerRec layerRec) {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            M3Rect bounds = layerRec._bounds;
            if (bounds == null)
                return _nSaveLayerRec(_ptr, M3Native.getPtr(layerRec._paint), M3Native.getPtr(layerRec._backdrop), layerRec._tileMode.ordinal(), M3Native.getPtr(layerRec._colorSpace), layerRec._flags);
            else
                return _nSaveLayerRecRect(_ptr, bounds._left, bounds._top, bounds._right, bounds._bottom, M3Native.getPtr(layerRec._paint), M3Native.getPtr(layerRec._backdrop), layerRec._tileMode.ordinal(), M3Native.getPtr(layerRec._colorSpace), layerRec._flags);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(layerRec);
        }
    }

    public int getSaveCount() {
        try {
            assert _ptr != 0 : "Canvas is closed";
            M3Stats.onNativeCall();
            return _nGetSaveCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Canvas restore() {
        assert _ptr != 0 : "Canvas is closed";
        M3Stats.onNativeCall();
        _nRestore(_ptr);
        return this;
    }

    public M3Canvas restoreToCount(int saveCount) {
        assert _ptr != 0 : "Canvas is closed";
        M3Stats.onNativeCall();
        _nRestoreToCount(_ptr, saveCount);
        return this;
    }

    public void invalidate() {
        _ptr = 0;
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public static native long _nGetFinalizer();
    public static native long _nMakeFromBitmap(long bitmapPtr, int flags, int pixelGeometry);
    public static native M3SurfaceProps _nGetBaseProps(long ptr);
    public static native M3SurfaceProps _nGetTopProps(long ptr);
    public static native M3IRect _nGetDeviceClipBounds(long ptr);
    public static native M3Rect _nGetLocalClipBounds(long ptr);
    public static native long _nGetSurface(long ptr);
    public static native void _nDrawPoint(long ptr, float x, float y, long paintPtr);
    public static native void _nDrawPoints(long ptr, int mode, float[] coords, long paintPtr);
    public static native void _nDrawLine(long ptr, float x0, float y0, float x1, float y1, long paintPtr);
    public static native void _nDrawArc(long ptr, float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean includeCenter, long paintPtr);
    public static native void _nDrawRect(long ptr, float left, float top, float right, float bottom, long paintPtr);
    public static native void _nDrawOval(long ptr, float left, float top, float right, float bottom, long paint);
    public static native void _nDrawRRect(long ptr, float left, float top, float right, float bottom, float[] radii, long paintPtr);
    public static native boolean _nQuickReject(long ptr, float left, float top, float right, float bottom);
    public static native boolean _nQuickRejectPath(long ptr, long pathPtr);
    public static native void _nDrawDRRect(long ptr, float ol, float ot, float or, float ob, float[] oradii, float il, float it, float ir, float ib, float[] iradii, long paintPtr);
    public static native void _nDrawPath(long ptr, long nativePath, long paintPtr);
    public static native void _nDrawImageRect(long ptr, long nativeImage, float sl, float st, float sr, float sb, float dl, float dt, float dr, float db, long samplingMode, long paintPtr, boolean strict);
    public static native void _nDrawImageNine(long ptr, long nativeImage, int cl, int ct, int cr, int cb, float dl, float dt, float dr, float db, int filterMode, long paintPtr);
    public static native void _nDrawRegion(long ptr, long nativeRegion, long paintPtr);
    public static native void _nDrawString(long ptr, String string, float x, float y, long font, long paint);
    public static native void _nDrawTextBlob(long ptr, long blob, float x, float y, long paint);
    public static native void _nDrawPicture(long ptr, long picturePtr, float[] matrix, long paintPtr);
    public static native void _nDrawVertices(long ptr, int verticesMode, float[] cubics, int[] colors, float[] texCoords, short[] indices, int blendMode, long paintPtr);
    public static native void _nDrawPatch(long ptr, float[] cubics, int[] colors, float[] texCoords, int blendMode, long paintPtr);
    public static native void _nDrawDrawable(long ptr, long drawablePrt, float[] matrix);
    public static native void _nDrawColor(long ptr, int color, int blendMode);
    public static native void _nDrawColor4f(long ptr, float r, float g, float b, float a, int blendMode);
    public static native void _nClear(long ptr, int color);
    public static native void _nDrawPaint(long ptr, long paintPtr);
    public static native void _nSetMatrix(long ptr, float[] matrix);
    public static native void _nSetMatrix44(long ptr, float[] matrix);
    public static native float[] _nGetLocalToDevice(long ptr);
    public static native void _nResetMatrix(long ptr);
    public static native void _nClipRect(long ptr, float left, float top, float right, float bottom, int mode, boolean antiAlias);
    public static native void _nClipRRect(long ptr, float left, float top, float right, float bottom, float[] radii, int mode, boolean antiAlias);
    public static native void _nClipPath(long ptr, long nativePath, int mode, boolean antiAlias);
    public static native void _nClipRegion(long ptr, long nativeRegion, int mode);
    public static native void _nConcat(long ptr, float[] matrix);
    public static native void _nConcat44(long ptr, float[] matrix);
    public static native boolean _nReadPixels(long ptr, long bitmapPtr, int srcX, int srcY);
    public static native boolean _nWritePixels(long ptr, long bitmapPtr, int x, int y);
    public static native int  _nSave(long ptr);
    public static native int  _nSaveLayer(long ptr, long paintPtr);
    public static native int  _nSaveLayerRect(long ptr, float left, float top, float right, float bottom, long paintPtr);
    public static native int  _nSaveLayerAlpha(long ptr, int alpha);
    public static native int  _nSaveLayerAlphaRect(long ptr, float left, float top, float right, float bottom, int alpha);
    public static native int  _nSaveLayerRec(long ptr, long paintPtr, long backdropPtr, int tileMode, long colorSpacePtr, int flags);
    public static native int  _nSaveLayerRecRect(long ptr, float left, float top, float right, float bottom, long paintPtr, long backdropPtr, int tileMode, long colorSpacePtr, int flags);
    public static native int  _nGetSaveCount(long ptr);
    public static native void _nRestore(long ptr);
    public static native void _nRestoreToCount(long ptr, int saveCount);
    public static native void _nAnnotateRectWithURL(long ptr, float left, float top, float right, float bottom, String url);
    public static native void _nAnnotateNamedDestination(long ptr, float x, float y, String name);
    public static native void _nAnnotateLinkToDestination(long ptr, float left, float top, float right, float bottom, String name);
}
