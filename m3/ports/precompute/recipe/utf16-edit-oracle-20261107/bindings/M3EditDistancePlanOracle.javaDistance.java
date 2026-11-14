    /**
     * The master owner under the proof domain of {@link #admit}: the shorter operand compiles to an
     * {@link M3EditDistancePlan} (its single-word Myers lane, at most 63 units) and the longer operand is the
     * value; the swapped orientation (the owner's two-row DP lane) must return the same distance. Declared
     * binding of the crate's candidate Myers method to the owner; the owner's wider bounds are not refused here.
     */
    //FLLDCIM: BIND - census L040: donor crate method javaDistance (candidate Myers body) replaced by the owner M3EditDistancePlan (Myers lane and DP lane); admit keeps the proof domain
    public static int javaDistance(char[] left, char[] right) {
        admit(left, right);
        char[] pattern = left.length <= right.length ? left : right;
        char[] text = left.length <= right.length ? right : left;
        int myers = M3EditDistancePlan.compile(CharBuffer.wrap(pattern)).distance(CharBuffer.wrap(text));
        int rows = M3EditDistancePlan.compile(CharBuffer.wrap(text)).distance(CharBuffer.wrap(pattern));
        if (myers != rows) throw new AssertionError("EDIT_LANE_DIFFERENTIAL: myers=" + myers + " rows=" + rows);
        return myers;
    }
