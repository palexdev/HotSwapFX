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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import io.github.palexdev.hotswapfx.orchestration.message.Message;
import org.tinylog.Logger;

import static java.util.Optional.ofNullable;

/// Implementation of a TCP server that listens on a given port either synchronously or asynchronously. It uses a
/// types hooking mechanism to let the user react to events coming from the clients ([#registerHook(Class, Consumer)]).
///
/// This also uses [Kryo](https://github.com/EsotericSoftware/kryo) and [Kryonet](https://github.com/palexdev/kryonet)
@SuppressWarnings({"unchecked", "rawtypes"})
public class HotSwapServer implements AutoCloseable {

    //================================================================================
    // Properties
    //================================================================================

    private final Server server;
    private final int port;
    private final Map<Class<?>, Consumer> hooks = new HashMap<>();

    //================================================================================
    // Constructors
    //================================================================================

    public HotSwapServer(int port) {
        Log.set(Log.LEVEL_NONE);
        this.port = port;
        server = new Server();
        // Kryo config
        Kryo kryo = server.getKryo();
        kryo.setRegistrationRequired(false);
        kryo.addDefaultSerializer(Path.class, new PathSerializer(kryo, Path.class));
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Binds the server to the port and starts listening for events.
    public HotSwapServer start() throws IOException {
        Logger.info("Starting server on port {}", port);
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Logger.info("Client connected ID:{}|IP:{}", connection.getID(), connection.getRemoteAddressTCP());
            }

            @Override
            public void disconnected(Connection connection) {
                Logger.info("Client disconnected ID:{}", connection.getID());
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Message m) {
                    Logger.debug("Server received message: {}", m.getClass());
                    ofNullable(hooks.get(m.getClass()))
                        .ifPresent(hook -> hook.accept(object));
                }
            }
        });
        server.bind(port);
        server.start();
        return this;
    }

    /// Calls [#start()] is a virtual thread.
    @SuppressWarnings({"resource"})
    public Future<HotSwapServer> startAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                start();
            } catch (IOException ex) {
                Logger.error(ex, "Agent server failed to start");
            }
            return this;
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    @Override
    public void close() {
        Logger.debug("Closing server...");
        server.close();
    }

    /// Registers a hook for the given event type that is triggered when the server receives an event of that type.
    public <I extends Message> void registerHook(Class<I> klass, Consumer<I> hook) {
        hooks.put(klass, hook);
    }
}
