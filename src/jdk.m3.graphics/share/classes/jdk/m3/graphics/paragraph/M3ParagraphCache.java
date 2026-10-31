package jdk.m3.graphics.paragraph;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3ParagraphCache extends M3Native {
    static { M3Library.staticLoad(); }
    public void abandon() {
        try {
            _validate();
            M3Stats.onNativeCall();
            _nAbandon(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public void reset() {
        try {
            _validate();
            M3Stats.onNativeCall();
            _nReset(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public boolean updateParagraph(M3Paragraph paragraph) {
        try {
            _validate();
            M3Stats.onNativeCall();
            return _nUpdateParagraph(_ptr, M3Native.getPtr(paragraph));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(paragraph);
        }
    }

    public boolean findParagraph(M3Paragraph paragraph) {
        try {
            _validate();
            M3Stats.onNativeCall();
            return _nFindParagraph(_ptr, M3Native.getPtr(paragraph));
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
            M3ReferenceUtil.reachabilityFence(paragraph);
        }
    }

    public void printStatistics() {
        try {
            _validate();
            M3Stats.onNativeCall();
            _nPrintStatistics(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public void setEnabled(boolean value) {
        try {
            _validate();
            M3Stats.onNativeCall();
            _nSetEnabled(_ptr, value);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public int getCount() {
        try {
            _validate();
            M3Stats.onNativeCall();
            return _nGetCount(_ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3ParagraphCache(M3FontCollection owner, long ptr) {
        super(ptr);
        this._owner = owner;
    }

    public final M3FontCollection _owner;

    public void _validate() {
        try {
            if (M3Native.getPtr(_owner) == 0)
                throw new IllegalStateException("ParagraphCache needs owning FontCollection to be alive");
        } finally {
            M3ReferenceUtil.reachabilityFence(_owner);
        }
    }

    public static native void    _nAbandon(long ptr);
    public static native void    _nReset(long ptr);
    public static native boolean _nUpdateParagraph(long ptr, long paragraphPtr);
    public static native boolean _nFindParagraph(long ptr, long paragraphPtr);
    public static native void    _nPrintStatistics(long ptr);
    public static native void    _nSetEnabled(long ptr, boolean value);
    public static native int     _nGetCount(long ptr);
 }