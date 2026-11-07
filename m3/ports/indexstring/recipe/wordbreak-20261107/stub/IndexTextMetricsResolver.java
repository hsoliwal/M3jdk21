package com.synexia.indexstring;
/** Donor-side stub (differential only). */
public interface IndexTextMetricsResolver {
    int utf16Length(long id);
    char utf16CharAt(long id, int offset);
}
