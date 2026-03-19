package agent;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.palexdev.hotswapfx.orchestration.HotSwapParticipant;
import io.github.palexdev.hotswapfx.orchestration.message.ReloadRequest;
import io.github.palexdev.hotswapfx.runtime.HotSwapAgent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class AgentTest {

    @Test
    void testAgent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HotSwapAgent agent = mockAgent(latch);
        agent.run();
        ReloadRequest.Changes changes = new ReloadRequest.Changes(Map.of(
            Path.of("com/example/Foo.class"), ReloadRequest.ChangeType.ADD,
            Path.of("com/example/Bar.class"), ReloadRequest.ChangeType.REMOVE,
            Path.of("com/example/Bazz.class"), ReloadRequest.ChangeType.UPDATE
        ));
        try (HotSwapParticipant participant = new HotSwapParticipant(8765)) {
            participant.send(new ReloadRequest(changes));
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        }
    }

    private static HotSwapAgent mockAgent(CountDownLatch latch) {
        Instrumentation inst = mock(Instrumentation.class);
        return new HotSwapAgent("port=8765", inst) {
            @Override
            public void run() {
                try {
                    server.registerHook(ReloadRequest.class, this::handleReload);
                    server.start();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            protected void handleReload(ReloadRequest request) {
                latch.countDown();
            }
        };
    }
}
