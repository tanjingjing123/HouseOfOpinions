package node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ConnectionRunnable implements Runnable {

    protected NodeEventListener listener;
    protected boolean isStopped = false;
    protected Socket socket = null;
    protected DataInputStream input;
    protected DataOutputStream output;
    protected String threadKey = null;

    public ConnectionRunnable(Socket socket, String threadKey, NodeEventListener listener) throws IOException {
        this.socket = socket;
        this.listener = listener;
        this.threadKey = threadKey;
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            while (!isStopped()) {
                processData();
            }

        } catch (IOException e) {
            System.out.println("Connection closed " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing!");
            }
            if (threadKey != null) {
                listener.removeThread(threadKey);
            }
        }
    }

    public void sendMyAddressToPeer(String ip, int port) throws IOException {
        output.writeUTF("My IP:" + ip + ":" + port);
    }

    public void sendPeerAddressToPeer(String ip, int port) throws IOException {
        output.writeUTF("Peer IP:" + ip + ":" + port);
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
    }

    private void processData() throws IOException {
        String msg = input.readUTF();
        //System.out.println("Received: " + msg);
        if (msg.startsWith("My IP")) {
            String ip = getIPFromMsg(msg);
            int port = getPortFromMsg(msg);
            listener.curAddressFromPeer(ip, port, this);
        } else if (msg.startsWith("Peer IP")) {
            String ip = getIPFromMsg(msg);
            int port = getPortFromMsg(msg);
            listener.peerAddressFromPeer(ip, port, socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        }
        throw new IOException("Bad data!");
    }

    private String getIPFromMsg(String msg) throws IOException {
        String[] ipInfo = msg.split(":");
        if (ipInfo.length == 3) {
            String ip = ipInfo[1];
            if (!isValidAddress(ip)) {
                throw new IOException("Bad data!");
            }
            return ip;
        }
        throw new IOException("Bad data!");
    }

    private int getPortFromMsg(String msg) throws IOException {
        String[] ipInfo = msg.split(":");
        if (ipInfo.length == 3) {
            String portStr = ipInfo[2];
            int port = -1;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IOException("Bad data!");
            }
            if (port <= 0 || port > 65535) {
                throw new IOException("Bad data!");
            }
            return port;
        }
        throw new IOException("Bad data!");
    }

    private boolean isValidAddress(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip).getHostAddress().equals(ip);
    }

    public void setThreadKey(String threadKey) {
        this.threadKey = threadKey;
    }
}
