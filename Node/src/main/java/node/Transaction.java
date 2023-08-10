package node;

import wallet.Address;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;

public class Transaction implements Serializable {
    public String hash;
    public String from;
    public String to;
    public String data;
    public long timeStamp;
    public double value;
    public byte[] publicKeyBytes;
    private byte[] signature;

    public Transaction (String from, String to, String data, double value) {
        this.from = from;
        this.to = to;
        this.data = data;
        this.value = value;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    public Transaction(String hash, String from, String to, String data, long timeStamp, double value) {
        this.hash = hash;
        this.from = from;
        this.to = to;
        this.data = data;
        this.timeStamp = timeStamp;
        this.value = value;
    }

    public Transaction() {}

    public String calculateHash() {
        String calculatedhash = Utils.applySha256(
                value +
                        Long.toString(timeStamp) +
                        data
        );
        return calculatedhash;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "hash='" + hash + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", data='" + data + '\'' +
                ", timeStamp=" + timeStamp +
                ", value=" + value +
//                ", signature=" + Arrays.toString(signature) +
                '}';
    }

    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(this);
            objectStream.flush();
            return byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignatureAndPublicKey(byte[] signature, byte[] publicKey) {
        this.signature = signature;
        this.publicKeyBytes = publicKey;
    }

    public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
         return bytesToPublicKey(publicKeyBytes);
    }

    public boolean isSameByHash(Transaction transaction) {
//        System.out.println("isSameByHash: " + hash + ";" + transaction.hash);
        if(hash.equals(transaction.hash)) return true;
        return false;
    }

    public static PublicKey bytesToPublicKey(byte[] publicKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
