    /**
     * Stable four-pass LSD radix sort for all signed int values.
     * Signed order is achieved by toggling the sign bit before byte extraction.
     */
    public static void radixSortSigned(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length < 2) return;
        int[] source = values;
        int[] target = new int[values.length];
        int[] counts = new int[256];
        for (int shift = 0; shift < 32; shift += 8) {
            Arrays.fill(counts, 0);
            for (int value : source) {
                int transformed = value ^ Integer.MIN_VALUE;
                counts[(transformed >>> shift) & 0xff]++;
            }
            int total = 0;
            for (int bucket = 0; bucket < counts.length; bucket++) {
                int count = counts[bucket];
                counts[bucket] = total;
                total += count;
            }
            for (int value : source) {
                int transformed = value ^ Integer.MIN_VALUE;
                int bucket = (transformed >>> shift) & 0xff;
                target[counts[bucket]++] = value;
            }
            int[] swap = source;
            source = target;
            target = swap;
        }
        if (source != values) System.arraycopy(source, 0, values, 0, values.length);
    }
