package com.swe.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Communicator class module for TCP.
 *
 */
public final class TCPCommunicator implements ProtocolBase {
    /**
     * The server socket used to receive client connections.
     **/
    private ServerSocket receiveSocket;
    /**
     * The list of all connected clients and their sockets.
     **/
    private final HashMap<ClientNode, Socket> clientSockets;

    /**
     * The port where the server is instantiated.
     **/
    private final Integer deviceServerPort;
    /**
     * The maximum time the socket must wait to connect to the destination.
     **/
    private final Integer clientConnectTimeout = 5000;
    // Integer clientSendTimeout = 2000;

    /**
     * The size of the buffer to read data.
     * Should be exactly the size of one chunk.
     */
    private final Integer byteBufferSize = 5000 * 1024;

    // maintain list of clients and add timeouts
    /**
     * Constructor function for TCP Communicator class.
     *
     * @param serverPort which port to start the TCP.
     */
    public TCPCommunicator(final int serverPort) {
        System.out.println("TCP communicator initialized...");
        clientSockets = new HashMap<>();
        deviceServerPort = serverPort;
        setServerPort();
    }

    private void setServerPort() {
        try {
            receiveSocket = new ServerSocket(deviceServerPort);
            receiveSocket.setSoTimeout(0);
            System.out.println(
                    "Creating new server port at " + deviceServerPort + " ...");
        } catch (IOException ex) {
            System.out.println("Error while opening a socket at this port");
        }
    }

    @Override
    public Socket openSocket() {
        System.out.println("Opening new socket...");
        return new Socket();
    }

    @Override
    public void closeSocket(final ClientNode client) {
        try {
            final Socket clientSocket = clientSockets.get(client);
            clientSockets.remove(client);
            clientSocket.close();
            System.out.println("Closing socket for client " + client + " ...");
        } catch (IOException ex) {
            System.out.println("Error occured while closing Socket...");
        }
    }

    @Override
    public void sendData(final byte[] data, final ClientNode dest) {
        final String destIp = dest.hostName();
        final Integer destPort = dest.port();
        try {
            if (clientSockets.containsKey(dest)) {
                System.out.println("Connection exists already...");
                final Socket destSocket = clientSockets.get(dest);
                final OutputStream output = destSocket.getOutputStream();
                final DataOutputStream dataOut = new DataOutputStream(output);
                dataOut.write(data);
                return;
            }
            final Socket destSocket = openSocket();
            System.out.println("Waiting for "
                    + clientConnectTimeout + " s to connect to the client...");
            System.out.println(destIp + destPort);
            destSocket.connect(new InetSocketAddress(destIp, destPort),
                    clientConnectTimeout);
            System.out.println("New connection created successfully...");
            final OutputStream output = destSocket.getOutputStream();
            final DataOutputStream dataOut = new DataOutputStream(output);
            dataOut.write(data);
            printIpAddr(destIp, destPort);
//            destSocket.close();
            clientSockets.put(new ClientNode(destIp, destPort), destSocket);
        } catch (IOException ex) {
            System.out.println("Error occured while sending data.... ");
        }
    }

    @Override
    public byte[] receiveData() {
        try {
            // TODO Run the receiveData function in an infinite loop
            final byte[] buffer = new byte[byteBufferSize];
            final Socket socket = receiveSocket.accept();
            // Add the received ports to the dictionary also.
            final String socketIp = socket.getInetAddress().getHostAddress();
            final int socketPort = socket.getPort();
            clientSockets.put(new ClientNode(socketIp, socketPort), socket);
            final InputStream input = socket.getInputStream();
            final DataInputStream dataIn = new DataInputStream(input);
            final int bytesRead = dataIn.read(buffer, 0, byteBufferSize);
            final byte[] data = Arrays.copyOfRange(buffer, 0, bytesRead);
            return data;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Function to handle socket on termination.
     */
    @Override
    public void close() {
        try {
            System.out.println("Closing TCP communicator");
            receiveSocket.close();
            for (Socket socket : clientSockets.values()) {
                socket.close();
            }
        } catch (IOException ex) {
        }
    }

    private void printIpAddr(final String ipAddr, final Integer port) {
        System.out.println("Client: " + ipAddr + ":" + port);
    }
}
