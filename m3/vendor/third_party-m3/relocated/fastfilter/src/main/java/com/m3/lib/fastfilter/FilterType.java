package com.m3.lib.fastfilter;

import com.m3.lib.fastfilter.bloom.BlockedBloom;
import com.m3.lib.fastfilter.bloom.Bloom;
import com.m3.lib.fastfilter.bloom.count.*;
import com.m3.lib.fastfilter.cuckoo.Cuckoo16;
import com.m3.lib.fastfilter.cuckoo.Cuckoo8;
import com.m3.lib.fastfilter.cuckoo.CuckooPlus16;
import com.m3.lib.fastfilter.cuckoo.CuckooPlus8;
import com.m3.lib.fastfilter.gcs.GolombCompressedSet;
import com.m3.lib.fastfilter.xor.Xor16;
import com.m3.lib.fastfilter.xor.Xor8;
import com.m3.lib.fastfilter.xor.XorSimple;
import com.m3.lib.fastfilter.xor.XorSimple2;
import com.m3.lib.fastfilter.xorplus.XorPlus8;

/**
 * The list of supported approximate membership implementations.
 */
public enum FilterType {
    BLOOM {
        @Override
        public Filter construct(long[] keys, int setting) {
            return Bloom.construct(keys, setting);
        }
    },
    COUNTING_BLOOM {
        @Override
        public Filter construct(long[] keys, int setting) {
            return CountingBloom.construct(keys, setting);
        }
    },
    SUCCINCT_COUNTING_BLOOM {
        @Override
        public Filter construct(long[] keys, int setting) {
            return SuccinctCountingBloom.construct(keys, setting);
        }
    },
    SUCCINCT_COUNTING_BLOOM_RANKED {
        @Override
        public Filter construct(long[] keys, int setting) {
            return SuccinctCountingBloomRanked.construct(keys, setting);
        }
    },
    BLOCKED_BLOOM {
        @Override
        public Filter construct(long[] keys, int setting) {
            return BlockedBloom.construct(keys, setting);
        }
    },
    SUCCINCT_COUNTING_BLOCKED_BLOOM {
        @Override
        public Filter construct(long[] keys, int setting) {
            return SuccinctCountingBlockedBloom.construct(keys, setting);
        }
    },
    SUCCINCT_COUNTING_BLOCKED_BLOOM_RANKED {
        @Override
        public Filter construct(long[] keys, int setting) {
            return SuccinctCountingBlockedBloomRanked.construct(keys, setting);
        }
    },
    XOR_SIMPLE {
        @Override
        public Filter construct(long[] keys, int setting) {
            return XorSimple.construct(keys);
        }
    },
    XOR_SIMPLE_2 {
        @Override
        public Filter construct(long[] keys, int setting) {
            return XorSimple2.construct(keys);
        }
    },
    XOR_8 {
        @Override
        public Filter construct(long[] keys, int setting) {
            return Xor8.construct(keys);
        }
    },
    XOR_16 {
        @Override
        public Filter construct(long[] keys, int setting) {
            return Xor16.construct(keys);
        }
    },
    XOR_PLUS_8 {
        @Override
        public Filter construct(long[] keys, int setting) {
            return XorPlus8.construct(keys);
        }
    },
    CUCKOO_8 {
        @Override
        public Filter construct(long[] keys, int setting) {
            return Cuckoo8.construct(keys);
        }
    },
    CUCKOO_16 {
        @Override
        public Filter construct(long[] keys, int setting) {
            return Cuckoo16.construct(keys);
        }
    },
    CUCKOO_PLUS_8 {
        @Override
        public Filter construct(long[] keys, int setting) {
            return CuckooPlus8.construct(keys);
        }
    },
    CUCKOO_PLUS_16 {
        @Override
        public Filter construct(long[] keys, int setting) {
            return CuckooPlus16.construct(keys);
        }
    },
    GCS {
        @Override
        public Filter construct(long[] keys, int setting) {
            return GolombCompressedSet.construct(keys, setting);
        }
    };

    /**
     * Construct the filter with the given keys and the setting.
     *
     * @param keys the keys
     * @param setting the setting (roughly bits per fingerprint)
     * @return the constructed filter
     */
    public abstract Filter construct(long[] keys, int setting);

}