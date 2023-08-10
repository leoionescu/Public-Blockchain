package node;

import java.io.Serializable;

public class NewNodeMessage implements Serializable {
    public int port;
    String message;

    public NewNodeMessage(int port, String message) {
        this.port = port;
        this.message = message;
    }
}
