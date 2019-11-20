package node;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPooledClient {

    protected final Node curNode;
    protected final ExecutorService threadPool =
            Executors.newFixedThreadPool(10);

    public ThreadPooledClient(Node node) {
        curNode = node;
    }

    public ConnectionRunnable connectToNew(String ip, int serverPort) throws IOException {
        Socket serverSocket = null;
        try {
            serverSocket = new Socket(ip, serverPort);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error connecting to server", e);
        }
        ConnectionRunnable thread = new ConnectionRunnable(serverSocket, ip + ":" + serverPort, curNode);
        threadPool.execute(thread);
        return thread;
    }

    public void stop() {
        threadPool.shutdown();
    }
}
