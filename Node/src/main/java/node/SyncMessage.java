package node;

import java.io.Serializable;
import java.util.ArrayList;

public class SyncMessage implements Serializable {
    public ArrayList<Block> blockchain = new ArrayList<Block>();

    public SyncMessage(ArrayList<Block> blockchain) {
        this.blockchain = blockchain;
    }
}
