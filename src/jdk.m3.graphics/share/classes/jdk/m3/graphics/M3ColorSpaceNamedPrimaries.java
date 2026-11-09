package jdk.m3.graphics;


public enum M3ColorSpaceNamedPrimaries {
    REC709(1),
    REC470_SYSTEM_M(4),
    REC470_SYSTEM_BG(5),
    REC601(6),
    SMPTE_ST_240(7),
    GENERIC_FILM(8),
    REC2020(9),
    SMPTE_ST_428_1(10),
    SMPTE_RP_431_2(11),
    SMPTE_EG_432_1(12),
    ITU_T_H273_VALUE22(22);

    public final int _value;

    M3ColorSpaceNamedPrimaries(int value) {
        _value = value;
    }
}
