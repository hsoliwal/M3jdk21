// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

/** Frozen member-kind code stored in the primitive M3Class member table. */
public enum M3MemberKind {
    FIELD(0),
    METHOD(1),
    CONSTRUCTOR(2),
    RECORD_COMPONENT(3);

    private final int code;

    M3MemberKind(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static M3MemberKind fromCode(int code) {
        return switch (code) {
            case 0 -> FIELD;
            case 1 -> METHOD;
            case 2 -> CONSTRUCTOR;
            case 3 -> RECORD_COMPONENT;
            default -> throw new IllegalArgumentException("unknown member kind: " + code);
        };
    }
}
