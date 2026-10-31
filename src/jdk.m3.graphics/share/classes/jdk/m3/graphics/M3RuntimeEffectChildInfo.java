package jdk.m3.graphics;


/**
 * Reflected description of a uniform child (shader or colorFilter) in the {@link RuntimeEffect}'s SkSL
 */
public class M3RuntimeEffectChildInfo {
    public final String _name;
    public final M3RuntimeEffectChildType _type;
    public final int _index;

    public M3RuntimeEffectChildInfo(String name, int type, int index) {
        this(name, M3RuntimeEffectChildType._values[type], index);
    }

    @java.lang.SuppressWarnings("all")
    public M3RuntimeEffectChildInfo(final String name, final M3RuntimeEffectChildType type, final int index) {
        if (name == null) {
            throw new java.lang.NullPointerException("name is marked non-null but is null");
        }
        if (type == null) {
            throw new java.lang.NullPointerException("type is marked non-null but is null");
        }
        this._name = name;
        this._type = type;
        this._index = index;
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this._name;
    }

    @java.lang.SuppressWarnings("all")
    public M3RuntimeEffectChildType getType() {
        return this._type;
    }

    @java.lang.SuppressWarnings("all")
    public int getIndex() {
        return this._index;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3RuntimeEffectChildInfo)) return false;
        final M3RuntimeEffectChildInfo other = (M3RuntimeEffectChildInfo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getIndex() != other.getIndex()) return false;
        final java.lang.Object this$_name = this.getName();
        final java.lang.Object other$_name = other.getName();
        if (this$_name == null ? other$_name != null : !this$_name.equals(other$_name)) return false;
        final java.lang.Object this$_type = this.getType();
        final java.lang.Object other$_type = other.getType();
        if (this$_type == null ? other$_type != null : !this$_type.equals(other$_type)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3RuntimeEffectChildInfo;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getIndex();
        final java.lang.Object $_name = this.getName();
        result = result * PRIME + ($_name == null ? 43 : $_name.hashCode());
        final java.lang.Object $_type = this.getType();
        result = result * PRIME + ($_type == null ? 43 : $_type.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RuntimeEffectChildInfo(_name=" + this.getName() + ", _type=" + this.getType() + ", _index=" + this.getIndex() + ")";
    }
}
