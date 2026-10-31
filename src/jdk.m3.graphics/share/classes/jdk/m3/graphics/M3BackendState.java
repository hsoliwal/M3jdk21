package jdk.m3.graphics;

// GrTypes.h - GrGLBackendState + kAll_GrBackendState
public enum M3BackendState {
    GL_RENDER_TARGET(1 << 0), /**
     * Also includes samplers bound to texture units.
     */
    GL_TEXTURE_BINDING(1 << 1), /**
     * View state stands for scissor and viewport
     */
    GL_VIEW(1 << 2), GL_BLEND(1 << 3), GL_MSAA_ENABLE(1 << 4), GL_VERTEX(1 << 5), GL_STENCIL(1 << 6), GL_PIXEL_STORE(1 << 7), GL_PROGRAM(1 << 8), GL_FIXED_FUNCTION(1 << 9), GL_MISC(1 << 10), GL_ALL(65535), /**
     * This value translates to reseting all the context state for any backend.
     */
    ALL(-1);
    public static final M3BackendState[] _values = values();
    public final int _value;

    M3BackendState(int value) {
        this._value = value;
    }

    @java.lang.SuppressWarnings("all")
    public int getValue() {
        return this._value;
    }
}
