package org.m3.runtime.lazy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public final class LazyObjectParentTest {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("m3-lazy-test");
        AtomicInteger creations = new AtomicInteger();
        LazyObjectParent.Codec<StringBuilder> codec = new LazyObjectParent.Codec<>() {
            public byte[] encode(StringBuilder value) {
                return value.toString().getBytes(StandardCharsets.UTF_8);
            }
            public StringBuilder decode(byte[] bytes) {
                return new StringBuilder(new String(bytes, StandardCharsets.UTF_8));
            }
        };
        try (LazyObjectParent p = new LazyObjectParent(dir)) {
            var recreated = p.child("recreated", LazyObjectParent.LAZY | LazyObjectParent.RECREATE,
                    () -> { creations.incrementAndGet(); return new StringBuilder("fresh"); }, null);
            var swapped = p.child("swapped", LazyObjectParent.LAZY | LazyObjectParent.SWAP,
                    () -> new StringBuilder("saved"), codec);
            require(p.residentCount() == 0, "lazy creation");
            recreated.get();
            swapped.get().append("-modified");
            require(p.evictOne(), "first evicted");
            require(creations.get() == 1, "no unexpected recreation");
            require(recreated.get().toString().equals("fresh"), "recreation");
            try (var lease = swapped.pin()) {
                require(lease.value().toString().equals("saved-modified"), "restored swap");
                p.evictUpTo(2);
                require(lease.value().toString().equals("saved-modified"), "pin protected");
            }
            require(p.evictUpTo(2) >= 1, "reclaim unlocked");
            require(swapped.get().toString().equals("saved-modified"), "swap roundtrip");
        }
        try (var stream = Files.list(dir)) {
            require(stream.findAny().isEmpty(), "temporary pages cleaned up");
        }
        Files.delete(dir);
        System.out.println("PASS LazyObjectParentTest");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
