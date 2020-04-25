package rsa;
import java.io.UnsupportedEncodingException;

public class Hex {
	public static String hexEncode(byte[] argInBytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : argInBytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static byte[] hexDecodeToBytes(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
		}
		return data;
	}
	
	public static String hexEncode(String arg) throws UnsupportedEncodingException {
		byte[] argInBytes = arg.getBytes("UTF-8");
		StringBuilder sb = new StringBuilder();
		for (byte b : argInBytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static String hexDecode(String hex) {
		try {
			int len = hex.length();
			byte[] data = new byte[len / 2];
			for (int i = 0; i < len; i += 2) {
				data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
			}
			return new String(data, "UTF-8");
		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
}