// expect: 3
import com.m3.collections.ewah.EWAHCompressedBitmap;

/** LIB_INTAKE smoke: the relocated JMOD is usable from the linked image. */
public class SMOKE {
    public static void main(String[] args) {
        System.out.println(EWAHCompressedBitmap.bitmapOf(1, 5, 9).cardinality());
    }
}
