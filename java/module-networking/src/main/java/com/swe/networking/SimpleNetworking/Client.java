package com.swe.networking.SimpleNetworking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;

/**
 * The main module of the client device.
 */
public class Client implements IUser {

    /**
     * The variable to store the device IP address.
     */
    private String deviceIp;
    /**
     * The variable to store the device port number.
     */
    private int devicePort;
    /**
     * The variable used by the server to connect to other devices.
     */
    private Socket sendSocket = new Socket();
    /**
     * The variable used by server to accept connections from clients.
     */
    private ServerSocket receiveSocket;
    /**
     * The singleton class object for packet parser.
     */
    private PacketParser parser;
    /**
     * The singleton class object for simplenetworking.
     */
    private SimpleNetworking simpleNetworking;
    /**
     * The variable to store the module type.
     */
    private ModuleType moduleType = ModuleType.NETWORKING;
    /**
     * The variable isused to store connection timeout.
     */
    private final int connectionTimeout = 5000;

    /**
     * The constructor function for the client class.
     *
     * @param deviceAddr the device IP address details
     */
    public Client(final ClientNode deviceAddr) {
        deviceIp = deviceAddr.hostName();
        devicePort = deviceAddr.port();
        parser = PacketParser.getPacketParser();
        simpleNetworking = SimpleNetworking.getSimpleNetwork();
        try {
            receiveSocket = new ServerSocket(devicePort);
            receiveSocket.setSoTimeout(0);
        } catch (IOException e) {
            System.err.println("Client1 Error: " + e.getMessage());
        }
    }

    /**
     * Function to send the data to a list of destination.
     *
     * @param data     the data to be sent
     * @param destIp   the list fo destination to send the data
     * @param serverIp the Ip address of the main server
     * @param module   the module to send th data to
     */
    @Override
    public void send(final byte[] data, final ClientNode[] destIp,
            final ClientNode serverIp, final ModuleType module) {
        for (ClientNode client : destIp) {
            final String ip = client.hostName();
            final int port = client.port();
            try {
                sendSocket = new Socket();
                System.out.println("COnnecting..");
                sendSocket.connect(new InetSocketAddress(serverIp.hostName(),
                        serverIp.port()), connectionTimeout);
                final OutputStream output = sendSocket.getOutputStream();
                final DataOutputStream dataOut = new DataOutputStream(output);
                final InetAddress addr = InetAddress.getByName(ip);
                System.out.println("Connected to " + ip + ":" + port + " ...");
                dataOut.write(parser.createPkt(0, 0, module.ordinal(),
                        0, 0, addr, port, data));
                System.out.println("Sent data succesfully...");
                sendSocket.close();
            } catch (IOException e) {
                System.err.println("Client2 Error: " + e.getMessage());
            }
        }
    }

    /**
     * Function to receive data from the given socket.
     */
    @Override
    public void receive() throws IOException {
        try {
            final Socket socket = receiveSocket.accept();
            final InputStream input = socket.getInputStream();
            final DataInputStream dataIn = new DataInputStream(input);
            final byte[] packet = dataIn.readAllBytes();
            parsePacket(packet);
        } catch (SocketTimeoutException e) {
            System.err.println("Client3 Error: " + e.getMessage());
        }
    }

    /**
     * Function to parse the received packet and perform required response.
     *
     * @param packet the packet to parse
     */
    public void parsePacket(final byte[] packet) {
        final int module = parser.getModule(packet);
        final ModuleType type = moduleType.getType(module);
//        final String data = new String(parser.getPayload(packet),
//                StandardCharsets.UTF_8);
//        System.out.println("Client Data received : " + data);
        simpleNetworking.callSubscriber(parser.getPayload(packet), type);
    }

    /**
     * Function to be called on closing.
     */
    @Override
    public void closeUser() {
        try {
            receiveSocket.close();
        } catch (IOException e) {
        }
    }
}
