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

package orchestration;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.esotericsoftware.minlog.Log;
import io.github.palexdev.hotswapfx.orchestration.HotSwapParticipant;
import io.github.palexdev.hotswapfx.orchestration.HotSwapServer;
import io.github.palexdev.hotswapfx.orchestration.message.ReloadRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrchestrationTest {

    @Test
    void testReloadRequest() throws Exception {
        Log.set(Log.LEVEL_NONE);
        int port = 8765;
        ReloadRequest.Changes changes = new ReloadRequest.Changes(Map.of(
            Path.of("com/example/Foo.class"), ReloadRequest.ChangeType.ADD,
            Path.of("com/example/Bar.class"), ReloadRequest.ChangeType.REMOVE,
            Path.of("com/example/Bazz.class"), ReloadRequest.ChangeType.UPDATE
        ));

        CountDownLatch latch = new CountDownLatch(1);
        try (
            HotSwapServer server = new HotSwapServer(port).start();
            HotSwapParticipant participant = new HotSwapParticipant(port)
        ) {
            server.registerHook(ReloadRequest.class, r -> {
                assertMaps(changes, r.changes());
                latch.countDown();
            });
            participant.send(new ReloadRequest(changes));
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        }
    }

    private <K, V> void assertMaps(Map<K, V> expected, Map<K, V> got) {
        assertEquals(expected.size(), got.size());
        for (Map.Entry<K, V> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), got.get(entry.getKey()));
        }
    }
}
