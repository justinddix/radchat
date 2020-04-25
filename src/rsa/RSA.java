package rsa;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Random;

/**
 * RSA hash function reference implementation.
 * Uses BigInteger.js https://github.com/peterolson/BigInteger.js
 * Code originally based on https://github.com/kubrickology/Bitcoin-explained/blob/master/RSA.js
 */
public class RSA {

	/**
	 * Generates a k-bit RSA public/private key pair
	 * https://en.wikipedia.org/wiki/RSA_(cryptosystem)#Code
	 *
	 * @param   {keysize} int, bitlength of desired RSA modulus n (should be even)
	 * @returns {KeySet} Result of RSA generation (object with three bigInt members: n, e, d)
	 */
	public static KeySet generate(int keysize) {
		Random rnd = new Random(System.currentTimeMillis());
		//rnd = new Random(489734592923409274L);

		// set up variables for key generation
		BigInteger e = new BigInteger("65537");  // use fixed public exponent
		BigInteger p;
		BigInteger q;
		BigInteger lambda;

		// generate p and q such that Î»(n) = lcm(p âˆ’ 1, q âˆ’ 1) is coprime with e and |p-q| >= 2^(keysize/2 - 100)
		do {
			p = BigInteger.probablePrime(keysize / 2, rnd);
			q = BigInteger.probablePrime(keysize / 2, rnd);
			lambda = lcm(p.subtract(BigInteger.ONE), q.subtract(BigInteger.ONE));
		} while (!(e.gcd(lambda).equals(BigInteger.ONE)) || p.subtract(q).abs().shiftRight(keysize / 2 - 100).equals(BigInteger.ZERO));

		return new KeySet(p.multiply(q), e, e.modInverse(lambda));

		//n: p.multiply(q),  // public key (part I)
		//e: e,  // public key (part II)
		//d: e.modInv(lambda),  // private key d = e^(-1) mod Î»(n)
	}

	/** Encrypt bytes to bytes
	 * 
	 * Encrypts a byte array
	 * returns an encrypted byte array
	 */
	public static byte[] encryptBytesToBytes(byte[] block, BigInteger n, BigInteger e, int keySizeBits) {
		byte[] blockWithOnePrependedByte = new byte[block.length + 1];
		blockWithOnePrependedByte[0] = 0x01;
		for (int i = 0; i < block.length; i++) {
			blockWithOnePrependedByte[i+1] = block[i];
		}		
		BigInteger m = new BigInteger(blockWithOnePrependedByte);
		BigInteger c = RSA.encrypt(m, n, e);
		byte[] cBytes = c.toByteArray();

		if (cBytes.length == 512) {
			byte[] cBytesWithOnePrependedByte = new byte[cBytes.length + 1];
			cBytesWithOnePrependedByte[0] = 0x01;
			for (int i = 0; i < cBytes.length; i++) {
				cBytesWithOnePrependedByte[i+1] = cBytes[i];
			}	
			return cBytesWithOnePrependedByte;
		}
		else if (cBytes.length == 511) {
			byte[] cBytesWithTwoPrependedBytes = new byte[cBytes.length + 2];
			cBytesWithTwoPrependedBytes[0] = 0x02;
			cBytesWithTwoPrependedBytes[1] = 0x02;
			for (int i = 0; i < cBytes.length; i++) {
				cBytesWithTwoPrependedBytes[i+2] = cBytes[i];
			}	
			return cBytesWithTwoPrependedBytes;
		}
		return cBytes;
	}

	/** Decrypt bytes to bytes
	 * 
	 * Decrypts byte arrray
	 */
	public static byte[] decryptBytesToBytes(byte[] block, BigInteger d, BigInteger n, int keySizeBits) {
		if (block[0] == 0x01) {
			byte blockWithTheFirstByteRemoved[] = new byte[block.length - 1];
			for (int i = 0; i < blockWithTheFirstByteRemoved.length; i++) {
				blockWithTheFirstByteRemoved[i] = block[i+1];
			}
			block = blockWithTheFirstByteRemoved;
		}
		else if (block[0] == 0x02) {
			byte blockWithTheFirstTwoBytesRemoved[] = new byte[block.length - 2];
			for (int i = 0; i < blockWithTheFirstTwoBytesRemoved.length; i++) {
				blockWithTheFirstTwoBytesRemoved[i] = block[i+2];
			}
			block = blockWithTheFirstTwoBytesRemoved;
		}

		BigInteger c = new BigInteger(block);
		BigInteger m = RSA.decrypt(c, d, n);
		byte[] mBytes = m.toByteArray();

		byte mBytesWithTheFirstByteRemoved[] = new byte[mBytes.length - 1];
		for (int i = 0; i < mBytesWithTheFirstByteRemoved.length; i++) {
			mBytesWithTheFirstByteRemoved[i] = mBytes[i+1];
		}

		return mBytesWithTheFirstByteRemoved;
	}

	/** Encrypt hex message
	 * 
	 * Encrypts a hex number, represented as a string of hexits.
	 * returns an encrypted hex number represented as a string of (keySizeBits / 8) * 2 hexits.
	 */
	public static String encryptHexBlockToHexBlock(String hexMessage, BigInteger n, BigInteger e, int keySizeBits) {
		StringBuilder pad = new StringBuilder(hexMessage);
		for (int i = hexMessage.length() / 2; i < keySizeBits / 8 - 1; i++) {
			pad.append("00");
		}
		BigInteger m = new BigInteger(pad.toString(), 16);
		BigInteger c = RSA.encrypt(m, n, e);

		String cHex = c.toString(16);
		// Replace leading zeros omitted by BigInteger.toString(16);
		int diff = (keySizeBits / 8) * 2 - cHex.length();
		StringBuilder pad2 = new StringBuilder("");
		for (int j = 0; j < diff; j++) {
			pad2.append("0");
		}
		cHex = pad2.toString() + cHex;

		return cHex;

	}

	/** Decrypt Message to hex
	 * 
	 * Decrypts message from g-delimited blocks
	 */
	public static String decryptHexBlockToHexBlock(String hexMessage, BigInteger d, BigInteger n, int keySizeBits) {

		BigInteger dm = RSA.decrypt(new BigInteger(hexMessage, 16), d, n);
		String dmHex = dm.toString(16);

		// Replace leading zeros omitted by BigInteger.toString(16);
		int diff = (keySizeBits / 8 - 1) * 2 - dmHex.length();
		StringBuilder pad = new StringBuilder("");
		for (int j = 0; j < diff; j++) {
			pad.append("0");
		}
		dmHex = pad.toString() + dmHex;

		return dmHex;
	}

	/** Encrypt message
	 * 
	 * Encrypts message into g-delimited blocks
	 */
	public static String encryptMessage(String message, BigInteger n, BigInteger e, int keySizeBits) {
		int blockSizeOctets = keySizeBits / 8 / 4;

		int nBlocks = (int)Math.ceil((double)message.length() / (double)blockSizeOctets);
		String[] blocks = new String[nBlocks];

		int j = 0;
		for (int i = 0; i < nBlocks * blockSizeOctets; i += blockSizeOctets) {
			blocks[j] = message.substring(i, Math.min(i + blockSizeOctets, message.length()));
			j++;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nBlocks; i++) {
			try {
				BigInteger m = new BigInteger(Hex.hexEncode(blocks[i]), 16);
				BigInteger c = RSA.encrypt(m, n, e);
				sb.append(c.toString());
				if (i != nBlocks - 1) sb.append("g");
			} 
			catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}			
		}
		return sb.toString();
	}

	/** Decrypt Message
	 * 
	 * Decrypts message from g-delimited blocks
	 */
	public static String decryptMessage(String message, BigInteger d, BigInteger n) {
		String[] blocks = message.split("g");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < blocks.length; i++) {
			BigInteger dm = RSA.decrypt(new BigInteger(blocks[i]), d, n);
			String dmHex = dm.toString(16);
			if (dmHex.length() % 2 != 0) {
				// BigInteger omitted a leading 0
				dmHex = "0" + dmHex;
			}
			String dms = Hex.hexDecode(dmHex);
			sb.append(dms);
		}
		return sb.toString();
	}


	/**
	 * Encrypt
	 *
	 * @param   {m} int / bigInt: the 'message' to be encoded
	 * @param   {n} int / bigInt: n value returned from RSA.generate() aka public key (part I)
	 * @param   {e} int / bigInt: e value returned from RSA.generate() aka public key (part II)
	 * @returns {bigInt} encrypted message
	 */
	public static BigInteger encrypt(BigInteger m, BigInteger n, BigInteger e) {
		return m.modPow(e, n);
	}

	/**
	 * Decrypt
	 *
	 * @param   {c} int / bigInt: the 'message' to be decoded (encoded with RSA.encrypt())
	 * @param   {d} int / bigInt: d value returned from RSA.generate() aka private key
	 * @param   {n} int / bigInt: n value returned from RSA.generate() aka public key (part I)
	 * @returns {bigInt} decrypted message
	 */
	public static BigInteger decrypt(BigInteger c, BigInteger d, BigInteger n) {
		return c.modPow(d, n);
	}

	static BigInteger lcm(BigInteger a, BigInteger b) {
		return a.multiply(b.divide(a.gcd(b)));
	}

	/**
	 * Generates a random k-bit prime greater than âˆš2 Ã— 2^(k-1)
	 *
	 * @param   {bits} int, bitlength of desired prime
	 * @returns {bigInt} a random generated prime
	 */
	//	BigInteger randomPrime(int bits) {
	//		BigInteger min = new BigInteger("6074001000").shiftLeft(bits - 33);  // min â‰ˆ âˆš2 Ã— 2^(bits - 1)
	//		BigInteger max = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);  // max = 2^(bits) - 1
	//		for (;;) {
	//			BigInteger p = BigInteger.randBetween(min, max);  // WARNING: not a cryptographically secure RNG!
	//			if (p.isProbablePrime(256)) {
	//				return p;
	//			}
	//		}
	//	}
}