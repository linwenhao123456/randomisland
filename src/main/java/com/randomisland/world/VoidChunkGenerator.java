package com.randomisland.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 纯净 void 生成器：所有列均为空气，仅在出生点由 IslandBuilder 放置空岛。
 *
 * <p>按 Minecraft 1.20.1（yarn 1.20.1+build.10）的 ChunkGenerator API 实现：
 * 噪声配置为 {@link NoiseConfig}，列样本为 {@link VerticalBlockSample}。
 */
public class VoidChunkGenerator extends ChunkGenerator {
    public static final Codec<VoidChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
    ).apply(instance, VoidChunkGenerator::new));

    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;

    /**
     * 全空气列样本缓存。内容恒定不变，无需每次调用都新建并填充 384 个元素。
     * 该样本仅被调用方读取（不调用 {@code setState}），因此可安全复用。
     */
    private static final VerticalBlockSample AIR_SAMPLE = createAirSample();

    private static VerticalBlockSample createAirSample() {
        BlockState[] states = new BlockState[HEIGHT];
        Arrays.fill(states, Blocks.AIR.getDefaultState());
        return new VerticalBlockSample(HEIGHT, states);
    }

    public VoidChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinimumY() {
        return MIN_Y;
    }

    @Override
    public int getWorldHeight() {
        return HEIGHT;
    }

    @Override
    public int getSpawnHeight(HeightLimitView world) {
        return IslandBuilder.ISLAND_TOP_Y + 1;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig randomState) {
        // 无地形，高度一律取世界最低层
        return MIN_Y;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig randomState) {
        // 无地形，列样本恒为全空气列
        return AIR_SAMPLE;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig randomState,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        // 不生成任何地形，保持空气
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void carve(ChunkRegion region, long seed, NoiseConfig randomState, BiomeAccess biomeSource,
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // 空岛世界无需雕刻
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structureAccessor, NoiseConfig randomState, Chunk chunk) {
        // 空岛世界无地表生成
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // 空岛世界不生成自然实体
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig randomState, BlockPos pos) {
        // 无需调试信息
    }
}
