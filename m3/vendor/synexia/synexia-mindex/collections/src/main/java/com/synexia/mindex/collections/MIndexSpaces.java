// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import com.synexia.mindex.MIndexString;
import com.synexia.mindex.collection.MIndexDomain;
import com.synexia.mindex.collection.MIndexStringDomain;
import java.util.Objects;

/** Factory and direct helpers for common Synexia index spaces. */
public final class MIndexSpaces {
    private MIndexSpaces() { }

    public static MIndexSpace<MIndexString> strings() {
        return StringSpace.INSTANCE;
    }

    public static int stringId(CharSequence value) {
        return MIndexString.from(Objects.requireNonNull(value, "value")).contentIndex();
    }

    public static int findStringId(CharSequence value) {
        return MIndexString.findContentIndex(Objects.requireNonNull(value, "value"));
    }

    public static <T> MIndexObjectSpace<T> values() {
        return new MIndexObjectSpace<>();
    }

    public static <T> MIndexIdentitySpace<T> identities() {
        return new MIndexIdentitySpace<>();
    }

    /**
     * Project the legacy runtime MIndexDomain contract into the canonical
     * collection MIndexSpace vocabulary without changing primitive IDs.
     *
     * <p>The global legacy String domain is returned as the existing canonical
     * String space directly. Other domains use an adapter whose identity
     * authority is the wrapped legacy domain, so multiple adapter instances
     * still canonicalize as one semantic domain.
     */
    @SuppressWarnings("unchecked")
    public static <E> MIndexSpace<E> fromDomain(
            MIndexDomain<E> domain) {
        MIndexDomain<E> actual =
                Objects.requireNonNull(domain, "domain");
        if ((Object) actual == MIndexStringDomain.INSTANCE) {
            return (MIndexSpace<E>) strings();
        }
        return new MIndexLegacyDomainSpace<>(actual);
    }

    public static MIndexCompositeSpace composites(MIndexCompositeIndex index) {
        return Objects.requireNonNull(index, "index").space();
    }

    private enum StringSpace implements MIndexSpace<MIndexString> {
        INSTANCE;

        @Override
        public int id(MIndexString value) {
            return Objects.requireNonNull(value, "value").contentIndex();
        }

        @Override
        public int findId(MIndexString value) {
            return Objects.requireNonNull(value, "value").contentIndex();
        }

        @Override
        public MIndexString value(int id) {
            IdSupport.checkId(id);
            return MIndexString.fromContentIndex(id);
        }

        @Override
        public int size() {
            return MIndexString.pooledValues();
        }

        @Override
        public boolean javaEqualityCompatible() {
            return true;
        }

        @Override
        public boolean containsId(int id) {
            return id >= 0 && id < MIndexString.pooledValues();
        }
    }
}
