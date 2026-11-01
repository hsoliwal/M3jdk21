package jdk.m3.graphics;


/**
 * @see io.github.humbleui.skija.FilterMipmap
 * @see io.github.humbleui.skija.CubicResampler
 */
public interface M3SamplingMode {
    public static final M3SamplingMode DEFAULT = new M3FilterMipmap(M3FilterMode.NEAREST, M3MipmapMode.NONE);
    public static final M3SamplingMode LINEAR = new M3FilterMipmap(M3FilterMode.LINEAR, M3MipmapMode.NONE);
    public static final M3SamplingMode MITCHELL = new M3CubicResampler(0.33333334f, 0.33333334f);
    public static final M3SamplingMode CATMULL_ROM = new M3CubicResampler(0, 0.5f);

    // 10 + 30-bit float + 32-bit float: CubicResampler
    // 01 + 30-bit zeros + 32-bit int:   SamplingModeAnisotropic
    // 00 + 30-bit int   + 32-bit int:   FilterMipmap
    long _pack();
}