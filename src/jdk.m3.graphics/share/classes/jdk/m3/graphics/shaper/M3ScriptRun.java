package jdk.m3.graphics.shaper;

import jdk.m3.graphics.*;

public class M3ScriptRun {
    public final int _end;
    public final int _scriptTag;

    public M3ScriptRun(int end, String script) {
        this(end, M3FourByteTag.fromString(script));
    }

    /**
     * Should be iso15924 codes.
     */
    public String getScript() {
        return M3FourByteTag.toString(_scriptTag);
    }

    @java.lang.SuppressWarnings("all")
    public M3ScriptRun(final int end, final int scriptTag) {
        this._end = end;
        this._scriptTag = scriptTag;
    }

    @java.lang.SuppressWarnings("all")
    public int getEnd() {
        return this._end;
    }

    @java.lang.SuppressWarnings("all")
    public int getScriptTag() {
        return this._scriptTag;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3ScriptRun)) return false;
        final M3ScriptRun other = (M3ScriptRun) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getEnd() != other.getEnd()) return false;
        if (this.getScriptTag() != other.getScriptTag()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3ScriptRun;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getEnd();
        result = result * PRIME + this.getScriptTag();
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ScriptRun(_end=" + this.getEnd() + ", _scriptTag=" + this.getScriptTag() + ")";
    }
}
