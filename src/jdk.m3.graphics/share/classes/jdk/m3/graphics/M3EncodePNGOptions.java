package jdk.m3.graphics;


public class M3EncodePNGOptions {
    public static final M3EncodePNGOptions DEFAULT = new M3EncodePNGOptions(M3EncodePNGFilterFlag.ALL._value, 6);

    public final int _flags;

    public final int _zlibLevel;

    public M3EncodePNGOptions(int flags, int level) {
        _flags = flags;
        _zlibLevel = level;
    }

    public M3EncodePNGOptions withFlags(M3EncodePNGFilterFlag... flags) {
        int _flags = 0;
        for (M3EncodePNGFilterFlag flag: flags) {
            _flags = _flags | flag._value;
        }
        return new M3EncodePNGOptions(_flags, _zlibLevel);
    }

    public M3EncodePNGOptions withZlibLevel(int level) {
        return new M3EncodePNGOptions(_flags, level);
    }
}
