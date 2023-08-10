package node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Block implements Serializable {
    public String hash;
    public String previousHash;
    public long timeStamp;
    public int nonce;
    public ArrayList<Transaction> transactions = new ArrayList<Transaction>();
    public String minedBy;
    public String merkleRoot;

    //Block Constructor.
    public Block(ArrayList<Transaction> transactions, Block previousBlock) {
        this.transactions = new ArrayList<>(transactions);
        this.nonce = previousBlock.nonce + 1;
        this.previousHash = previousBlock.hash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    // Block constructor for genesis block
    public Block(ArrayList<Transaction> transactions, String previousHash) {
        this.transactions = new ArrayList<>(transactions);
        this.nonce = 0;
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
        this.minedBy = "Satoshi";
        this.merkleRoot = calculateMerkleRoot();
    }

    public Block() {}

    public String calculateHash() {
        String calculatedhash = Utils.applySha256(
                previousHash +
                        Long.toString(timeStamp) +
                        Integer.toString(nonce) +
                        merkleRoot
        );
        return calculatedhash;
    }

    public void mineBlock(int difficulty) {
        long startTime = System.nanoTime();
        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while(!hash.substring( 0, difficulty).equals(target)) {
            nonce ++;
            hash = calculateHash();
        }
        long endTime = System.nanoTime();
        double elapsedTimeMillis = (double) (endTime - startTime) / 1_000_000.0;
        System.out.println("Block Mined!!! : " + hash + " in " + elapsedTimeMillis + " ms");
    }

    private String hash(String data) {
        return Utils.applySha256(data);
    }

    private String calculateCombinedHash(String hash1, String hash2) {
        String combinedHash = hash1 + hash2;
        return hash(combinedHash);
    }

    public String calculateMerkleRoot() {
        if(transactions.size() > 0) {
            ArrayList<String> hashes = new ArrayList<String>();
            for (Transaction transaction : transactions) {
                hashes.add(transaction.hash);
            }

            while (hashes.size() > 1) {
                ArrayList<String> nextLevelHashes = new ArrayList<>();
                int size = hashes.size();

                for (int i = 0; i < size - 1; i += 2) {
                    String hash1 = hashes.get(i);
                    String hash2 = hashes.get(i + 1);
                    String combinedHash = calculateCombinedHash(hash1, hash2);
                    nextLevelHashes.add(combinedHash);
                }

                hashes = nextLevelHashes;
            }

            return hashes.get(0);
        } else {
            return "0";
        }
    }

    public void setMerkleRoot() {
        merkleRoot = calculateMerkleRoot();
    }


    public void setTransactions(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
    }

    public String getMinedBy() {
        return minedBy;
    }

    public void setMinedBy(String minedBy) {
        this.minedBy = minedBy;
    }

    @Override
    public String toString() {
        return "Block{" +
                "hash='" + hash + '\'' +
                ", previousHash='" + previousHash + '\'' +
                ", timeStamp=" + timeStamp +
                ", nonce=" + nonce +
                ", transactions=" + transactions +
                '}';
    }
}
