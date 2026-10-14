package com.randomisland;

import com.randomisland.config.ModConfig;
import com.randomisland.enchant.MiningEnchantment;
import com.randomisland.log.ModLog;
import com.randomisland.refresh.RefreshScheduler;
import com.randomisland.world.IslandBuilder;
import com.randomisland.world.VoidChunkGenerator;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class RandomIslandMod implements ModInitializer {
    public static final String MOD_ID = "random_island";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Path logFile = ModLog.init();
        ModLog.info("随机空岛 mod 正在初始化...");
        if (logFile != null) {
            ModLog.info("运行日志文件: %s", logFile.toAbsolutePath());
        }

        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        ModLog.info("配置已注册: %s（读取自 config/random_island.json）", ModConfig.class.getSimpleName());

        // 1.20.1 中区块生成器类型是静态注册表里的 Codec（不是 WorldPreset 那种内置注册表）。
        // 世界预设在 1.20.1 无法用代码注册，改由数据包提供：
        //   data/random_island/worldgen/world_preset/skyblock.json
        Registry.register(Registries.CHUNK_GENERATOR, new Identifier(MOD_ID, "void"), VoidChunkGenerator.CODEC);

        // 注册「挖掘」附魔（只能附在镐上，瞬间破坏任意方块含基岩）
        MiningEnchantment.register();
        ModLog.info("已注册附魔: %s:mining（挖掘）", MOD_ID);
        ModLog.info("已注册区块生成器类型: %s:void", MOD_ID);
        ModLog.info("世界预设数据包: data/%s/worldgen/world_preset/skyblock.json（创建世界时选择「随机空岛（虚空）」）", MOD_ID);

        // 安全诊断：确认预设数据包资源是否随 mod 的 built-in datapack 被加载。
        // （注意：world_preset 注册表仅在创建世界瞬间存在，运行期读取会崩溃，故只检查资源存在性）
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return new Identifier(MOD_ID, "world_preset_diag");
                    }
                    @Override
                    public void reload(ResourceManager manager) {
                        boolean exists = manager.getResource(
                                new Identifier(MOD_ID, "worldgen/world_preset/skyblock.json")).isPresent();
                        ModLog.info("世界预设数据包资源 skyblock.json 是否随 datapack 加载: %s", exists);
                    }
                });

        ServerWorldEvents.LOAD.register(IslandBuilder::onWorldLoad);
        ServerTickEvents.END_SERVER_TICK.register(RefreshScheduler::onEndTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            RefreshScheduler.reset();
            ModLog.info("服务器已停止。");
        });
        RandomIslandCommand.register();

        ModLog.info("随机空岛 mod 初始化完成。");
    }
}
