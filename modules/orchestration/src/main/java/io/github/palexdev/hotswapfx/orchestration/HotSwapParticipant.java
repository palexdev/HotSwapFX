/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of HotSwapFX (https://github.com/palexdev/HotSwapFX)
 *
 * HotSwapFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * HotSwapFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HotSwapFX. If not, see <http://www.gnu.org/licenses/>.
 */

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
