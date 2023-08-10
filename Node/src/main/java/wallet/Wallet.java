package wallet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import node.Transaction;

import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

public class Wallet {
    KeyPair keyPair;
    Signature signature;
    public String address;
    Socket socket;
    ObjectInputStream in;
    ObjectOutputStream out;

    public Wallet(String filename){
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        try {
            readKeys(filename);
            System.out.println("Wallet read succesfully");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            System.out.println("Wallet does not exist");
            System.out.println("Creating wallet.....");
            generateKeyPair();
            try {
                saveKeys(filename);
            } catch (IOException ex) {
                System.out.println("Couldn't create wallet.");
            }
        }
        try {
            initSignature();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            e.printStackTrace();
        }
        address = Address.encodeAddress(keyPair.getPublic());
        System.out.println("public address: " + address);
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(ecSpec, random);   //256 bytes provides an acceptable security level
            keyPair = keyGen.generateKeyPair();

        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initSignature() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initSign(keyPair.getPrivate());
    }

    public void saveKeys(String filename) throws IOException {
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        byte[] publicKeyBytes = publicKey.getEncoded();
        byte[] privateKeyBytes = privateKey.getEncoded();

        DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
        dos.writeInt(publicKeyBytes.length);
        dos.write(publicKeyBytes);
        dos.writeInt(privateKeyBytes.length);
        dos.write(privateKeyBytes);
    }

    public void readKeys(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        byte[] publicKeyBytes;
        byte[] privateKeyBytes;

        DataInputStream dis = new DataInputStream(new FileInputStream(filename));
        publicKeyBytes = new byte[dis.readInt()];
        dis.readFully(publicKeyBytes);
        privateKeyBytes = new byte[dis.readInt()];
        dis.readFully(privateKeyBytes);


        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        keyPair = new KeyPair(publicKey, privateKey);
    }

    public void connectToBlockchain() throws IOException {
        socket = new Socket("localhost", 8000);

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

//        out.writeObject("Hello from client");
//        System.out.println("Received message: " + response);

    }

    public void disconnectFromBlockchain() throws IOException {
        socket.close();
    }

    public void signTransaction(Transaction transaction) throws SignatureException {
        byte[] transactionBytes = transaction.toByteArray();
        signature.update(transactionBytes);
        transaction.setSignatureAndPublicKey(signature.sign(), keyPair.getPublic().getEncoded());
    }

    public void sendTransaction(Transaction transaction) throws IOException, SignatureException {
        signTransaction(transaction);
        out.writeObject(transaction);
        System.out.println("Transaction sent" + transaction.toString());

//        String response = in.readLine();
//        System.out.println("Received message: " + response);
    }

    public static void main(String[] args) {
        Wallet wallet = new Wallet("1.txt");
        try {
            wallet.connectToBlockchain();

            Transaction transaction = new Transaction(wallet.address, "0x12", "test", 1.0f);
            wallet.sendTransaction(transaction);

            Thread.sleep(2000);

            transaction = new Transaction(wallet.address, "0x13", "test", 2.0f);
            wallet.sendTransaction(transaction);

            Thread.sleep(2000);

            transaction = new Transaction(wallet.address, "0x14", "test", 3.0f);
            wallet.sendTransaction(transaction);

            Thread.sleep(2000);

            transaction = new Transaction(wallet.address, "0x15", "test", 4.0f);
            wallet.sendTransaction(transaction);

            Thread.sleep(2000);

            transaction = new Transaction(wallet.address, "0x16", "test", 5.0f);
            wallet.sendTransaction(transaction);

        } catch (IOException e) {
            System.out.println("Couldn't connect to blockchain.....");
            e.printStackTrace();
        } catch (SignatureException e) {
            System.out.println("Couldn't send transaction.....");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
