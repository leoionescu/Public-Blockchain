package node;

import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NodeConnection {
    public Socket socket;
    public ObjectInputStream in;
    public ObjectOutputStream out;

    public NodeConnection(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }
}
