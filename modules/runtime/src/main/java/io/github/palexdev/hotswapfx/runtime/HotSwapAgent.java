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

package io.github.palexdev.hotswapfx.runtime;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import io.github.palexdev.hotswapfx.core.HotSwapException;
import io.github.palexdev.hotswapfx.core.HotSwapService;
import io.github.palexdev.hotswapfx.core.ServiceHook;
import io.github.palexdev.hotswapfx.core.ServiceHook.HookType;
import io.github.palexdev.hotswapfx.core.annotations.HotSwappable;
import io.github.palexdev.hotswapfx.orchestration.HotSwapServer;
import io.github.palexdev.hotswapfx.orchestration.message.ProcessPendingReloads;
import io.github.palexdev.hotswapfx.orchestration.message.ReloadRequest;
import io.github.palexdev.hotswapfx.orchestration.message.ReloadRequest.Changes;
import io.github.palexdev.hotswapfx.orchestration.message.ToggleAutoReload;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import org.tinylog.Logger;

import static java.util.Optional.ofNullable;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;

/// Java agent that enables hot reloading and swapping functions through bytecode manipulation.
///
/// Thanks to [ByteBuddy](https://bytebuddy.net/), we can transform the bytecode of classes annotated with [HotSwappable]
/// to automatically register on the [HotSwapService]'s registry when a constructor is used. In other words, every new
/// instance is automatically registered for hot swapping.
///
/// The agent starts a [HotSwapServer] that listens for reload requests. This process is about redefining all changed
/// classes through the [Instrumentation#redefineClasses(ClassDefinition...)] and asking the service to swap the changed nodes.
public class HotSwapAgent {

    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    public static void premain(String args, Instrumentation inst) {
        HotSwapAgent agent = new HotSwapAgent(args, inst);
        agent.install();
        agent.run();
    }

    private final Map<String, String> args;
    private final Instrumentation inst;

    protected HotSwapServer server;
    private boolean autoReload = true;
    private boolean processPending = false;
    private int port = -1;

    private final Changes pendingChanges = new Changes();

    public HotSwapAgent(String args, Instrumentation inst) {
        this.args = parseArgs(args);
        this.inst = inst;

        server = new HotSwapServer(port());
        server.registerHook(ProcessPendingReloads.class, this::processPending);
        server.registerHook(ToggleAutoReload.class, this::toggleAutoReload);
    }

    public void install() {
        new AgentBuilder.Default()
            .type(isAnnotatedWith(HotSwappable.class))
            .transform((builder, _, _, _, _) ->
                builder.visit(Advice.to(NodeRegistrationAdvice.class).on(isConstructor()))
            )
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .installOn(inst);
    }

    public void run() {
        if (useLegacyWatchService()) {
            LegacyWatchService.instance().onReloadRequest(this::handleReload);
            LegacyWatchService.instance().start();
        } else {
            server.registerHook(ReloadRequest.class, this::handleReload);
        }

        if (useDevTools()) {
            try {
                Class<?> klass = Class.forName("io.github.palexdev.hotswapfx.devtools.DevTools");
                Method main = klass.getDeclaredMethod("main", String[].class);
                main.setAccessible(true);
                String[] args = new String[]{"--port=" + port(), "--autoclose"};
                main.invoke(null, (Object) args);
            } catch (ReflectiveOperationException ex) {
                Logger.error(ex, "Failed to start devtools");
            }
        }

        server.startAsync();
    }

    protected void processPending(ProcessPendingReloads request) {
        try {
            processPending = true;
            if (!pendingChanges.isEmpty()) {
                handleReload(new ReloadRequest(pendingChanges));
                pendingChanges.clear();
            } else {
                Logger.info("No pending changes, skipping manual reload...");
            }
        } finally {
            processPending = false;
        }
    }

    protected void handleReload(ReloadRequest request) {
        if (request.changes() == null || request.changes().isEmpty()) return;
        if (!autoReload && !processPending) {
            Logger.info("Auto reload is disabled, storing changes as pending...");
            request.changes().forEach((k, v) ->
                pendingChanges.merge(k, v, (_, c2) -> c2));
            return;
        }

        // Notify early hooks
        request.changes().keySet().forEach(this::notifyEarlyHooks);

        // Phase 1 - Redefine
        Logger.info("Reloading on: {}", request.changes());
        ClassDefinition[] toRedefine = request.changes().keySet().stream()
            .filter(p -> {
                String fileName = p.getFileName().toString();
                return fileName.endsWith(".class") && !"module-info.class".equals(fileName);
            })
            .map(p -> {
                try {
                    byte[] classBytes = Files.readAllBytes(p);
                    TypeDescription typeDesc = TypePool.Default.of(
                        new ClassFileLocator.Simple(Collections.singletonMap("ignored.Name", classBytes))
                    ).describe("ignored.Name").resolve();
                    String className = typeDesc.getActualName();
                    if (className.isEmpty()) {
                        throw new HotSwapException("Could not resolve class name for path: " + p);
                    }
                    return new ClassDefinition(Class.forName(className), classBytes);
                } catch (Exception ex) {
                    Logger.error(ex);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toArray(ClassDefinition[]::new);
        try {
            inst.redefineClasses(toRedefine);
        } catch (Exception ex) {
            Logger.error("Could not redefine classes: {}", ex);
        }

        Set<Class<?>> reloaded = new LinkedHashSet<>();
        for (ClassDefinition cd : toRedefine) {
            reloaded.add(cd.getDefinitionClass());
            reloaded.addAll(HotSwapService.instance().dependsOn(cd.getDefinitionClass()));
        }

        // Phase 2 - Swap
        Logger.info("Swapping classes: {}", Arrays.toString(reloaded.toArray()));
        reloaded.forEach(HotSwapService.instance()::swapNodes);
    }

    protected void toggleAutoReload(ToggleAutoReload request) {
        autoReload = request.state();
        Logger.info("Auto reload: {}", autoReload ? "Enabled" : "Disabled");
    }

    @SuppressWarnings("unchecked")
    private void notifyEarlyHooks(Path path) {
        ofNullable(HotSwapService.instance().hooks().get(HookType.ON_FILE))
            .ifPresent(hooks -> hooks.forEach(h -> ((ServiceHook<Path>) h).onEvent(path)));
    }

    public int port() {
        if (port < 0) {
            port = ofNullable(args.get("port"))
                .map(Integer::valueOf)
                .orElseGet(this::getAvailablePort);
        }
        return port;
    }

    public boolean useDevTools() {
        return ofNullable(args.get("useDevTools"))
            .map(Boolean::valueOf)
            .orElse(false);
    }

    // when not using the Gradle plugin, the agent _may_ not receive the port argument, in that case get one so that
    // there can be communication with the DevTools
    private int getAvailablePort() {
        try (ServerSocket server = new ServerSocket(0)) {
            return server.getLocalPort();
        } catch (Exception ex) {
            return 8765;
        }
    }

    private boolean useLegacyWatchService() {
        return ofNullable(args.get("legacyWatchService"))
            .map(Boolean::valueOf)
            .orElse(false);
    }

    private Map<String, String> parseArgs(String allArgs) {
        if (allArgs == null || allArgs.isBlank()) return Collections.emptyMap();
        Map<String, String> argsMap = new HashMap<>();
        String[] args = allArgs.split(",");
        for (String arg : args) {
            String[] keyVal = arg.split("=");
            if (keyVal.length != 2) {
                throw new IllegalArgumentException("Invalid argument: " + arg);
            }
            argsMap.put(keyVal[0], keyVal[1]);
        }
        return argsMap;
    }
}
