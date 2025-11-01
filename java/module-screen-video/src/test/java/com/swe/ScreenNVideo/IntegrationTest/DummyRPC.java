/**
 * Contributed by Priyanshu Pandey.
 */

package com.swe.ScreenNVideo.IntegrationTest;

import com.socketry.SocketryServer;
import com.swe.RPC.AbstractRPC;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * A simple mock implementation of {@link AbstractRPC} for integration testing,
 * using {@link SocketryServer} to simulate RPC communication locally.
 *
 */
public class DummyRPC implements AbstractRPC {

    /** Map storing registered remote procedure names and their corresponding handler functions. */
    private final HashMap<String, Function<byte[], byte[]>> procedures;

    /** The underlying {@link SocketryServer} instance used to handler RPC communication. */
    private SocketryServer server;

    /** Default port number used by the {@link SocketryServer} for testing. */
    private static final int TEST_SERVER_PORT = 60000;

    public DummyRPC() {
        procedures = new HashMap<>();
    }

    @Override
    public void subscribe(final String name, final Function<byte[], byte[]> func) {
        procedures.put(name, func);
    }

    @Override
    public Thread connect() throws IOException, ExecutionException, InterruptedException {
        System.out.println(procedures);
        server = new SocketryServer(TEST_SERVER_PORT, procedures);
        final Thread handler = new Thread(server::listenLoop);
        handler.start();
        return handler;
    }

    @Override
    public CompletableFuture<byte[]> call(final String name, final byte[] args) {
        if (server == null) {
            System.err.println("Server is null");
            return CompletableFuture.supplyAsync(() -> {
                return new byte[0];
            });
        }
        final byte funcId = server.getRemoteProcedureId(name);
        try {
            return server.makeRemoteCall(funcId, args, 0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
