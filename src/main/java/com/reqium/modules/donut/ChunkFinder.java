package com.reqium.modules.donut;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.VineBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChunkFinder extends Module {
    private static final long WORLD_SEED = 6608149111735331168L;
    private static final float RENDER_Y = 63.0f;
    private static final int SCAN_INTERVAL_TICKS = 1;
    private static final int ORIGIN_SEARCH_RADIUS = 8;
    private static final int MAX_PATH_LENGTH = 16;
    private static final int MIN_ORIGIN_WEIGHT = 6;

    private final SliderSetting sensitivity = new SliderSetting("Sensitivity", 3, 1, 20, 1);
    private final SliderSetting simDistance = new SliderSetting("Sim Distance", 16, 2, 16, 1);
    private final SliderSetting alpha = new SliderSetting("Alpha", 120, 1, 255, 1);

    private final Setting<Boolean> detectAmethyst = new Setting<>("Detect Amethyst", true);
    private final Setting<Boolean> detectVines = new Setting<>("Detect Vines", true);
    private final Setting<Boolean> detectCaveVines = new Setting<>("Detect Cave Vines", true);
    private final Setting<Boolean> detectKelp = new Setting<>("Detect Kelp", true);
    private final Setting<Boolean> detectRotatedDeepslate = new Setting<>("Detect Rotated Deepslate", true);

    private final Set<ChunkPos> flaggedArea = ConcurrentHashMap.newKeySet();
    private final Map<ChunkPos, Float> scoreCache = new ConcurrentHashMap<>();

    private final ExecutorService engine = Executors.newFixedThreadPool(1);
    private volatile boolean isRunning = false;
    private int tickCounter = 0;

    public ChunkFinder() {
        super("ChunkFinder", "Finds suspicious chunks using plant and cluster growth", "Donut");
        addSetting(sensitivity);
        addSetting(simDistance);
        addSetting(alpha);
        addSetting(detectAmethyst);
        addSetting(detectVines);
        addSetting(detectCaveVines);
        addSetting(detectKelp);
        addSetting(detectRotatedDeepslate);
    }

    @Override
    public void onEnable() {
        clear();
    }

    @Override
    public void onDisable() {
        clear();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            return;
        }

        tickCounter++;
        if (tickCounter < SCAN_INTERVAL_TICKS || isRunning) {
            return;
        }
        tickCounter = 0;

        isRunning = true;
        engine.submit(() -> {
            try {
                deepCoreScan();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isRunning = false;
            }
        });
    }

    private void clear() {
        flaggedArea.clear();
        scoreCache.clear();
    }

    private void deepCoreScan() {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (world == null || mc.player == null) {
            return;
        }

        ChunkPos playerChunk = mc.player.getChunkPos();
        int radius = (int) Math.round(simDistance.getValue());
        int sens = (int) Math.round(sensitivity.getValue());
        int enabledIndicators = getEnabledIndicatorCount();
        boolean requireAmethyst = detectAmethyst.getValue();
        boolean requireGrowth = detectVines.getValue() || detectKelp.getValue() || detectCaveVines.getValue();
        boolean useRotated = detectRotatedDeepslate.getValue();

        Map<ChunkPos, Float> frameScores = new HashMap<>();
        Map<ChunkPos, Integer> sourceHits = new HashMap<>();

        float longTimeThreshold = getLongTimeThreshold(sens, enabledIndicators);
        int minAmethystHits = getRequiredAmethystHits(sens);
        int minGrowthHits = getRequiredGrowthHits(sens);
        int minRotatedHits = getRequiredRotatedHits(sens);
        int minTypes = getRequiredTypes(enabledIndicators);
        int minIndicators = getRequiredIndicatorCount(enabledIndicators);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
                if (!world.isChunkLoaded(cp.x, cp.z)) {
                    continue;
                }

                ChunkScanResult result = runChunkSimulation(cp, world, sens);
                if (result.score <= 0.0f) {
                    continue;
                }
                if (result.indicatorCount < minIndicators) {
                    continue;
                }
                if (Integer.bitCount(result.typeMask) < minTypes) {
                    continue;
                }

                boolean rotatedOk = useRotated && result.rotatedHits >= minRotatedHits;
                if (!rotatedOk) {
                    if (requireAmethyst && result.amethystHits < minAmethystHits) {
                        continue;
                    }
                    if (requireGrowth && result.growthHits < minGrowthHits) {
                        continue;
                    }
                }

                frameScores.put(cp, result.score);

                if (result.score >= longTimeThreshold) {
                    mergeSourceHits(sourceHits, result.sourceHitsLocal);
                }
                if (rotatedOk) {
                    addSourceHit(sourceHits, cp, result.rotatedHits);
                }
            }
        }

        if (sourceHits.isEmpty()) {
            mc.execute(() -> {
                flaggedArea.clear();
                scoreCache.clear();
            });
            return;
        }

        List<ChunkPos> sourceList = new ArrayList<>(sourceHits.keySet());
        sourceList.sort(Comparator.comparingInt(pos -> sourceHits.getOrDefault(pos, 0)).reversed());
        if (sourceList.size() > 2) {
            sourceList = sourceList.subList(0, 2);
        }

        if (sourceList.isEmpty() || sourceHits.getOrDefault(sourceList.get(0), 0) < MIN_ORIGIN_WEIGHT) {
            mc.execute(() -> {
                flaggedArea.clear();
                scoreCache.clear();
            });
            return;
        }

        Set<ChunkPos> newArea = new HashSet<>();
        if (sourceList.size() >= 2) {
            ChunkPos c1 = sourceList.get(0);
            ChunkPos c2 = sourceList.get(1);
            int dx = c2.x - c1.x;
            int dz = c2.z - c1.z;
            int dist = Math.max(Math.abs(dx), Math.abs(dz));
            if (dist > MAX_PATH_LENGTH) {
                dist = MAX_PATH_LENGTH;
            }
            for (int k = 0; k <= dist; k++) {
                int px = c1.x + dx * k / Math.max(1, dist);
                int pz = c1.z + dz * k / Math.max(1, dist);
                addPathCell(newArea, px, pz, 1);
            }
        } else {
            ChunkPos c1 = sourceList.get(0);
            addPathCell(newArea, c1.x, c1.z, 1);
        }

        mc.execute(() -> {
            flaggedArea.clear();
            flaggedArea.addAll(newArea);
            scoreCache.clear();
            scoreCache.putAll(frameScores);
        });
    }

    private ChunkScanResult runChunkSimulation(ChunkPos cp, World world, int sens) {
        WorldChunk chunk = world.getChunk(cp.x, cp.z);
        if (chunk == null || chunk.isEmpty()) {
            return ChunkScanResult.empty();
        }

        float score = 0.0f;
        int typeMask = 0;
        int indicatorCount = 0;
        int amethystCount = 0;
        int growthCount = 0;
        int rotatedCount = 0;
        Map<ChunkPos, Integer> sourceHitsLocal = new HashMap<>();
        Map<ChunkPos, Long> popSeedCache = new HashMap<>();
        ChunkRandom random = new ChunkRandom(Random.create());
        long popSeed = random.setPopulationSeed(WORLD_SEED, cp.getStartX(), cp.getStartZ());

        int startX = cp.getStartX();
        int startZ = cp.getStartZ();
        int bottomY = world.getBottomY();

        ChunkSection[] sections = chunk.getSectionArray();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()) {
                continue;
            }
            int sectionY = bottomY + i * 16;

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int ly = 0; ly < 16; ly++) {
                        BlockState state = section.getBlockState(lx, ly, lz);
                        if (state.isAir()) {
                            continue;
                        }

                        int wx = startX + lx;
                        int wy = sectionY + ly;
                        int wz = startZ + lz;
                        pos.set(wx, wy, wz);

                        BlockAnalysisResult analysis = analyzeBlock(pos, state, world, random, popSeed);
                        if (analysis.points > 0.0f) {
                            score += analysis.points;
                        }
                        if (analysis.typeMask != 0) {
                            typeMask |= analysis.typeMask;
                            indicatorCount += analysis.indicatorHits;
                            amethystCount += analysis.amethystHits;
                            growthCount += analysis.growthHits;
                            rotatedCount += analysis.rotatedHits;

                            if (analysis.sourceWeight > 0 && analysis.originType != null) {
                                ChunkPos source = findBestOriginChunk(
                                        pos,
                                        analysis.originType,
                                        analysis.originActual,
                                        popSeedCache
                                );
                                if (source != null) {
                                    addSourceHit(sourceHitsLocal, source, analysis.sourceWeight);
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ChunkScanResult(
                score * (sens / 3.0f),
                typeMask,
                indicatorCount,
                amethystCount,
                growthCount,
                rotatedCount,
                sourceHitsLocal
        );
    }

    private BlockAnalysisResult analyzeBlock(BlockPos pos, BlockState state, World world, ChunkRandom random, long popSeed) {
        Block block = state.getBlock();
        float points = 0.0f;
        int typeMask = 0;
        int indicatorHits = 0;
        int amethystHits = 0;
        int growthHits = 0;
        int rotatedHits = 0;
        int sourceWeight = 0;
        OriginType originType = null;
        int originActual = 0;

        if (detectAmethyst.getValue() && (block instanceof AmethystClusterBlock || block == Blocks.AMETHYST_CLUSTER)) {
            int expected = expectedStage(random, popSeed, pos);
            int actual = getAmethystStage(state);
            if (actual > expected + 1) {
                points += (actual - expected) * 12.0f;
                sourceWeight += 2;
                originType = OriginType.AMETHYST;
                originActual = actual;
                typeMask |= IndicatorType.AMETHYST.mask;
                indicatorHits++;
                amethystHits++;
            }
        }

        if (detectKelp.getValue() && (block == Blocks.KELP || block == Blocks.KELP_PLANT)) {
            float p = growthScore(state, random, popSeed, pos, 1.2f, 28);
            points += p;
            typeMask |= IndicatorType.KELP.mask;
            indicatorHits++;
            growthHits++;
            if (p > 0) {
                sourceWeight += 1;
                if (originType == null) {
                    originType = OriginType.KELP;
                    originActual = state.get(Properties.AGE_25);
                }
            }
        }

        if (detectCaveVines.getValue() && (block == Blocks.CAVE_VINES || block == Blocks.CAVE_VINES_PLANT)) {
            float p = growthScore(state, random, popSeed, pos, 1.4f, 4);
            points += p;
            typeMask |= IndicatorType.CAVE_VINES.mask;
            indicatorHits++;
            growthHits++;
            if (p > 0) {
                sourceWeight += 1;
                if (originType == null) {
                    originType = OriginType.CAVE_VINES;
                    originActual = state.get(Properties.AGE_25);
                }
            }
        }

        if (detectVines.getValue() && block instanceof VineBlock) {
            int len = getVineLength(world, pos, block, 24);
            int expected = expectedLength(random, popSeed, pos, 3, 8);
            if (len > expected + 20) {
                points += (len - expected) * 0.7f;
                sourceWeight += 1;
                if (originType == null) {
                    originType = OriginType.VINES;
                    originActual = len;
                }
            }
            typeMask |= IndicatorType.VINES.mask;
            indicatorHits++;
            growthHits++;
        }

        if (detectRotatedDeepslate.getValue() && block == Blocks.DEEPSLATE) {
            if (state.contains(Properties.AXIS) && state.get(Properties.AXIS) != Direction.Axis.Y) {
                points += 40.0f;
                sourceWeight += 2;
                if (originType == null) {
                    originType = OriginType.ROTATED;
                    originActual = 1;
                }
            }
            typeMask |= IndicatorType.DEEPSLATE.mask;
            indicatorHits++;
            rotatedHits++;
        }

        return new BlockAnalysisResult(
                points,
                typeMask,
                indicatorHits,
                amethystHits,
                growthHits,
                rotatedHits,
                sourceWeight,
                originType,
                originActual
        );
    }

    private float growthScore(BlockState state, ChunkRandom random, long popSeed, BlockPos pos, float weight, int minDelta) {
        if (!state.contains(Properties.AGE_25)) {
            return 0.0f;
        }
        int actual = state.get(Properties.AGE_25);
        int expected = expectedAge(random, popSeed, pos, 8);
        if (actual <= expected + minDelta) {
            return 0.0f;
        }
        return (actual - expected) * weight;
    }

    private int getAmethystStage(BlockState state) {
        if (state.isOf(Blocks.SMALL_AMETHYST_BUD)) return 1;
        if (state.isOf(Blocks.MEDIUM_AMETHYST_BUD)) return 2;
        if (state.isOf(Blocks.LARGE_AMETHYST_BUD)) return 3;
        if (state.isOf(Blocks.AMETHYST_CLUSTER)) return 4;
        return 0;
    }

    private int getVineLength(World world, BlockPos start, Block block, int maxLen) {
        int length = 1;
        BlockPos.Mutable pos = new BlockPos.Mutable(start.getX(), start.getY(), start.getZ());
        while (length < maxLen) {
            pos.set(pos.getX(), pos.getY() - 1, pos.getZ());
            if (pos.getY() < world.getBottomY()) break;
            if (!world.getBlockState(pos).isOf(block)) break;
            length++;
        }
        return length;
    }

    private int expectedStage(ChunkRandom random, long popSeed, BlockPos pos) {
        random.setSeed(mixSeed(popSeed, pos));
        return random.nextInt(3);
    }

    private int expectedAge(ChunkRandom random, long popSeed, BlockPos pos, int max) {
        random.setSeed(mixSeed(popSeed, pos));
        return random.nextInt(Math.max(1, max));
    }

    private int expectedLength(ChunkRandom random, long popSeed, BlockPos pos, int min, int max) {
        random.setSeed(mixSeed(popSeed, pos));
        int span = Math.max(1, max - min + 1);
        return min + random.nextInt(span);
    }

    private long mixSeed(long popSeed, BlockPos pos) {
        long s = popSeed;
        s ^= (long) pos.getX() * 341873128712L;
        s ^= (long) pos.getZ() * 132897987541L;
        s ^= (long) pos.getY() * 42317861L;
        s ^= s << 13;
        s ^= s >> 7;
        s ^= s << 17;
        return s;
    }

    private float getLongTimeThreshold(int sens, int enabledIndicators) {
        float base = 18.0f;
        float adjust = (sens - 3) * 1.1f;
        float indicatorBoost = Math.max(0, 4 - enabledIndicators) * 1.6f;
        return Math.max(8.0f, base - adjust + indicatorBoost);
    }

    private int getEnabledIndicatorCount() {
        int count = 0;
        if (detectAmethyst.getValue()) count++;
        if (detectVines.getValue()) count++;
        if (detectCaveVines.getValue()) count++;
        if (detectKelp.getValue()) count++;
        if (detectRotatedDeepslate.getValue()) count++;
        return count;
    }

    private int getRequiredTypes(int enabledIndicators) {
        if (enabledIndicators >= 3) return 2;
        return 1;
    }

    private int getRequiredIndicatorCount(int enabledIndicators) {
        if (enabledIndicators >= 4) return 4;
        if (enabledIndicators >= 2) return 3;
        return 1;
    }

    private int getRequiredAmethystHits(int sens) {
        return 10;
    }

    private int getRequiredGrowthHits(int sens) {
        int base = 4;
        int adjust = Math.max(0, (sens - 3) / 2);
        return base + adjust;
    }

    private int getRequiredRotatedHits(int sens) {
        int base = 6;
        int adjust = Math.max(0, (sens - 3) / 2);
        return base + adjust;
    }

    private void addPathCell(Set<ChunkPos> out, int cx, int cz, int width) {
        out.add(new ChunkPos(cx, cz));
        if (width <= 0) return;
        for (int dx = -width; dx <= width; dx++) {
            for (int dz = -width; dz <= width; dz++) {
                out.add(new ChunkPos(cx + dx, cz + dz));
            }
        }
    }

    private void mergeSourceHits(Map<ChunkPos, Integer> target, Map<ChunkPos, Integer> source) {
        for (Map.Entry<ChunkPos, Integer> entry : source.entrySet()) {
            addSourceHit(target, entry.getKey(), entry.getValue());
        }
    }

    private void addSourceHit(Map<ChunkPos, Integer> sourceHits, ChunkPos pos, int weight) {
        sourceHits.put(pos, sourceHits.getOrDefault(pos, 0) + weight);
    }

    private ChunkPos findBestOriginChunk(BlockPos pos, OriginType type, int actual, Map<ChunkPos, Long> popSeedCache) {
        if (type == OriginType.ROTATED) {
            return new ChunkPos(pos);
        }

        int baseX = pos.getX() >> 4;
        int baseZ = pos.getZ() >> 4;
        int bestScore = Integer.MIN_VALUE;
        ChunkPos best = null;

        for (int dx = -ORIGIN_SEARCH_RADIUS; dx <= ORIGIN_SEARCH_RADIUS; dx++) {
            for (int dz = -ORIGIN_SEARCH_RADIUS; dz <= ORIGIN_SEARCH_RADIUS; dz++) {
                ChunkPos candidate = new ChunkPos(baseX + dx, baseZ + dz);
                long popSeed = popSeedCache.computeIfAbsent(
                        candidate,
                        c -> new ChunkRandom(Random.create()).setPopulationSeed(WORLD_SEED, c.getStartX(), c.getStartZ())
                );
                int expected = expectedForType(type, popSeed, pos);
                int delta = actual - expected;
                if (delta > bestScore) {
                    bestScore = delta;
                    best = candidate;
                }
            }
        }

        return bestScore > 0 ? best : null;
    }

    private int expectedForType(OriginType type, long popSeed, BlockPos pos) {
        return switch (type) {
            case AMETHYST -> expectedStage(new ChunkRandom(Random.create()), popSeed, pos);
            case KELP, CAVE_VINES -> expectedAge(new ChunkRandom(Random.create()), popSeed, pos, 8);
            case VINES -> expectedLength(new ChunkRandom(Random.create()), popSeed, pos, 3, 8);
            default -> 0;
        };
    }

    public void render(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (flaggedArea.isEmpty() || mc.player == null || mc.world == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        Vec3d cam = camera.getPos();
        float alphaValue = (float) (alpha.getValue() / 255.0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        MatrixStack stack = new MatrixStack();
        stack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        stack.translate(-cam.x, -cam.y, -cam.z);

        for (ChunkPos cp : flaggedArea) {
            float x1 = cp.getStartX();
            float y1 = RENDER_Y - 0.5f;
            float z1 = cp.getStartZ();
            float x2 = x1 + 16.0f;
            float y2 = RENDER_Y;
            float z2 = z1 + 16.0f;

            drawBox(stack, projectionMatrix, x1, y1, z1, x2, y2, z2, 1.0f, 0.0f, 0.0f, alphaValue);
        }

        RenderSystem.disableBlend();
    }

    private void drawBox(MatrixStack matrices, Matrix4f projectionMatrix,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float r, float g, float b, float a) {
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        line(buffer, matrices, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(buffer, matrices, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(buffer, matrices, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(buffer, matrices, x1, y1, z2, x1, y1, z1, r, g, b, a);

        line(buffer, matrices, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(buffer, matrices, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(buffer, matrices, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(buffer, matrices, x1, y2, z2, x1, y2, z1, r, g, b, a);

        line(buffer, matrices, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(buffer, matrices, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(buffer, matrices, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(buffer, matrices, x1, y1, z2, x1, y2, z2, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void line(BufferBuilder buffer, MatrixStack matrices,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float r, float g, float b, float a) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len <= 0.0001f) {
            len = 1.0f;
        }

        nx /= len;
        ny /= len;
        nz /= len;

        vertex(buffer, matrices, x1, y1, z1, r, g, b, a, nx, ny, nz);
        vertex(buffer, matrices, x2, y2, z2, r, g, b, a, nx, ny, nz);
    }

    private void vertex(BufferBuilder buffer, MatrixStack matrices,
                        float x, float y, float z,
                        float r, float g, float b, float a,
                        float nx, float ny, float nz) {
        MatrixStack.Entry entry = matrices.peek();
        buffer.vertex(entry.getPositionMatrix(), x, y, z)
                .color(r, g, b, a)
                .normal(entry, nx, ny, nz);
    }

    private enum IndicatorType {
        AMETHYST(1),
        VINES(1 << 1),
        CAVE_VINES(1 << 2),
        KELP(1 << 3),
        DEEPSLATE(1 << 4);

        private final int mask;

        IndicatorType(int mask) {
            this.mask = mask;
        }
    }

    private static final class ChunkScanResult {
        private final float score;
        private final int typeMask;
        private final int indicatorCount;
        private final int amethystHits;
        private final int growthHits;
        private final int rotatedHits;
        private final Map<ChunkPos, Integer> sourceHitsLocal;

        private ChunkScanResult(float score, int typeMask, int indicatorCount, int amethystHits, int growthHits,
                                int rotatedHits, Map<ChunkPos, Integer> sourceHitsLocal) {
            this.score = score;
            this.typeMask = typeMask;
            this.indicatorCount = indicatorCount;
            this.amethystHits = amethystHits;
            this.growthHits = growthHits;
            this.rotatedHits = rotatedHits;
            this.sourceHitsLocal = sourceHitsLocal;
        }

        private static ChunkScanResult empty() {
            return new ChunkScanResult(0.0f, 0, 0, 0, 0, 0, new HashMap<>());
        }
    }

    private static final class BlockAnalysisResult {
        private final float points;
        private final int typeMask;
        private final int indicatorHits;
        private final int amethystHits;
        private final int growthHits;
        private final int rotatedHits;
        private final int sourceWeight;
        private final OriginType originType;
        private final int originActual;

        private BlockAnalysisResult(float points, int typeMask, int indicatorHits, int amethystHits, int growthHits,
                                    int rotatedHits, int sourceWeight, OriginType originType, int originActual) {
            this.points = points;
            this.typeMask = typeMask;
            this.indicatorHits = indicatorHits;
            this.amethystHits = amethystHits;
            this.growthHits = growthHits;
            this.rotatedHits = rotatedHits;
            this.sourceWeight = sourceWeight;
            this.originType = originType;
            this.originActual = originActual;
        }
    }

    private enum OriginType {
        AMETHYST,
        KELP,
        CAVE_VINES,
        VINES,
        ROTATED
    }
}