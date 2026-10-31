// expect: 3
import com.m3.collections.ewah.EWAHCompressedBitmap;

/** LIB_INTAKE smoke: the relocated JMOD is usable from the linked image. */
public final class SMOKE {
    private SMOKE() {
    }

    /**
     * Prints the cardinality of a three-bit bitmap built through the relocated module.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println(EWAHCompressedBitmap.bitmapOf(1, 5, 9).cardinality());
    }
}
