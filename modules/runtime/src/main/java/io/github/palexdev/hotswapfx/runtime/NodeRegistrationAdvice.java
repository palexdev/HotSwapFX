package io.github.palexdev.hotswapfx.runtime;

import io.github.palexdev.hotswapfx.core.HotSwapService;
import javafx.scene.Node;
import net.bytebuddy.asm.Advice;

class NodeRegistrationAdvice {
    @Advice.OnMethodExit
    public static void onExit(@Advice.This Node node) {
        HotSwapService.instance().register(node);
    }
}