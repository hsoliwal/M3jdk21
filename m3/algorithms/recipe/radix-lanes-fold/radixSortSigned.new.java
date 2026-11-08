    /**
     * Stable four-pass LSD radix sort for all signed int values.
     * Signed order is achieved by toggling the sign bit before byte extraction.
     */
    public static void radixSortSigned(int[] values) {
        Objects.requireNonNull(values, "values");
        radixSortSigned(values, new int[values.length], new int[RADIX], ProgressMonitor.none());
    }
