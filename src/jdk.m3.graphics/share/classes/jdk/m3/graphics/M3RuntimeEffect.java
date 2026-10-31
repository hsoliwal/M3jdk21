package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

/**
 * <p>RuntimeEffect supports creating custom {@link Shader} and {@link ColorFilter}
 * objects using Skia's SkSL shading language.</p>
 *
 * <p>NOTE: This API is experimental and subject to change.</p>
 */
public class M3RuntimeEffect extends M3RefCnt {
    static { M3Library.staticLoad(); }

    public static M3RuntimeEffect makeForColorFilter(String sksl) {
        return makeForColorFilter(sksl, M3RuntimeEffectOptions.DEFAULT);
    }

    /**
     * <p>MakeForColorFilter and MakeForShader verify that the SkSL code is valid for those stages of
     * the Skia pipeline. In all of the signatures described below, color parameters and return
     * values are flexible. They are listed as being 'vec4', but they can also be 'half4' or
     * 'float4'. ('vec4' is an alias for 'float4').</p>
     *
     * <p>Color filter SkSL requires an entry point that looks like:</p>
     *
     * <pre>{@code
     * vec4 main(vec4 inColor) { ... }
     * }</pre>
     *
     * @param sksl     shader code
     * @param options  options
     * @return         new RuntimeEffect
     * @see <a href="https://fiddle.skia.org/c/@runtimeeffect_colorfilter_grid">https://fiddle.skia.org/c/@runtimeeffect_colorfilter_grid</a>
     */
    public static M3RuntimeEffect makeForColorFilter(String sksl, M3RuntimeEffectOptions options) {
        assert sksl != null : "Can't makeForColorFilter with sksl == null";
        M3Stats.onNativeCall();
        return new M3RuntimeEffect(_nMakeForColorFilter(sksl, options._forceUnoptimized, options._name));
    }

    public static M3RuntimeEffect makeForShader(String sksl) {
        return makeForShader(sksl, M3RuntimeEffectOptions.DEFAULT);
    }

    /**
     * <p>Shader SkSL requires an entry point that looks like:</p>
     *
     * <pre>{@code
     * vec4 main(vec2 inCoords) { ... }
     * }</pre>
     *
     * <p>The color that is returned should be premultiplied.</p>
     *
     * @param sksl     shader code
     * @param options  options
     * @return         new RuntimeEffect
     */
    public static M3RuntimeEffect makeForShader(String sksl, M3RuntimeEffectOptions options) {
        assert sksl != null : "Can't makeForShader with sksl == null";
        M3Stats.onNativeCall();
        return new M3RuntimeEffect(_nMakeForShader(sksl, options._forceUnoptimized, options._name));
    }

    public static M3RuntimeEffect makeForBlender(String sksl) {
        return makeForBlender(sksl, M3RuntimeEffectOptions.DEFAULT);
    }

    /**
     * <p>Blend SkSL requires an entry point that looks like:</p>
     *
     * <pre>{@code
     * vec4 main(vec4 srcColor, vec4 dstColor) { ... }
     * }</pre>
     *
     * @param sksl     shader code
     * @param options  options
     * @return         new RuntimeEffect
     */
    public static M3RuntimeEffect makeForBlender(String sksl, M3RuntimeEffectOptions options) {
        assert sksl != null : "Can't makeForBlender with sksl == null";
        M3Stats.onNativeCall();
        return new M3RuntimeEffect(_nMakeForBlender(sksl, options._forceUnoptimized, options._name));
    }

    public <T extends M3RuntimeEffectChild> M3ColorFilter makeColorFilter(M3Data uniforms, T[] children) {
        M3Stats.onNativeCall();
        int childCount = children == null ? 0 : children.length;
        long[] childrenPtrs = new long[childCount];
        for (int i = 0; i < childCount; i++)
            childrenPtrs[i] = M3Native.getPtr(children[i]);
        return new M3ColorFilter(_nMakeColorFilter(_ptr, M3Native.getPtr(uniforms), childrenPtrs));
    }

    public M3ColorFilter makeColorFilter(M3Data uniforms) {
        return makeColorFilter(uniforms, (M3RuntimeEffectChild[]) null);
    }

    public <T extends M3RuntimeEffectChild> M3Shader makeShader(M3Data uniforms, T[] children, M3Matrix33 localMatrix) {
        M3Stats.onNativeCall();
        int childCount = children == null ? 0 : children.length;
        long[] childrenPtrs = new long[childCount];
        for (int i = 0; i < childCount; i++) {
            childrenPtrs[i] = M3Native.getPtr(children[i]);
        }
        float[] matrix = localMatrix == null ? null : localMatrix._mat;
        return new M3Shader(_nMakeShader(_ptr, M3Native.getPtr(uniforms), childrenPtrs, matrix));
    }

    public <T extends M3RuntimeEffectChild> M3Shader makeShader(M3Data uniforms, T[] children) {
        return makeShader(uniforms, children, null);
    }

    public <T extends M3RuntimeEffectChild> M3Blender makeBlender(M3Data uniforms, T[] children) {
        M3Stats.onNativeCall();
        int childCount = children == null ? 0 : children.length;
        long[] childrenPtrs = new long[childCount];
        for (int i = 0; i < childCount; i++) {
            childrenPtrs[i] = M3Native.getPtr(children[i]);
        }
        return new M3Blender(_nMakeBlender(_ptr, M3Native.getPtr(uniforms), childrenPtrs));
    }

    public M3Blender makeBlender(M3Data uniforms) {
        return makeBlender(uniforms, (M3RuntimeEffectChild[]) null);
    }

    /**
     * Returns the SkSL source of the runtime effect.
     *
     * @return  the SkSL source code
     */
    public String getSource() {
        try {
            M3Stats.onNativeCall();
            return _nGetSource(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns the combined size of all 'uniform' variables.
     * When calling makeColorFilter, makeShader, or makeBlender, provide a Data of this size.
     *
     * @return  size in bytes of all uniform variables combined
     */
    public int getUniformSize() {
        try {
            M3Stats.onNativeCall();
            return _nGetUniformSize(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if this runtime effect can be used as a shader.
     *
     * @return  true if allowShader flag is set
     */
    public boolean isShaderAllowed() {
        try {
            M3Stats.onNativeCall();
            return _nIsShaderAllowed(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if this runtime effect can be used as a color filter.
     *
     * @return  true if allowColorFilter flag is set
     */
    public boolean isColorFilterAllowed() {
        try {
            M3Stats.onNativeCall();
            return _nIsColorFilterAllowed(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns true if this runtime effect can be used as a blender.
     *
     * @return  true if allowBlender flag is set
     */
    public boolean isBlenderAllowed() {
        try {
            M3Stats.onNativeCall();
            return _nIsBlenderAllowed(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns information about all uniform variables in the runtime effect.
     *
     * @return  array of RuntimeEffectUniformInfo objects
     */
    public M3RuntimeEffectUniformInfo[] getUniforms() {
        try {
            M3Stats.onNativeCall();
            return _nGetUniforms(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns information about a specific uniform variable by name.
     *
     * @param name  the name of the uniform to find
     * @return      RuntimeEffectUniformInfo for the uniform, or null if not found
     */
    public M3RuntimeEffectUniformInfo getUniform(String name) {
        try {
            assert name != null : "Can't getUniform with name == null";
            M3Stats.onNativeCall();
            return _nGetUniform(_ptr, name);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns information about all child effects (shaders, color filters, blenders) in the runtime effect.
     *
     * @return  array of RuntimeEffectChildInfo objects
     */
    public M3RuntimeEffectChildInfo[] getChildren() {
        try {
            M3Stats.onNativeCall();
            return _nGetChildren(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns information about a specific child effect by name.
     *
     * @param name  the name of the child to find
     * @return      RuntimeEffectChildInfo for the child, or null if not found
     */
    public M3RuntimeEffectChildInfo getChild(String name) {
        try {
            assert name != null : "Can't getChild with name == null";
            M3Stats.onNativeCall();
            return _nGetChild(_ptr, name);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3RuntimeEffect(long ptr) {
        super(ptr);
    }

    public static native long _nMakeForColorFilter(String sksl, boolean forceUnoptimized, String name);
    public static native long _nMakeForShader(String sksl, boolean forceUnoptimized, String name);
    public static native long _nMakeForBlender(String sksl, boolean forceUnoptimized, String name);
    public static native long _nMakeShader(long runtimeEffectPtr, long uniformPtr, long[] childrenPtrs, float[] localMatrix);
    public static native long _nMakeColorFilter(long runtimeEffectPtr, long uniformPtr, long[] childrenPtrs);
    public static native long _nMakeBlender(long runtimeEffectPtr, long uniformPtr, long[] childrenPtrs);
    public static native String _nGetSource(long ptr);
    public static native int _nGetUniformSize(long ptr);
    public static native boolean _nIsShaderAllowed(long ptr);
    public static native boolean _nIsColorFilterAllowed(long ptr);
    public static native boolean _nIsBlenderAllowed(long ptr);
    public static native M3RuntimeEffectUniformInfo[] _nGetUniforms(long ptr);
    public static native M3RuntimeEffectUniformInfo _nGetUniform(long ptr, String name);
    public static native M3RuntimeEffectChildInfo[] _nGetChildren(long ptr);
    public static native M3RuntimeEffectChildInfo _nGetChild(long ptr, String name);
}