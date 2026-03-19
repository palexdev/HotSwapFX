package io.github.palexdev.hotswapfx.orchestration;

import java.io.IOException;
import java.nio.file.Path;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import io.github.palexdev.hotswapfx.orchestration.message.Message;
import org.tinylog.Logger;

/// A fire and forget implementation of a TCP client using [Kryo](https://github.com/EsotericSoftware/kryo)
/// and [Kryonet](https://github.com/palexdev/kryonet)
public class HotSwapParticipant implements AutoCloseable {

    //================================================================================
    // Properties
    //================================================================================

    private final Client client;
    private final int port;

    //================================================================================
    // Constructors
    //================================================================================

    public HotSwapParticipant(int port) {
        this.port = port;
        client = new Client();
        // Kryo config
        Kryo kryo = client.getKryo();
        kryo.setRegistrationRequired(false);
        kryo.addDefaultSerializer(Path.class, new PathSerializer(kryo, Path.class));
    }

    //================================================================================
    // Methods
    //================================================================================

    private void connect() {
        try {
            if (!client.isConnected()) {
                client.start();
                client.connect("localhost", port);
            }
        } catch (IOException ex) {
            Logger.error(ex, "Client failed to connect");
        }
    }

    public void send(Message message) {
        connect();
        client.sendTCP(message);
    }

    @Override
    public void close() {
        Logger.debug("Closing client {}...", client.getID());
        client.close();
    }
}
