package node;

import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import wallet.Address;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.Comparator;

public class Node {
    private ArrayList<NodeConnection> nodes = new ArrayList<NodeConnection>();
    private int myPort = 0;
    ServerSocket finalServerSocket;
    private ArrayList<Transaction> mempool = new ArrayList<Transaction>();
    BlockChain blockChain;
    Thread miningThread;
    public int syncMessagesCount = 0;

    Node() {
        try {
            init();
            listen();
            connectToNodes();
            blockChain = new BlockChain(myPort);
            sync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        int port;
        ServerSocket serverSocket = null;
        for(port = 8000; port < 8080; port++) {
            try {
                serverSocket = new ServerSocket(port);
                break;
            } catch (IOException ignored) {}
        }
        myPort = port;
        System.out.println("Node started on port: " + port);
        finalServerSocket = serverSocket;
    }

    public void mine() throws IOException {
        System.out.println("mining");
        miningThread = new Thread(() -> {
            while(true) {
                ArrayList<Transaction> transactions = new ArrayList<Transaction>(mempool);
                Block block = new Block(transactions, blockChain.getLastBlock());
                block.setMerkleRoot();
                block.mineBlock(blockChain.difficulty);
                block.setMinedBy(String.valueOf(myPort));
                blockChain.addBlock(block);
                // remove transactions
                for(int i = 0; i < block.transactions.size(); i++) {
                    for(int j = 0; j < mempool.size(); j++) {
                        if(mempool.get(j).isSameByHash(block.transactions.get(i))) {
                            mempool.remove(j);
                            j--;
                        }
                    }
                }
                System.out.println("Transactions in block: " + transactions);
                System.out.println("Transactions in mempool: " + mempool);
                try {
                    broadcastToNodes(block);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        miningThread.start();
    }

    public void sync() throws IOException {
        System.out.println("syncing");
        if(nodes.size() != 0) {
            ArrayList<BlockChain> receivedBlockchains = new ArrayList<>();
            for(NodeConnection node : nodes) {
                //request from nodes
                try {
                    Message syncMessage = new Message("Sync Request");
                    node.out.writeObject(syncMessage);
                    syncMessagesCount++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("only node in the network, no sync required");
            System.out.println("Chain is valid: " + blockChain.isChainValid());
            mine();
        }
    }

    public boolean checkSignature(Transaction transaction) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidKeySpecException {
        // Assuming you have the following components
        byte[] originalData = new Transaction(transaction.hash, transaction.from, transaction.to, transaction.data, transaction.timeStamp, transaction.value).toByteArray();
        byte[] signature = transaction.getSignature(); // Signature obtained from the signer

        PublicKey publicKey = transaction.getPublicKey();

        // Create a Signature instance
        Signature verifier = Signature.getInstance("SHA256withECDSA");

        // Initialize the verification process with the public key
        verifier.initVerify(publicKey);

        // Supply the original data
        verifier.update(originalData);

        // Perform the signature verification
        boolean isValid = verifier.verify(signature);

        // Check the result
        if (isValid) {
            System.out.println("Signature is valid.");
        } else {
            System.out.println("Signature is invalid.");
        }
        return true;
    }

    public boolean checkTransactionValidity(Transaction transaction) throws NoSuchAlgorithmException, SignatureException, InvalidKeySpecException, InvalidKeyException {
        // check for double spending
        // check balance
        // check public key
        //
        if(!checkSignature(transaction)) return false;
        return true;
    }

    public void broadcastToNodes(Object input) throws IOException {
        for(NodeConnection node : nodes) {
//            BufferedReader in = new BufferedReader(new InputStreamReader(node.getInputStream()));

            node.out.writeObject(input);
//            String response = in.readLine();
//            System.out.println(response);
        }
    }

    public void listen() throws IOException {
        Thread t = new Thread(() -> {
            while(true) {
                try {
                    Socket socket = finalServerSocket.accept();

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    while(true) {

                        Object input = in.readObject();
//                        System.out.println("Received message: " + input);

                        if(input instanceof NewNodeMessage) {
                            NewNodeMessage newNodeMessage = (NewNodeMessage) input;
                            if (newNodeMessage.message.equals("add node")) {
//
//                                out.writeObject(new String("node added"));
                                listen();

                                NodeConnection nodeConnection = new NodeConnection(socket, in, out);
                                nodes.add(nodeConnection);
//                                listenToNode(nodeConnection);
                                System.out.println("Connected to: " + nodes.size());
//                                out.writeObject(new String("Hello from server"));
//                                break;
                            }
                        } else if(input instanceof Transaction) {
                            Transaction transaction = (Transaction) input;
                            System.out.println("Transaction received: " + transaction.toString());
                            if(checkTransactionValidity(transaction)) {
                                mempool.add(transaction);
                            }
                            out.writeObject(new Message("Transaction received: " + transaction.toString()));
                            broadcastToNodes(input);
                        } else if(input instanceof SyncMessage) {
                            System.out.println("sync message received");
                            SyncMessage syncMessage = (SyncMessage) input;
                            BlockChain receivedBlockchain = new BlockChain((ArrayList<Block>) syncMessage.blockchain);
                            if(receivedBlockchain.isChainValid()) {
                                blockChain.sync(receivedBlockchain.blockchain);
//                                receivedBlockchains.add(receivedBlockchain);
                            }
                        } else if(input instanceof Block) {
                            System.out.println("mined block received");
                            Block block = (Block) input;
                            for(int i = 0; i < block.transactions.size(); i++) {
                                for(int j = 0; j < mempool.size(); j++) {
                                    if(mempool.get(j).isSameByHash(block.transactions.get(i))) {
                                        mempool.remove(j);
                                        j--;
                                    }
                                }
                            }
                            miningThread.stop();
//                            blockChain = new BlockChain();
                            blockChain.addBlock(block);
                            mine();
                        } else if(input instanceof Message) {
                                Message message = (Message) input;
                                if(message.message.equals("Sync Request")) {
                                    System.out.println("sync request received");
//                                    ObjectOutputStream out = new ObjectOutputStream(node.getOutputStream());
//                                    out.reset();
//                                out.writeObject(blockChain.blockchain);
                                    out.writeObject(new SyncMessage(blockChain.blockchain));
//                                    out.writeObject(new Message("fake blockchain"));
                                } else {
                                    System.out.println("message received: " + message.message);
                                }
                            } else {
                            System.out.println("input unknown");
                            System.out.println(input);
                        }
                    }

//                    socket.close();
//                    System.out.println("Client disconnected");
                } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException | InvalidKeyException e) {
//                    e.printStackTrace();
                }
            }
        });
       t.start();
    }

    public void listenToNode(NodeConnection node) throws IOException {
        new Thread(() -> {
            try {
                while(true) {
//                    ObjectInputStream in = new ObjectInputStream(node.getInputStream());
//                    PrintWriter out = new PrintWriter(node.getOutputStream(), true);

                    Object input = node.in.readObject();

                    if (input instanceof Transaction) {
                        Transaction transaction = (Transaction) input;
                        System.out.println("Transaction received: " + transaction.toString());
                        if(checkTransactionValidity(transaction)) {
                            mempool.add(transaction);
                        }
                    } else {
                        if(input instanceof Block) {
                            System.out.println("mined block received");
                            Block block = (Block) input;
                            for(int i = 0; i < block.transactions.size(); i++) {
                                for(int j = 0; j < mempool.size(); j++) {
                                    if(mempool.get(j).isSameByHash(block.transactions.get(i))) {
                                        mempool.remove(j);
                                        j--;
                                    }
                                }
                            }
                            miningThread.stop();
                            blockChain.addBlock(block);
//                            blockChain = new BlockChain();
//                            sync();
                            mine();
                        } else if(input instanceof SyncMessage) {
                            System.out.println("sync answer received");
                            SyncMessage syncMessage = (SyncMessage) input;
                            BlockChain receivedBlockchain = new BlockChain((ArrayList<Block>) syncMessage.blockchain);
                            if(receivedBlockchain.isChainValid()) {
                                blockChain.sync(receivedBlockchain.blockchain);
//                                receivedBlockchains.add(receivedBlockchain);
                            }
                            syncMessagesCount--;
                            if(syncMessagesCount == 0) {
                                System.out.println("sync completed");
                                System.out.println("Chain is valid: " + blockChain.isChainValid());
                                mine();
                            }
                        } else {
                            if(input instanceof Message) {
                                Message message = (Message) input;
                                System.out.println("message received: " + message.message);
                            } else {
                                System.out.println("input unknown");
                                System.out.println(input);
                            }
                        }
                    }

//                    System.out.println("sending success");
//                    out.println("success");
                }
            } catch (ClassNotFoundException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException | NullPointerException | InvalidKeyException e) {
//                e.printStackTrace();
            } catch(IOException e) {
//                e.printStackTrace();
                System.out.println("removing node");
                System.out.println("Connected to: " + nodes.size());
                nodes.remove(node);
            }
        }).start();
    }

    public void connectToNodes() {
        int port;
        Socket socket = null;
        for(port = 8000; port < 8005; port++) {
            if(port < myPort) {
                try {
                    socket = new Socket("localhost", port);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

                    out.writeObject(new NewNodeMessage(myPort, "add node"));

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                    NodeConnection nodeConnection = new NodeConnection(socket, in, out);
                    nodes.add(nodeConnection);
//                    System.out.println("node added");
//                    System.out.println(nodes);
                    listenToNode(nodeConnection);
                } catch (IOException e) {
//                e.printStackTrace();
                }
            }
        }
        System.out.println("Connected to: " + nodes.size());
//        System.out.println(nodes);
    }

    public static void main(String[] args) {
        Node node = new Node();
    }
}
