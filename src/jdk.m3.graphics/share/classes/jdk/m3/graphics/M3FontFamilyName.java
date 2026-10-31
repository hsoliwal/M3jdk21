package jdk.m3.graphics;


public class M3FontFamilyName {
    public final String _name;
    public final String _language;

    @java.lang.SuppressWarnings("all")
    public M3FontFamilyName(final String name, final String language) {
        this._name = name;
        this._language = language;
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this._name;
    }

    @java.lang.SuppressWarnings("all")
    public String getLanguage() {
        return this._language;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof M3FontFamilyName)) return false;
        final M3FontFamilyName other = (M3FontFamilyName) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$_name = this.getName();
        final java.lang.Object other$_name = other.getName();
        if (this$_name == null ? other$_name != null : !this$_name.equals(other$_name)) return false;
        final java.lang.Object this$_language = this.getLanguage();
        final java.lang.Object other$_language = other.getLanguage();
        if (this$_language == null ? other$_language != null : !this$_language.equals(other$_language)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof M3FontFamilyName;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $_name = this.getName();
        result = result * PRIME + ($_name == null ? 43 : $_name.hashCode());
        final java.lang.Object $_language = this.getLanguage();
        result = result * PRIME + ($_language == null ? 43 : $_language.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FontFamilyName(_name=" + this.getName() + ", _language=" + this.getLanguage() + ")";
    }
}
