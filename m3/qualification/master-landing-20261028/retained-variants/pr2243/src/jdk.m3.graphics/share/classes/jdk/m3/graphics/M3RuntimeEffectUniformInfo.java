package jdk.m3.graphics;


/**
 * Reflected description of a uniform variable in the {@link RuntimeEffect}'s SkSL
 */
public class M3RuntimeEffectUniformInfo {
    public final String _name;
    public final int _offset;
    public final M3RuntimeEffectUniformType _type;
    public final int _count;
    public final int _flags;

    public boolean isArray() {
        return (_flags & M3RuntimeEffectUniformFlag.ARRAY._value) != 0;
    }

    public boolean isColor() {
        return (_flags & M3RuntimeEffectUniformFlag.COLOR._value) != 0;
    }

    public int getSizeInBytes() {
        int sizeOfFloat = 4;
        int sizeOfInt = 4;
        int baseSize;
        switch (_type) {
        case FLOAT:
            baseSize = sizeOfFloat;
            break;
        case FLOAT2:
            baseSize = sizeOfFloat * 2;
            break;
        case FLOAT3:
            baseSize = sizeOfFloat * 3;
            break;
        case FLOAT4:
            baseSize = sizeOfFloat * 4;
            break;
        case FLOAT2X2:
            baseSize = sizeOfFloat * 4;
            break;
        case FLOAT3X3:
            baseSize = sizeOfFloat * 9;
            break;
        case FLOAT4X4:
            baseSize = sizeOfFloat * 16;
            break;
        case INT:
            baseSize = sizeOfInt;
            break;
        case INT2:
            baseSize = sizeOfInt * 2;
            break;
        case INT3:
            baseSize = sizeOfInt * 3;
            break;
        case INT4:
            baseSize = sizeOfInt * 4;
            break;
        default:
            throw new IllegalArgumentException("Unknown type: " + _type);
        }
        return baseSize * _count;
    }

    public M3RuntimeEffectUniformInfo(String name, int offset, int type, int count, int flags) {
        this(name, offset, M3RuntimeEffectUniformType._values[type], count, flags);
    }

    @java.lang.SuppressWarnings("all")
    public M3RuntimeEffectUniformInfo(final String name, final int offset, final M3RuntimeEffectUniformType type, final int count, final int flags) {
        if (name == null) {
            throw new java.lang.NullPointerException("name is marked non-null but is null");
        }
        if (type == null) {
            throw new java.lang.NullPointerException("type is marked non-null but is null");
        }
        this._name = name;
        this._offset = offset;
        this._type = type;
        this._count = count;
        this._flags = flags;
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this._name;
    }

    @java.lang.SuppressWarnings("all")
    public int getOffset() {
        return this._offset;
    }

    @java.lang.SuppressWarnings("all")
    public M3RuntimeEffectUniformType getType() {
        return this._type;
    }

    @java.lang.SuppressWarnings("all")
    public int getCount() {
        return this._count;
    }

    @java.lang.SuppressWarnings("all")
    public int getFlags() {
        return this._flags;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3RuntimeEffectUniformInfo)) return false;
        final M3RuntimeEffectUniformInfo other = (M3RuntimeEffectUniformInfo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getOffset() != other.getOffset()) return false;
        if (this.getCount() != other.getCount()) return false;
        if (this.getFlags() != other.getFlags()) return false;
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
        return other instanceof M3RuntimeEffectUniformInfo;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getOffset();
        result = result * PRIME + this.getCount();
        result = result * PRIME + this.getFlags();
        final java.lang.Object $_name = this.getName();
        result = result * PRIME + ($_name == null ? 43 : $_name.hashCode());
        final java.lang.Object $_type = this.getType();
        result = result * PRIME + ($_type == null ? 43 : $_type.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RuntimeEffectUniformInfo(_name=" + this.getName() + ", _offset=" + this.getOffset() + ", _type=" + this.getType() + ", _count=" + this.getCount() + ", _flags=" + this.getFlags() + ")";
    }
}
