/*
 * @test
 * @summary Immutable byte atoms share private backing through array offsets
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3ByteSlicesTest
 */

import jdk.internal.m3.M3ByteSlices;

import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class M3ByteSlicesTest {
    public static void main(String[] args) {
        verifyInternCollisionAndSlice();
        verifyPagesAndGrowth();
        verifyRandomized();
        System.out.println("M3ByteSlicesTest PASS");
    }

    private static void verifyInternCollisionAndSlice() {
        final M3ByteSlices pool = new M3ByteSlices();
        final byte[] source = "hello world".getBytes(StandardCharsets.UTF_8);
        final long helloWorld = pool.intern(source);
        source[0] = (byte) 'x';
        require(Arrays.equals(pool.copyBytes(helloWorld),
                "hello world".getBytes(StandardCharsets.UTF_8)), "caller cannot mutate interned bytes");
        final long same = pool.intern("hello world".getBytes(StandardCharsets.UTF_8));
        require(helloWorld == same && pool.atomCount() == 1, "canonical interning");
        final long hello = pool.slice(helloWorld, 0, 5);
        final long world = pool.slice(helloWorld, 6, 5);
        require(pool.byteOffset(world) == pool.byteOffset(helloWorld) + 6, "offset slice");
        require(pool.pageId(hello) == pool.pageId(world), "zero-copy sharing");
        require(Arrays.equals(pool.copyBytes(hello), "hello".getBytes(StandardCharsets.UTF_8)),
                "slice payload");
        require(pool.asReadOnlyBuffer(world).remaining() == 5, "buffer limit");
        expect(ReadOnlyBufferException.class, () -> pool.asReadOnlyBuffer(world).put((byte) 2));
        pool.retain(hello);
        pool.retain(hello);
        require((pool.stateMask(hello) & M3ByteSlices.SHARED) != 0, "lease shared mask");
        require(!pool.forgetSlice(hello), "leased slice cannot be invalidated");
        pool.release(hello);
        pool.release(hello);
        require(pool.forgetSlice(hello), "unleased slice invalidates");
        expect(IllegalArgumentException.class, () -> pool.length(hello));
        require(!pool.forgetSlice(helloWorld), "interned canonical atom stays");
        final long recycled = pool.slice(helloWorld, 0, 5);
        require((int) hello == (int) recycled && hello != recycled,
                "generation prevents stale slice handle");
        final long aa = pool.intern("Aa".getBytes(StandardCharsets.UTF_8));
        final long bb = pool.intern("BB".getBytes(StandardCharsets.UTF_8));
        require(aa != bb, "real byte equality, not 32-bit hash collision");
        require(Arrays.hashCode("Aa".getBytes(StandardCharsets.UTF_8)) ==
                Arrays.hashCode("BB".getBytes(StandardCharsets.UTF_8)), "collision fixture");
    }

    private static void verifyPagesAndGrowth() {
        final M3ByteSlices pool = new M3ByteSlices();
        final byte[] big = new byte[70000];
        Arrays.fill(big, (byte) 77);
        long first = pool.intern(big);
        require(pool.length(first) == big.length, "large atom");
        byte[] more = new byte[64];
        long other = pool.intern(more);
        require(pool.pageId(first) != pool.pageId(other), "new page on full page");
        for (int i = 0; i < 2000; i++) {
            byte[] input = ("value:" + i).getBytes(StandardCharsets.UTF_8);
            long handle = pool.intern(input);
            require(Arrays.equals(input, pool.copyBytes(handle)), "growth preserves atom");
        }
        require(pool.atomCount() == 2002, "intern table growth");
    }

    private static void verifyRandomized() {
        final M3ByteSlices pool = new M3ByteSlices();
        final Random random = new Random(0x56781234L);
        for (int i = 0; i < 4000; i++) {
            byte[] input = new byte[random.nextInt(512)];
            random.nextBytes(input);
            final long atom = pool.intern(input);
            require(pool.intern(input) == atom, "re-intern");
            int start = input.length == 0 ? 0 : random.nextInt(input.length + 1);
            int len = random.nextInt(input.length - start + 1);
            final long slice = pool.slice(atom, start, len);
            require(Arrays.equals(pool.copyBytes(slice), Arrays.copyOfRange(input, start, start + len)),
                    "randomized slice value");
            require(pool.forgetSlice(slice), "discard view metadata");
            expect(IllegalArgumentException.class, () -> pool.byteAt(slice, 0));
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Expected " + type.getSimpleName());
        } catch (Throwable error) {
            if (!type.isInstance(error)) {
                throw new AssertionError(error);
            }
        }
    }

    private static void require(boolean value, String detail) {
        if (!value) {
            throw new AssertionError(detail);
        }
    }
}
