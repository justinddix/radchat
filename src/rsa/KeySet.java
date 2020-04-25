package rsa;
import java.math.BigInteger;

public class KeySet {
	public BigInteger n;
	public BigInteger e;
	public BigInteger d;

	public KeySet(BigInteger n, BigInteger e, BigInteger d) {
		this.n = n;
		this.e = e;
		this.d = d;
	}
}