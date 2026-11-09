//
//  NOTE: The following source code is heavily derived from the
//  iHarder.net public domain Base64 library.  See the original at
//  http://iharder.sourceforge.net/current/java/base64/
//

package org.eclipse.jgit.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.jgit.internal.JGitText;

/**
 * Encodes and decodes to and from Base64 notation.
 * <p>
 * I am placing this code in the Public Domain. Do with it as you will. This
 * software comes with no guarantees or warranties but with plenty of
 * well-wishing instead! Please visit
 * <a href="http://iharder.net/base64">http://iharder.net/base64</a>
 * periodically to check for updates or to contribute improvements.
 * </p>
 * <p>
 * M3JDK (JGIT_BASE64_TO_JDK v1): java.base's {@code java.util.Base64} encodes, and
 * decodes input in the canonical form. Other input keeps JGit's lenient
 * decoding loop, with its results and exceptions.
 * </p>
 *
 * @author Robert Harder
 * @author rob@iharder.net
 */
public class Base64 {
	//FLLDCIM: M3 - JGIT_BASE64_TO_JDK v1: base64 is java.base's java.util.Base64, the JDK's one implementation
	/** The equals sign (=) as a byte. */
	private static final byte EQUALS_SIGN = (byte) '=';

	/** Indicates equals sign in encoding. */
	private static final byte EQUALS_SIGN_DEC = -1;

	/** Indicates white space in encoding. */
	private static final byte WHITE_SPACE_DEC = -2;

	/** Indicates an invalid byte during decoding. */
	private static final byte INVALID_DEC = -3;

	/** The 64 valid Base64 values. */
	private static final byte[] ENC;

	/**
	 * Translates a Base64 value to either its 6-bit reconstruction value or a
	 * negative number indicating some other meaning. The table is only 7 bits
	 * wide, as the 8th bit is discarded during decoding.
	 */
	private static final byte[] DEC;

	static {
		ENC = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" // //$NON-NLS-1$
				+ "abcdefghijklmnopqrstuvwxyz" // //$NON-NLS-1$
				+ "0123456789" // //$NON-NLS-1$
				+ "+/" // //$NON-NLS-1$
		).getBytes(UTF_8);

		DEC = new byte[128];
		Arrays.fill(DEC, INVALID_DEC);

		for (int i = 0; i < 64; i++)
			DEC[ENC[i]] = (byte) i;
		DEC[EQUALS_SIGN] = EQUALS_SIGN_DEC;

		DEC['\t'] = WHITE_SPACE_DEC;
		DEC['\n'] = WHITE_SPACE_DEC;
		DEC['\r'] = WHITE_SPACE_DEC;
		DEC[' '] = WHITE_SPACE_DEC;
	}

	/** Defeats instantiation. */
	private Base64() {
		// Suppress empty block warning.
	}

	/**
	 * Encodes a byte array into Base64 notation.
	 *
	 * @param source
	 *            The data to convert
	 * @return encoded base64 representation of source.
	 */
	public static String encodeBytes(byte[] source) {
		return encodeBytes(source, 0, source.length);
	}

	/**
	 * Encodes a byte array into Base64 notation.
	 *
	 * @param source
	 *            The data to convert
	 * @param off
	 *            Offset in array where conversion should begin
	 * @param len
	 *            Length of data to convert
	 * @return encoded base64 representation of source.
	 */
	public static String encodeBytes(byte[] source, int off, int len) {
		ByteBuffer encoded = java.util.Base64.getEncoder()
				.encode(ByteBuffer.wrap(source, off, len));
		return new String(encoded.array(), 0, encoded.limit(), UTF_8);
	}

	/**
	 * Decodes four bytes from array <var>source</var> and writes the resulting
	 * bytes (up to three of them) to <var>destination</var>. The source and
	 * destination arrays can be manipulated anywhere along their length by
	 * specifying <var>srcOffset</var> and <var>destOffset</var>. This method
	 * does not check to make sure your arrays are large enough to accommodate
	 * <var>srcOffset</var> + 4 for the <var>source</var> array or
	 * <var>destOffset</var> + 3 for the <var>destination</var> array. This
	 * method returns the actual number of bytes that were converted from the
	 * Base64 encoding.
	 *
	 * @param source
	 *            the array to convert
	 * @param srcOffset
	 *            the index where conversion begins
	 * @param destination
	 *            the array to hold the conversion
	 * @param destOffset
	 *            the index where output will be put
	 * @return the number of decoded bytes converted
	 */
	private static int decode4to3(byte[] source, int srcOffset,
			byte[] destination, int destOffset) {
		// Example: Dk==
		if (source[srcOffset + 2] == EQUALS_SIGN) {
			int outBuff = ((DEC[source[srcOffset]] & 0xFF) << 18)
					| ((DEC[source[srcOffset + 1]] & 0xFF) << 12);
			destination[destOffset] = (byte) (outBuff >>> 16);
			return 1;
		}

		// Example: DkL=
		else if (source[srcOffset + 3] == EQUALS_SIGN) {
			int outBuff = ((DEC[source[srcOffset]] & 0xFF) << 18)
					| ((DEC[source[srcOffset + 1]] & 0xFF) << 12)
					| ((DEC[source[srcOffset + 2]] & 0xFF) << 6);
			destination[destOffset] = (byte) (outBuff >>> 16);
			destination[destOffset + 1] = (byte) (outBuff >>> 8);
			return 2;
		}

		// Example: DkLE
		else {
			int outBuff = ((DEC[source[srcOffset]] & 0xFF) << 18)
					| ((DEC[source[srcOffset + 1]] & 0xFF) << 12)
					| ((DEC[source[srcOffset + 2]] & 0xFF) << 6)
					| ((DEC[source[srcOffset + 3]] & 0xFF));

			destination[destOffset] = (byte) (outBuff >> 16);
			destination[destOffset + 1] = (byte) (outBuff >> 8);
			destination[destOffset + 2] = (byte) (outBuff);

			return 3;
		}
	}

	/**
	 * Low-level decoding ASCII characters from a byte array.
	 *
	 * @param source
	 *            The Base64 encoded data
	 * @param off
	 *            The offset of where to begin decoding
	 * @param len
	 *            The length of characters to decode
	 * @return decoded data
	 * @throws java.lang.IllegalArgumentException
	 *             the input is not a valid Base64 sequence.
	 */
	public static byte[] decode(byte[] source, int off, int len) {
		if (source != null && off >= 0 && len > 0 && len <= source.length - off
				&& len % 4 == 0) {
			try {
				ByteBuffer decoded = java.util.Base64.getDecoder()
						.decode(ByteBuffer.wrap(source, off, len));
				byte[] out = decoded.array();
				return out.length == decoded.limit() ? out
						: Arrays.copyOf(out, decoded.limit());
			} catch (IllegalArgumentException notCanonical) {
				// Not the canonical form: JGit's lenient loop decodes it.
			}
		}
		return decodeLenient(source, off, len);
	}

	/**
	 * JGit's lenient decoding loop, the upstream body of
	 * {@link #decode(byte[], int, int)}: white space is skipped, the high bit
	 * of each byte is dropped, an unpadded final group is dropped and input
	 * after the padding is ignored.
	 */
	private static byte[] decodeLenient(byte[] source, int off, int len) {
		byte[] outBuff = new byte[len * 3 / 4]; // Upper limit on size of output
		int outBuffPosn = 0;

		byte[] b4 = new byte[4];
		int b4Posn = 0;

		for (int i = off; i < off + len; i++) {
			byte sbiCrop = (byte) (source[i] & 0x7f);
			byte sbiDecode = DEC[sbiCrop];

			if (EQUALS_SIGN_DEC <= sbiDecode) {
				b4[b4Posn++] = sbiCrop;
				if (b4Posn > 3) {
					outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn);
					b4Posn = 0;

					// If that was the equals sign, break out of 'for' loop
					if (sbiCrop == EQUALS_SIGN)
						break;
				}

			} else if (sbiDecode != WHITE_SPACE_DEC)
				throw new IllegalArgumentException(MessageFormat.format(
						JGitText.get().badBase64InputCharacterAt,
						Integer.valueOf(i), Integer.valueOf(source[i] & 0xff)));
		}

		if (outBuff.length == outBuffPosn)
			return outBuff;

		byte[] out = new byte[outBuffPosn];
		System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
		return out;
	}

	/**
	 * Decodes data from Base64 notation.
	 *
	 * @param s
	 *            the string to decode
	 * @return the decoded data
	 */
	public static byte[] decode(String s) {
		byte[] bytes = s.getBytes(UTF_8);
		return decode(bytes, 0, bytes.length);
	}
}
