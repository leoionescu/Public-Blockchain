package node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.*;
import org.bson.Document;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class BlockChain {
    public int difficulty = 6;
    public ArrayList<Block> blockchain = new ArrayList<Block>();
    MongoClient mongoClient;
    MongoDatabase database;
    MongoCollection<Document> collection;

    public BlockChain(int port) {
        connectToDatabase(port);
        readDatabase();
//        System.out.println(blockchain);
    }

    public BlockChain(ArrayList<Block> blockchain) {
        this.blockchain = blockchain;
    }

    public void connectToDatabase(int port) {
        mongoClient = MongoClients.create();
        database = mongoClient.getDatabase("blockchain-" + String.valueOf(port));
//        System.out.println("connected to database");
    }

    public void readDatabase() {
        blockchain = new ArrayList<Block>();
        collection = database.getCollection("blocks");
        ModelMapper mapper = new ModelMapper();
        MongoCursor<Document> cursor = collection.find().iterator();

        while (cursor.hasNext()) {
            Document doc = cursor.next();

            Block block = new Block();
            block.hash = doc.get("hash").toString();
            block.previousHash = doc.get("previousHash").toString();
            block.timeStamp = (long) doc.get("timeStamp");
            block.nonce = (int) doc.get("nonce");
            block.minedBy = doc.get("minedBy").toString();
            block.merkleRoot = doc.get("merkleRoot").toString();
            ArrayList<Document> transactions = mapper.map(doc.get("transactions"), ArrayList.class);

            for (Document t : transactions) {
                Transaction transaction = new Transaction();
                transaction.hash = t.get("hash").toString();
                transaction.from = t.get("from").toString();
                transaction.to = t.get("to").toString();
                transaction.data = t.get("data").toString();
                transaction.timeStamp = (long) t.get("timeStamp");
                transaction.value = (double) t.get("value");

                block.transactions.add(transaction);
            }
            blockchain.add(block);
        }

        cursor.close();

        if(blockchain.size() == 0) {
            // create new Blockchain with genesis block
            System.out.println("Creating new blockchain with genesis block");
            ArrayList<Transaction> genesisTransactions = new ArrayList<Transaction>();
            genesisTransactions.add(new Transaction("0", "AARMQ1tUq8j6+5BY2NKatJxxndGzwjxHdg==", "", 1000000)); // total supply
            genesisTransactions.add(new Transaction("AARMQ1tUq8j6+5BY2NKatJxxndGzwjxHdg==", "ADssN/k5OuAotSVrd9+Fkk2JzTswWL53wA==", "", 10000));
            genesisTransactions.add(new Transaction("AARMQ1tUq8j6+5BY2NKatJxxndGzwjxHdg==", "AGBxTHhiZlsXxCEQv8EIBiKr3msNnO70bg==", "", 10000));
            Block genesisBlock = new Block(genesisTransactions, "0");
            addBlock(genesisBlock);
        }

        System.out.println("read database");
    }

    public void saveBlockChainToDatabase() {
        if(isChainValid()) {

            collection.deleteMany(new Document());

            for (Block block : blockchain) {

                ArrayList<Document> transactions = new ArrayList<Document>();

                for (Transaction t : block.transactions) {
                    transactions.add(new Document("hash", t.hash)
                            .append("from", t.from)
                            .append("to", t.to)
                            .append("data", t.data)
                            .append("timeStamp", t.timeStamp)
                            .append("value", t.value)
                    );
                }

                Document doc = new Document("hash", block.hash)
                        .append("previousHash", block.previousHash)
                        .append("merkleRoot", block.merkleRoot)
                        .append("timeStamp", block.timeStamp)
                        .append("nonce", block.nonce)
                        .append("minedBy", block.minedBy)
                        .append("transactions", transactions);

                collection.insertOne(doc);
            }
        }
    }

    public void sync(ArrayList<Block> blockchain) {
//        System.out.println("syncing in blockchain");
//        System.out.println(blockchain);
        if(blockchain.size() > this.blockchain.size()) {
            this.blockchain = blockchain;
            saveBlockChainToDatabase();
        }
    }

    public void addBlock(Block block) {

        blockchain.add(block);
        if(isChainValid()) {

            ArrayList<Document> transactions = new ArrayList<Document>();

            for (Transaction t : block.transactions) {
                transactions.add(new Document("hash", t.hash)
                        .append("from", t.from)
                        .append("to", t.to)
                        .append("data", t.data)
                        .append("timeStamp", t.timeStamp)
                        .append("value", t.value)
                );
            }

            Document doc = new Document("hash", block.hash)
                    .append("previousHash", block.previousHash)
                    .append("merkleRoot", block.merkleRoot)
                    .append("timeStamp", block.timeStamp)
                    .append("nonce", block.nonce)
                    .append("minedBy", block.minedBy)
                    .append("transactions", transactions);

//            System.out.println("adding block");
//            System.out.println(doc);

            collection.insertOne(doc);
//            System.out.println("Added block successfully");
        } else {
            readDatabase();
            System.out.println("Block already added");
        }
    }

    public Boolean isChainValid() {
        for(int i = 1; i < blockchain.size(); i++) {
            if(!blockchain.get(i).hash.equals(blockchain.get(i).calculateHash())) {
//                System.out.println("Wrong hash");
                return false;
            }
            if(!blockchain.get(i - 1).hash.equals(blockchain.get(i).previousHash)) {
//                System.out.println("Wrong previous hash");
                return false;
            }
            if(!blockchain.get(i).calculateMerkleRoot().equals(blockchain.get(i).merkleRoot)) {
//                System.out.println("Wrong merkle root");
                return false;
            }
        }
        return true;
    }

    public Block getBlock(int nonce) {
        return blockchain.get(nonce);
    }

    public Block getLastBlock() {
        return blockchain.get(blockchain.size() - 1);
    }
}
