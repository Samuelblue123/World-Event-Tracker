//package org.wet.world_event_tracker.features;
//
//
//import com.mojang.brigadier.Command;
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
//import net.minecraft.text.Text;
//import org.wet.world_event_tracker.World_event_tracker;
//import org.wet.world_event_tracker.components.Feature;
//import org.wet.world_event_tracker.components.Managers;
//import org.wet.world_event_tracker.net.World_event_trackerClient;
//import org.wet.world_event_tracker.net.event.NetEvents;
//import org.wet.world_event_tracker.net.type.Api;
//import org.wet.world_event_tracker.utils.McUtils;
//import org.wet.world_event_tracker.utils.NetUtils;
//import org.wet.world_event_tracker.utils.type.Prepend;

//public class AutoUpdateFeature extends Feature {
//    private boolean completed = false;
//    private boolean needUpdate = false;
//    private String modDownloadURL;
//
//    @Override
//    public void init() {
//        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
//            dispatcher.register(World_event_tracker.BASE_COMMAND.then(ClientCommandManager.literal("update")
//                    .executes((context) -> {
//                        World_event_tracker.LOGGER.info("guild update");
//                        // do some md5 verification stuff on downloaded file
//                        return Command.SINGLE_SUCCESS;
//                    })));
//        });
//        NetEvents.LOADED.register(this::onApiLoaded);
//    }
//
//    private void onApiLoaded(Api loaded) {
//        if (!completed && loaded.getClass().equals(World_event_trackerClient.class)) {
//            Managers.Net.user.get("mod/update").whenCompleteAsync((res, err) -> {
//                try {
//                    NetUtils.applyDefaultCallback(res, err, (resOK) -> {
//                        World_event_tracker.LOGGER.info("auto update result: {}", resOK);
//                        String latestVersion = resOK.getAsJsonObject().get("versionNumber").getAsString();
//                        if (!World_event_tracker.MOD_VERSION.equals(latestVersion)) {
//                            World_event_tracker.LOGGER.info("outdated version: {}", World_event_tracker.MOD_VERSION);
//                            McUtils.sendLocalMessage(Text.literal("§a[World Event Tracker] You are running build v" + World_event_tracker.MOD_VERSION + ", but the latest build is v" + latestVersion + "." +
//                                    " " +
//                                    "Please consider updating through modrinth."), Prepend.EMPTY.get(), false);
//                        }
//                    }, NetUtils.defaultFailed("mod update check", false));
//                } catch (Exception e) {
//                    NetUtils.defaultException("auto update", e);
//                }
//            });
//            completed = true;
//        }
//    }
//}