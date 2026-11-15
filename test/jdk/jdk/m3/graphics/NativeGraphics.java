/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 */

/*
 * @test
 * @summary Exercise the absorbed native graphics owners, clipping, codecs, text and lifetime
 * @modules jdk.m3.graphics
 * @run main/othervm -Xmx128m -Xcheck:jni NativeGraphics
 */

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.M3Library;
import jdk.m3.graphics.impl.M3Managed;
import jdk.m3.graphics.paragraph.*;
import jdk.m3.graphics.resources.*;
import jdk.m3.graphics.shaper.M3Shaper;
import jdk.m3.graphics.skottie.M3Animation;
import jdk.m3.graphics.skottie.M3Logger;
import jdk.m3.graphics.skottie.M3LogLevel;
import jdk.m3.graphics.sksg.M3InvalidationController;
import jdk.m3.graphics.svg.M3SVGDOM;
import jdk.m3.graphics.svg.M3SVGSVG;

public class NativeGraphics {
    private static int checks;

    public static void main(String[] args) throws Exception {
        M3Library.load();
        check(M3Library._loaded, "native initialization completed");
        rasterViewportAndCodecs();
        bitmapVolatility();
        textAndParagraph();
        vectorAndAnimation();
        runtimeEffectAndDocument();
        crossThreadCleanup();
        typefaceFactories();
        unavailableBackends();
        System.out.println("PASS: " + checks + " native graphics checks");
    }

    private static void rasterViewportAndCodecs() {
        try (M3Surface surface = M3Surface.makeRasterN32Premul(16, 16);
             M3Paint paint = new M3Paint().setColor(0xffff0000)) {
            M3Canvas canvas = surface.getCanvas();
            canvas.clear(0xffffffff);
            canvas.save();
            canvas.clipRect(M3Rect.makeXYWH(4, 4, 8, 8), false);
            check(canvas.quickReject(M3Rect.makeXYWH(20, 20, 2, 2)), "clip rejects outside rectangle");
            // A large logical drawing extent must retain the small raster viewport.
            canvas.drawRect(M3Rect.makeLTRB(-1_000_000, -1_000_000, 1_000_000, 1_000_000), paint);
            check(canvas.getLocalToDevice() != null, "native matrix array descriptor");
            canvas.restore();
            try (M3Image snapshot = surface.makeImageSnapshot();
                 M3Bitmap pixels = M3Bitmap.makeFromImage(snapshot);
                 M3Data png = snapshot.encodeToData(M3EncodedImageFormat.PNG);
                 M3Image decoded = M3Image.makeFromEncoded(png.getBytes());
                 M3Bitmap decodedPixels = M3Bitmap.makeFromImage(decoded);
                 M3Codec codec = M3Codec.makeFromData(png)) {
                check(pixels.getColor(0, 0) == 0xffffffff, "outside viewport preserved");
                check(pixels.getColor(5, 5) == 0xffff0000, "viewport painted");
                check(decodedPixels.getColor(5, 5) == 0xffff0000, "PNG round trip pixels");
                check(decoded.getWidth() == 16 && decoded.getHeight() == 16, "image dimensions");
                check(codec.getEncodedImageFormat() == M3EncodedImageFormat.PNG, "codec format JNI width");
                check(codec.getEncodedOrigin() != null, "codec origin JNI width");
                check(codec.getFramesInfo() != null, "codec frame array JNI descriptor");
                check(png.getBytes().length > 8, "native encoded data");
            }
            try (M3Bitmap pixels = new M3Bitmap()) {
                check(pixels.allocN32Pixels(16, 16), "pixel allocation");
                check(canvas.readPixels(pixels, 0, 0), "canvas boolean JNI result");
            }
        }
        M3Paint released = new M3Paint();
        released.close();
        released.close();
        check(released.isClosed(), "managed close remains idempotent");
    }

    private static boolean volatileHint(M3Bitmap bitmap) {
        return M3Bitmap._nIsVolatile(bitmap._ptr);
    }

    private static void setVolatileHint(M3Bitmap bitmap, boolean value) {
        M3Bitmap._nSetVolatile(bitmap._ptr, value);
    }

    private static void bitmapVolatility() {
        try (M3Bitmap source = new M3Bitmap(); M3Bitmap target = new M3Bitmap()) {
            check(!volatileHint(source), "default volatility");
            setVolatileHint(source, true);
            check(source.allocN32Pixels(4, 4), "volatile pixel allocation");
            check(volatileHint(source), "successful allocation retains hint");
            try (M3Bitmap clone = source.makeClone()) {
                check(volatileHint(clone), "clone copies volatility");
                setVolatileHint(clone, false);
                check(volatileHint(source), "shared pixels do not share volatility");
            }
            check(source.extractSubset(target, M3IRect.makeXYWH(1, 1, 2, 2)), "subset succeeds");
            check(volatileHint(target), "subset copies volatility");
            setVolatileHint(target, false);
            source.swap(target);
            check(!volatileHint(source) && volatileHint(target), "swap exchanges volatility");
            check(target.extractAlpha(source), "alpha extraction");
            check(!volatileHint(source), "alpha destination has initial hint");
            target.reset();
            check(!volatileHint(target), "reset clears volatility");
            setVolatileHint(target, true);
            check(!target.allocN32Pixels(-1, 2), "invalid allocation fails");
            check(!volatileHint(target), "failed allocation resets hint");
        }
        for (int i = 0; i < 256; i++) {
            try (M3Bitmap bitmap = new M3Bitmap()) {
                check(!volatileHint(bitmap), "new allocation cannot inherit released hint");
                setVolatileHint(bitmap, true);
            }
        }
    }

    private static void textAndParagraph() {
        M3FontMgr manager = M3FontMgr.getDefault();
        try (M3Typeface face = manager.matchFamilyStyle(null, M3FontStyle.NORMAL);
             M3Font font = new M3Font(face, 24);
             M3Shaper shaper = M3Shaper.make();
             M3TextBlob blob = shaper.shape("Hello", font);
             M3TextBlob unicode = shaper.shape("Latin العربية हिन्दी", font)) {
            check(blob != null && blob.getGlyphs().length > 0, "HarfBuzz text shaping");
            check(unicode != null && unicode.getGlyphs().length > 0, "Unicode text shaping");
            check(font.getMetrics() != null && font.getSpacing() > 0, "font metrics JNI arguments");
            M3Rect bounds = blob.getBounds();
            float lower = bounds.getTop();
            float upper = bounds.getBottom();
            float[] expected = blob.getIntercepts(lower, upper, null);
            check(Arrays.equals(expected, blob.getIntercepts(lower, upper)), "two-argument intercept overload");
            check(expected != null && expected.length > 0, "real glyph intercepts");
            try (M3Data serialized = blob.serializeToData(); M3TextBlob restored = M3TextBlob.makeFromData(serialized)) {
                check(Arrays.equals(blob.getGlyphs(), restored.getGlyphs()), "text blob serialization");
            }
        }
        try (M3FontCollection fonts = new M3FontCollection();
             M3TextStyle text = new M3TextStyle().setFontSize(14).setFontFamily("sans-serif");
             M3ParagraphStyle style = new M3ParagraphStyle().setTextStyle(text);
             M3Surface surface = M3Surface.makeRasterN32Premul(64, 64)) {
            fonts.setDefaultFontManager(manager);
            check(style.getMaxLinesCount() == -1, "paragraph unlimited line sentinel");
            long logicalLimit = (long) Integer.MAX_VALUE + 7;
            style.setMaxLinesCount(logicalLimit);
            check(style.getMaxLinesCount() == logicalLimit, "paragraph line limit keeps 64-bit JNI value");
            style.setMaxLinesCount(-1);
            check(style.getMaxLinesCount() == -1, "paragraph unlimited line sentinel round trip");
            try (M3ParagraphBuilder builder = new M3ParagraphBuilder(style, fonts)) {
                builder.pushStyle(text).addText("wrapped text across multiple short lines").popStyle();
                try (M3Paragraph paragraph = builder.build()) {
                    paragraph.layout(48);
                    check(paragraph.getHeight() > 0 && paragraph.getLineNumber() > 1, "paragraph layout");
                    paragraph.paint(surface.getCanvas(), 0, 0);
                    check(paragraph.getLineMetrics().length > 1, "paragraph native callback descriptors");
                }
            }
        }
    }

    private static void vectorAndAnimation() throws Exception {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'>"
                + "<rect width='16' height='16' fill='#00ff00'/></svg>";
        try (M3Data data = M3Data.makeFromBytes(svg.getBytes(StandardCharsets.UTF_8));
             M3SVGDOM dom = new M3SVGDOM(data);
             M3Surface surface = M3Surface.makeRasterN32Premul(16, 16)) {
            dom.setContainerSize(16, 16).render(surface.getCanvas());
            try (M3Image image = surface.makeImageSnapshot(); M3Bitmap pixels = M3Bitmap.makeFromImage(image)) {
                check(pixels.getColor(8, 8) == 0xff00ff00, "SVG renderer pixels");
            }
            try (M3SVGSVG root = dom.getRoot()) {
                check(root != null, "SVG native owner lookup");
                check(root.getTag() != null, "inherited SVG node tag owner");
            }
        }
        String animation = "{\"v\":\"5.5.0\",\"fr\":30,\"ip\":0,\"op\":30,"
                + "\"w\":16,\"h\":16,\"nm\":\"native test\",\"ddd\":0,\"assets\":[],\"layers\":[]}";
        try (M3Animation movie = M3Animation.makeFromString(animation);
             M3InvalidationController invalidation = new M3InvalidationController();
             M3Surface surface = M3Surface.makeRasterN32Premul(16, 16)) {
            check(movie.getWidth() == 16 && movie.getFPS() == 30, "Skottie metadata");
            movie.seekFrame(1, invalidation).render(surface.getCanvas());
            invalidation.invalidate(1, 2, 5, 6, M3Matrix33.IDENTITY);
            check(invalidation.getBounds().getWidth() >= 4, "scene graph invalidation bounds");
            invalidation.reset();
        }
        Path directory = Files.createTempDirectory("graphics-resources");
        try (M3FileResourceProvider files = M3FileResourceProvider.make(directory.toString());
             M3CachingResourceProvider cached = M3CachingResourceProvider.make(files);
             M3DataURIResourceProviderProxy dataUris = M3DataURIResourceProviderProxy.make(cached)) {
            check(dataUris._ptr != 0, "resource providers retain native ownership");
        } finally {
            Files.delete(directory);
        }
    }

    private static void runtimeEffectAndDocument() {
        try (M3RuntimeEffect effect = M3RuntimeEffect.makeForShader("half4 main(float2 xy) { return half4(0, 0, 1, 1); }");
             M3RuntimeEffectBuilder builder = new M3RuntimeEffectBuilder(effect);
             M3Shader shader = builder.makeShader();
             M3Paint paint = new M3Paint().setShader(shader);
             M3Surface surface = M3Surface.makeRasterN32Premul(4, 4)) {
            surface.getCanvas().drawRect(M3Rect.makeWH(4, 4), paint);
            try (M3Image image = surface.makeImageSnapshot(); M3Bitmap pixels = M3Bitmap.makeFromImage(image)) {
                check(pixels.getColor(1, 1) == 0xff0000ff, "SkSL runtime shader pixels");
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (M3OutputWStream stream = new M3OutputWStream(output); M3Document document = M3Document.makePDF(stream)) {
            document.beginPage(16, 16).clear(0xff000000);
            document.endPage();
        }
        check(new String(output.toByteArray(), 0, 4, StandardCharsets.US_ASCII).equals("%PDF"), "PDF native output callback");
    }

    private static void typefaceFactories() throws Exception {
        long fromName = M3Typeface._nMakeFromName(null, M3FontStyle.NORMAL._value);
        check(fromName != 0, "retained typeface name factory");
        try (M3Typeface face = new M3Typeface(fromName)) {
            check(face.getGlyphsCount() > 0, "typeface name factory ownership");
        }
        for (M3FontStyle style : new M3FontStyle[] {M3FontStyle.NORMAL, M3FontStyle.BOLD,
                M3FontStyle.ITALIC, M3FontStyle.BOLD_ITALIC}) {
            int uniqueId;
            try (M3Typeface face = new M3Typeface(M3Typeface._nMakeFromName(null, style._value))) {
                uniqueId = face.getUniqueId();
            }
            M3FontStyle narrowed = style.withWidth(M3FontWidth.ULTRA_CONDENSED);
            try (M3Typeface face = new M3Typeface(M3Typeface._nMakeFromName(null, narrowed._value))) {
                check(face.getUniqueId() == uniqueId, "legacy default face identity and width normalization");
            }
        }
        long fallback = M3Typeface._nMakeFromName("missing-font-family-for-native-test", M3FontStyle.NORMAL._value);
        check(fallback != 0, "legacy typeface factory retains unknown-family fallback");
        try (M3Typeface face = new M3Typeface(fallback)) {
            check(face.getGlyphsCount() > 0, "legacy fallback is a usable owned typeface");
        }
        check(M3Typeface._nMakeFromFile("missing-font-for-native-test", 0) == 0, "missing font file result");
        try (M3Data invalid = M3Data.makeFromBytes(new byte[] {1, 2, 3})) {
            check(M3Typeface._nMakeFromData(invalid._ptr, 0) == 0, "invalid font data result");
        }
        String fixture = System.getProperty("test.font.file");
        if (fixture != null) {
            long fromFile = M3Typeface._nMakeFromFile(fixture, 0);
            check(fromFile != 0, "retained valid font file factory");
            try (M3Typeface face = new M3Typeface(fromFile);
                 M3Data data = M3Data.makeFromBytes(Files.readAllBytes(Path.of(fixture)))) {
                long fromData = M3Typeface._nMakeFromData(data._ptr, 0);
                check(fromData != 0, "retained valid font data factory");
                try (M3Typeface restored = new M3Typeface(fromData)) {
                    check(face.getGlyphsCount() == restored.getGlyphsCount(), "font factory results agree");
                }
            }
        }
    }

    private static void crossThreadCleanup() throws Exception {
        try (M3Logger logger = new M3Logger() {
                 @Override public void log(M3LogLevel level, String message, String json) {}
             };
             M3OutputWStream stream = new M3OutputWStream(new ByteArrayOutputStream())) {
            closeOnAnotherThread(logger);
            closeOnAnotherThread(stream);
        }
    }

    private static void closeOnAnotherThread(M3Managed resource) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        // Explicit close invokes the same CleanerThunk used by automatic cleanup.
        Thread cleanup = new Thread(() -> {
            try {
                resource.close();
            } catch (Throwable problem) {
                failure.set(problem);
            }
        }, "native-graphics-cleanup");
        cleanup.start();
        cleanup.join();
        if (failure.get() != null) throw new AssertionError("native cleanup", failure.get());
        check(resource.isClosed(), "native reference cleanup uses the current thread");
    }

    private static void unavailableBackends() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) {
            unsupported(() -> M3DirectContext._nMakeMetal(0, 0), "Metal");
            unsupported(() -> M3BackendRenderTarget._nMakeMetal(1, 1, 0), "Metal target");
            unsupported(() -> M3Surface._nWrapMTKView(0, 0, 0, 0, 0, 0, null), "Metal surface");
        }
        if (!os.contains("windows")) {
            unsupported(() -> M3DirectContext._nMakeDirect3D(0, 0, 0), "Direct3D");
            unsupported(() -> M3BackendRenderTarget._nMakeDirect3D(1, 1, 0, 0, 1, 1), "Direct3D target");
        }
    }

    private static void unsupported(Runnable action, String name) {
        try {
            action.run();
            throw new AssertionError(name + " unexpectedly succeeded");
        } catch (UnsupportedOperationException expected) {
            check(expected.getMessage().contains("unavailable"), name + " explicit status");
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }
}
