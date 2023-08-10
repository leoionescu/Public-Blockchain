package wallet;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.math.BigInteger;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
//import org.bouncycastle.util.encoders.Base64;
import java.util.Base64;


public class Address {
    public static byte[] hash160(byte[] input) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] sha256hash = sha256.digest(input);
            MessageDigest ripemd160 = MessageDigest.getInstance("RIPEMD160");
            return ripemd160.digest(sha256hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hash256(byte[] input) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash1 = sha256.digest(input);
            byte[] hash2 = sha256.digest(hash1);
            return Arrays.copyOf(hash2, 4); // return first 4 bytes of hash2
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeAddress(PublicKey publicKey) {
        byte[] publicKeyBytes = publicKey.getEncoded();
        byte[] publicKeyHash = hash160(publicKeyBytes);
        byte[] versionedPublicKeyHash = new byte[1 + publicKeyHash.length];
        versionedPublicKeyHash[0] = 0x00; // version byte for Bitcoin
        System.arraycopy(publicKeyHash, 0, versionedPublicKeyHash, 1, publicKeyHash.length);
        byte[] checksum = hash256(versionedPublicKeyHash);
        byte[] addressBytes = new byte[versionedPublicKeyHash.length + 4];
        System.arraycopy(versionedPublicKeyHash, 0, addressBytes, 0, versionedPublicKeyHash.length);
        System.arraycopy(checksum, 0, addressBytes, versionedPublicKeyHash.length, 4);
        byte[] address = Base64.getEncoder().encode(addressBytes);
        System.out.println(address);
        return new String(address);
//        return address;
    }

}
