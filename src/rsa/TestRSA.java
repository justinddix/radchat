package rsa;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TestRSA {
	public static final int keySizeBits = 4096;

	public static void main(String[] args) throws UnsupportedEncodingException {

		KeySet G = RSA.generate(keySizeBits);

		System.out.println("Generated Key: \n");

		System.out.println("G.n.toString(): " + G.n.toString());
		System.out.println("G.e.toString(): " + G.e.toString());
		System.out.println("G.d.toString(): " + G.d.toString());
		
		byte[] hmb = new byte[] {-128, 0,0,1,0,0,-128,-128,0,0,1,127,124};
		//String hm = Hex.hexEncode(hmb);
		//hm = "0ccdabcdabcd";
		//hm = "00AF04E4";
		hmb = ("import java.net.*;\r\n" + 
				"\r\n" + 
				"import java.sql.Connection;\r\n" + 
				"import java.sql.PreparedStatement;\r\n" + 
				"import java.sql.ResultSet;\r\n" + 
				"import java.sql.SQLException;\r\n" + 
				"\r\n" + 
				"import java.util.ArrayList;\r\n" + 
				"import java.util.List;\r\n" + 
				"import java.util.ListIterator;\r\n" + 
				"import java.util.Set;\r\n" + 
				"\r\n" + 
				"import java.io.*;\r\n" + 
				"\r\n" + 
				"import java.math.BigInteger;\r\n" + 
				"\r\n" + 
				"import rsa.KeySet;\r\n" + 
				"import rsa.RSA;\r\n" + 
				"\r\n" + 
				"import messages.MessageTypes;\r\n" + 
				"\r\n" + 
				"import common.Constants;\r\n" + 
				"\r\n" + 
				"import model.ConnectionManager;\r\n" + 
				"\r\n" + 
				"public class MultiThreadedServer extends Thread {\r\n" + 
				"	private static").getBytes();
		
		System.out.println("hmb.length = " + hmb.length);

		System.out.println(new String(hmb));
		System.out.println("hm : " + Hex.hexEncode(hmb));
		byte[] ehm = RSA.encryptBytesToBytes(hmb, G.n, G.e, keySizeBits);
		
		System.out.println("ehm.length = " + ehm.length);
		
		System.out.println("ehm: " + Hex.hexEncode(ehm));
		
		byte[] dhm = RSA.decryptBytesToBytes(ehm, G.d, G.n, keySizeBits);
		
		System.out.println("dhm.length = " + dhm.length);
		
		System.out.println("dhm: " + Hex.hexEncode(dhm));
		System.out.println(new String(dhm));

		System.exit(0);

		System.out.println("\nMessage: \n");

//		String message = "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM"
//				       + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM"
//				       + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM"
//				       + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM";
		
		String message = "a\r\n" + 
				"";
		
		String m3 = "a\r\n" + 
				"";
		
		System.out.println("message: " + message);

		BigInteger m = new BigInteger(Hex.hexEncode(message), 16);
		System.out.println("Hex.hexEncode(message): " + Hex.hexEncode(message));		
		System.out.println("(m = new BigInteger(Hex.hexEncode(message), 16)).toString(): " + m.toString());
		System.out.println("(m = new BigInteger(Hex.hexEncode(message), 16)).toString(16): " + m.toString(16));

		System.out.println("\nEncrypt: \n");

		BigInteger c = RSA.encrypt(m, G.n, G.e);

		System.out.println("(c = RSA.encrypt(m, G.n, G.e)).toString(): " + c);

		System.out.println("\nDecrypt: \n");

		BigInteger dm = RSA.decrypt(c, G.d, G.n);

		System.out.println("(dm = RSA.decrypt(c, G.d, G.n)).toString(): " + dm.toString());
		System.out.println("dm.toString(16): " + dm.toString(16));
		System.out.println("Hex.hexDecode(dm.toString(16)): " + Hex.hexDecode(dm.toString(16)));
		
		//////////////////////
		String message2 = "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫"
				+ "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫"
				+ "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫"
				+ "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫"
				+ "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫"
				+ "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫"
				+ "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫"
				+ "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫"
				+ "疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫疫";
		// message2 = "USER2: 0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012301234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901230123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012301234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901230123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012301234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901230123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012301234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901230123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012301234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901230123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012301234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901230123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012301234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901230123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123";
		String encryptedMessage2 = RSA.encryptMessage(message2, G.n, G.e, keySizeBits);
		System.out.println("encryptedMessage2: " + encryptedMessage2);
		String decryptedMessage2 = RSA.decryptMessage(encryptedMessage2, G.d, G.n);
		System.out.println("decryptedMessage2: " + decryptedMessage2);
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} 
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		//
		byte[] hashInBytes = md.digest("ABC123█".getBytes("UTF-8"));
		// bytes to hex
		//
		StringBuilder sb = new StringBuilder();
		for (byte b : hashInBytes) {
			sb.append(String.format("%02x", b));
		}
		String hash = sb.toString();
		System.out.println("hash: " + hash);

		
	}
}