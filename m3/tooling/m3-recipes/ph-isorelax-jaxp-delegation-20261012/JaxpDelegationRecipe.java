// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Hash-pinned, deterministic source recipe for JAXP entity-expansion delegation.
 * The upstream source image is immutable; this recipe produces a separate postimage.
 */
public final class JaxpDelegationRecipe {
    private static final String PREIMAGE_GIT_BLOB = "6d251eda4e40ae78be8134d61f4606cf932a6b84";
    private static final String GETTER_BEFORE = """
          public boolean isExpandEntityReference ()
          {
            return _WrappedFactory.isExpandEntityReferences ();
          }
        """;
    private static final String GETTER_AFTER = """
          @Override
          public boolean isExpandEntityReferences ()
          {
            return _WrappedFactory.isExpandEntityReferences ();
          }

          /** Preserves the historical singular alias. */
          public boolean isExpandEntityReference ()
          {
            return isExpandEntityReferences ();
          }
        """;
    private static final String SETTER_BEFORE = """
          public void setExpandEntityReference (final boolean expandEntityRef)
          {
            _WrappedFactory.setExpandEntityReferences (expandEntityRef);
          }
        """;
    private static final String SETTER_AFTER = """
          @Override
          public void setExpandEntityReferences (final boolean expandEntityRef)
          {
            _WrappedFactory.setExpandEntityReferences (expandEntityRef);
          }

          /** Preserves the historical singular alias. */
          public void setExpandEntityReference (final boolean expandEntityRef)
          {
            setExpandEntityReferences (expandEntityRef);
          }
        """;

    private JaxpDelegationRecipe() { }

    public static String apply(String source) throws Exception {
        if (source == null) throw new IllegalArgumentException("source is null");
        if (isPostimage(source)) return source;
        if (!PREIMAGE_GIT_BLOB.equals(gitBlob(source))) {
            throw new IllegalArgumentException("source blob drift; refusing transformation");
        }
        if (count(source, GETTER_BEFORE) != 1 || count(source, SETTER_BEFORE) != 1) {
            throw new IllegalArgumentException("expected JAXP singular-method preimages exactly once");
        }
        String candidate = source.replace(GETTER_BEFORE, GETTER_AFTER)
                .replace(SETTER_BEFORE, SETTER_AFTER);
        if (!isPostimage(candidate)) throw new IllegalStateException("postimage contract failed");
        return candidate;
    }

    private static boolean isPostimage(String source) {
        if (count(source, GETTER_AFTER) != 1 || count(source, SETTER_AFTER) != 1
                || count(source, GETTER_BEFORE) != 0 || count(source, SETTER_BEFORE) != 0) {
            return false;
        }
        try {
            String restored = source.replace(GETTER_AFTER, GETTER_BEFORE)
                    .replace(SETTER_AFTER, SETTER_BEFORE);
            return PREIMAGE_GIT_BLOB.equals(gitBlob(restored));
        } catch (Exception unavailableDigest) {
            return false;
        }
    }

    private static int count(String source, String needle) {
        int count = 0, from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static String gitBlob(String source) throws Exception {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        byte[] header = ("blob " + bytes.length + "\0").getBytes(StandardCharsets.US_ASCII);
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(header);
        return HexFormat.of().formatHex(sha1.digest(bytes));
    }
}
