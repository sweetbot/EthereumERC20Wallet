package Wallet.lib;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.spongycastle.crypto.digests.RIPEMD160Digest;
import org.spongycastle.util.encoders.Hex;

import Wallet.lib.exceptions.ValidationException;

public class ByteUtils {
	private static final char[] b58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
	private static final int[] r58 = new int[256];

	static {
		for (int i = 0; i < 256; ++i) {
			r58[i] = -1;
		}
		for (int i = 0; i < b58.length; ++i) {
			r58[b58[i]] = i;
		}
	}

	public static String toBase58(byte[] b) {
		if (b.length == 0) {
			return "";
		}

		int lz = 0;
		while (lz < b.length && b[lz] == 0) {
			++lz;
		}

		StringBuffer s = new StringBuffer();
		BigInteger n = new BigInteger(1, b);
		while (n.compareTo(BigInteger.ZERO) > 0) {
			BigInteger[] r = n.divideAndRemainder(BigInteger.valueOf(58));
			n = r[0];
			char digit = b58[r[1].intValue()];
			s.append(digit);
		}
		while (lz > 0) {
			--lz;
			s.append("1");
		}
		return s.reverse().toString();
	}

	public static byte[] keyHash(byte[] key) {
		byte[] ph = new byte[20];
		try {
			byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(key);
			RIPEMD160Digest digest = new RIPEMD160Digest();
			digest.update(sha256, 0, sha256.length);
			digest.doFinal(ph, 0);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return ph;
	}

	public static byte[] hash(byte[] data, int offset, int len) {
		try {
			MessageDigest a = MessageDigest.getInstance("SHA-256");
			a.update(data, offset, len);
			return a.digest(a.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] hash(byte[] data) {
		return hash(data, 0, data.length);
	}

	public static String toBase58WithChecksum(byte[] b) {
		byte[] cs = hash(b);
		byte[] extended = new byte[b.length + 4];
		System.arraycopy(b, 0, extended, 0, b.length);
		System.arraycopy(cs, 0, extended, b.length, 4);
		return toBase58(extended);
	}

	public static byte[] fromBase58WithChecksum(String s) throws ValidationException {
		byte[] b = fromBase58(s);
		if (b.length < 4) {
			throw new ValidationException("Too short for checksum " + s);
		}
		byte[] cs = new byte[4];
		System.arraycopy(b, b.length - 4, cs, 0, 4);
		byte[] data = new byte[b.length - 4];
		System.arraycopy(b, 0, data, 0, b.length - 4);
		byte[] h = new byte[4];
		System.arraycopy(hash(data), 0, h, 0, 4);
		if (Arrays.equals(cs, h)) {
			return data;
		}
		throw new ValidationException("Checksum mismatch " + s);
	}

	public static byte[] fromBase58(String s) throws ValidationException {
		try {
			boolean leading = true;
			int lz = 0;
			BigInteger b = BigInteger.ZERO;
			for (char c : s.toCharArray()) {
				if (leading && c == '1') {
					++lz;
				} else {
					leading = false;
					b = b.multiply(BigInteger.valueOf(58));
					b = b.add(BigInteger.valueOf(r58[c]));
				}
			}
			byte[] encoded = b.toByteArray();
			if (encoded[0] == 0) {
				if (lz > 0) {
					--lz;
				} else {
					byte[] e = new byte[encoded.length - 1];
					System.arraycopy(encoded, 1, e, 0, e.length);
					encoded = e;
				}
			}
			byte[] result = new byte[encoded.length + lz];
			System.arraycopy(encoded, 0, result, lz, encoded.length);

			return result;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ValidationException("Invalid character in address");
		} catch (Exception e) {
			throw new ValidationException(e);
		}
	}

	public static byte[] reverse(byte[] data) {
		for (int i = 0, j = data.length - 1; i < data.length / 2; i++, j--) {
			data[i] ^= data[j];
			data[j] ^= data[i];
			data[i] ^= data[j];
		}
		return data;
	}

	public static String toHex(byte[] data) {
		try {
			return new String(Hex.encode(data), "US-ASCII");
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

	public static byte[] fromHex(String hex) {
		return Hex.decode(hex);
	}

	public static boolean isLessThanUnsigned(long n1, long n2) {
		return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
	}

}
