package com.lootbeams.listeners;

import com.lootbeams.events.ResourceEvents;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;
import net.minecraft.util.Unit;

public class ResourceReloadListener implements PreparableReloadListener {
    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor prepareExecutor, PreparationBarrier barrier, Executor applyExecutor) {
        ResourceManager manager = sharedState.resourceManager();
        return barrier.wait(Unit.INSTANCE)
            .thenRunAsync(() -> ResourceEvents.RESOURCE_RELOAD.invoker().onResourceReload(barrier, manager, prepareExecutor, applyExecutor), applyExecutor);
    }

    @Override
    public String getName() {
        return "lootbeams:reload_resources";
    }
}
