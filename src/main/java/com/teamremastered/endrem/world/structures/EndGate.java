package com.teamremastered.endrem.world.structures;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.teamremastered.endrem.EndRemastered;
import com.teamremastered.endrem.config.ERConfig;
import com.teamremastered.endrem.utils.ERUtils;
import com.teamremastered.endrem.world.structures.config.ERStructures;
import com.teamremastered.endrem.world.structures.utils.CustomMonsterSpawn;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;


public class EndGate extends Structure<NoFeatureConfig> {
    private static final ResourceLocation START_POOL = new ResourceLocation(EndRemastered.MOD_ID, "end_gate/start_pool");
    private static final List<CustomMonsterSpawn> MONSTER_SPAWN_LIST =  ImmutableList.of(
            new CustomMonsterSpawn(EntityType.SKELETON, 30, 30, 35),
            new CustomMonsterSpawn(EntityType.ZOMBIE, 20, 25, 30),
            new CustomMonsterSpawn(EntityType.CAVE_SPIDER, 20, 25, 30),
            new CustomMonsterSpawn(EntityType.WITCH, 10, 10, 15)
    );

    public EndGate(Codec<NoFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public @Nonnull GenerationStage.Decoration step() {
        return GenerationStage.Decoration.STRONGHOLDS;
    }

    public static List<Biome.Category> getValidBiomeCategories() {
        List<Biome.Category> biomeCategories = new ArrayList<>();
        for (String biomeName : ERConfig.END_GATE_WHITELISTED_BIOME_CATEGORIES.getList()) {
            biomeCategories.add(Biome.Category.byName(biomeName));
        }
        return biomeCategories;
    }

    public static void setupStructureSpawns(final StructureSpawnListGatherEvent event) {
        if(event.getStructure() == ERStructures.END_GATE.get()) {
            for (CustomMonsterSpawn monsterSpawn : MONSTER_SPAWN_LIST) {
                event.addEntitySpawn(EntityClassification.MONSTER, monsterSpawn.getIndividualMobSpawnInfo());
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, BiomeProvider biomeSource, long seed, SharedSeedRandom chunkRandom, int chunkX, int chunkZ, Biome biome, ChunkPos chunkPos, NoFeatureConfig noFeatureConfig) {
        return ERUtils.getChunkDistanceFromSpawn(chunkPos) >= ERConfig.END_GATE_SPAWN_DISTANCE.getRaw();
    }

    @Override
    @ParametersAreNonnullByDefault
    public BlockPos getNearestGeneratedFeature(IWorldReader world, StructureManager manager, BlockPos blockPos, int radius, boolean skipExistingChunks, long seed, StructureSeparationSettings separationSettings) {
        /*
         * Not Even Remotely Close From Knowing Exactly How This Works :((
         *
         * This code was basically copy-pasted from Minecraft's and adapted so that locating
         * the structure would not return a position close to the portal
         * */

        int i = separationSettings.spacing();
        int j = blockPos.getX() >> 4;
        int k = blockPos.getZ() >> 4;
        int l = 0;

        for (SharedSeedRandom worldgenrandom = new SharedSeedRandom(); l <= radius; ++l) {
            for (int i1 = -l; i1 <= l; ++i1) {
                boolean flag = i1 == -l || i1 == l;

                for (int j1 = -l; j1 <= l; ++j1) {
                    boolean flag1 = j1 == -l || j1 == l;
                    if (flag || flag1) {
                        int k1 = j + i * i1;
                        int l1 = k + i * j1;
                        ChunkPos chunkpos = this.getPotentialFeatureChunk(separationSettings, seed, worldgenrandom, k1, l1);
                        IChunk chunkAccess = world.getChunk(chunkpos.x, chunkpos.z, ChunkStatus.STRUCTURE_STARTS);
                        StructureStart<?> structurestart = manager.getStartForFeature(SectionPos.of(chunkAccess.getPos(), 0), this, chunkAccess);
                        if (structurestart != null && structurestart.isValid()) {
                            if (skipExistingChunks && structurestart.canBeReferenced()) {
                                structurestart.addReference();
                                return new BlockPos(structurestart.getPieces().get(((Start) structurestart).getLocatedRoom()).getBoundingBox().x0,
                                        structurestart.getPieces().get(((Start) structurestart).getLocatedRoom()).getBoundingBox().y0,
                                        structurestart.getPieces().get(((Start) structurestart).getLocatedRoom()).getBoundingBox().z0);
                            }

                            if (!skipExistingChunks) {
                                return new BlockPos(structurestart.getPieces().get(((Start) structurestart).getLocatedRoom()).getBoundingBox().x0,
                                        structurestart.getPieces().get(((Start) structurestart).getLocatedRoom()).getBoundingBox().y0,
                                        structurestart.getPieces().get(((Start) structurestart).getLocatedRoom()).getBoundingBox().z0);
                            }
                        }

                        if (l == 0) {
                            break;
                        }
                    }
                }

                if (l == 0) {
                    break;
                }
            }
        }

        return null;
    }

    @Override
    public @Nonnull IStartFactory<NoFeatureConfig> getStartFactory() {
        return Start::new;
    }

    public static class Start extends StructureStart<NoFeatureConfig> {
        public Start(Structure<NoFeatureConfig> structureIn, int chunkX, int chunkZ, MutableBoundingBox mutableBoundingBox, int referenceIn, long seedIn) {
            super(structureIn, chunkX, chunkZ, mutableBoundingBox, referenceIn, seedIn);
        }

        @Override
        @ParametersAreNonnullByDefault
        public void generatePieces(DynamicRegistries registryAccess, ChunkGenerator chunkGenerator, TemplateManager manager, int chunkX, int chunkZ, Biome biomeIn, NoFeatureConfig config) {
            BlockPos genPosition = new BlockPos(chunkX << 4, ERConfig.END_GATE_HEIGHT.getRaw(), chunkZ << 4);

            JigsawManager.addPieces(
                    registryAccess,
                    new VillageConfig(() -> registryAccess.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY).get(START_POOL),
                            ERConfig.END_GATE_SIZE.getRaw()),
                    AbstractVillagePiece::new,
                    chunkGenerator,
                    manager,
                    genPosition,
                    this.pieces,
                    this.random,
                    false,
                    false);
            this.calculateBoundingBox();
        }

        public int getLocatedRoom() {
            return Math.min(16, this.pieces.size()) - 1;
        }
    }
}
