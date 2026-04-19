package misanthropy.brute_force_culling_revived.api;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import misanthropy.brute_force_culling_revived.api.data.ChunkCullingMap;
import misanthropy.brute_force_culling_revived.api.data.EntityCullingMap;
import misanthropy.brute_force_culling_revived.api.impl.IEntitiesForRender;
import misanthropy.brute_force_culling_revived.api.impl.IRenderChunkInfo;
import misanthropy.brute_force_culling_revived.api.impl.IRenderSectionVisibility;
import misanthropy.brute_force_culling_revived.mixin.AccessorLevelRender;
import misanthropy.brute_force_culling_revived.mixin.AccessorMinecraft;
import misanthropy.brute_force_culling_revived.util.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Checks;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL30.*;

public class CullingStateManager {
    public static final String MOD_ID = "brute_force_culling_revived";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static @Nullable EntityCullingMap ENTITY_CULLING_MAP = null;
    public static volatile @Nullable ChunkCullingMap CHUNK_CULLING_MAP = null;
    public static @NotNull Matrix4f VIEW_MATRIX = new Matrix4f();
    public static @NotNull Matrix4f PROJECTION_MATRIX = new Matrix4f();

    static {
        PROJECTION_MATRIX.identity();
    }

    public static final int DEPTH_SIZE = 5;
    public static int DEPTH_INDEX;
    public static int MAIN_DEPTH_TEXTURE = 0;
    public static RenderTarget @NotNull [] DEPTH_BUFFER_TARGET = new RenderTarget[DEPTH_SIZE];
    public static RenderTarget CHUNK_CULLING_MAP_TARGET;
    public static RenderTarget ENTITY_CULLING_MAP_TARGET;
    public static ShaderInstance CHUNK_CULLING_SHADER;
    public static ShaderInstance COPY_DEPTH_SHADER;
    public static ShaderInstance REMOVE_COLOR_SHADER;
    public static ShaderInstance INSTANCED_ENTITY_CULLING_SHADER;
    public static Frustum FRUSTUM;
    public static boolean updatingDepth;
    public static boolean applyFrustum;
    public static int DEBUG = 0;
    public static int @NotNull [] DEPTH_TEXTURE = new int[DEPTH_SIZE];
    public static @Nullable ShaderLoader SHADER_LOADER = null;
    public static @Nullable Class<?> OptiFine = null;

    public static final LifeTimer<Entity> visibleEntity = new LifeTimer<>();
    public static final LifeTimer<BlockPos> visibleBlock = new LifeTimer<>();
    private static boolean @NotNull [] nextTick = new boolean[20];
    public static int fps = 0;
    private static int tick = 0;
    public static int clientTickCount = 0;
    public static int entityCulling = 0;
    public static int entityCount = 0;
    public static int blockCulling = 0;
    public static int blockCount = 0;
    public static long entityCullingTime = 0;
    public static long blockCullingTime = 0;
    public static long chunkCullingTime = 0;
    private static long preEntityCullingTime = 0;
    private static long preBlockCullingTime = 0;
    private static long preChunkCullingTime = 0;
    public static long preApplyFrustumTime = 0;
    public static long applyFrustumTime = 0;
    public static long chunkCullingInitTime = 0;
    public static long preChunkCullingInitTime = 0;
    public static long entityCullingInitTime = 0;
    public static long preEntityCullingInitTime = 0;
    public static int cullingInitCount = 0;
    public static int preCullingInitCount = 0;
    public static boolean checkCulling = false;
    public static boolean checkTexture = false;
    private static boolean usingShader = false;
    protected static int fullChunkUpdateCooldown = 0;
    private static String shaderName = "";
    public static int LEVEL_SECTION_RANGE;
    public static int LEVEL_POS_RANGE;
    public static int LEVEL_MIN_SECTION_ABS;
    public static int LEVEL_MIN_POS;
    public static Camera CAMERA;
    private static final HashMap<Integer, Integer> SHADER_DEPTH_BUFFER_ID = new HashMap<>();
    private static int frame;
    private static int lastVisibleUpdatedFrame;
    public static volatile boolean useOcclusionCulling = true;
    private static int continueUpdateCount;
    private static boolean lastUpdate;

    static {
        RenderSystem.recordRenderCall(() -> {
            for (int i = 0; i < DEPTH_BUFFER_TARGET.length; ++i) {
                DEPTH_BUFFER_TARGET[i] = new TextureTarget(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), false, Minecraft.ON_OSX);
                DEPTH_BUFFER_TARGET[i].setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            }

            CHUNK_CULLING_MAP_TARGET = new TextureTarget(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), false, Minecraft.ON_OSX);
            CHUNK_CULLING_MAP_TARGET.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            ENTITY_CULLING_MAP_TARGET = new TextureTarget(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), false, Minecraft.ON_OSX);
            ENTITY_CULLING_MAP_TARGET.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        });
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(CullingStateManager.class);

        try {
            OptiFine = Class.forName("net.optifine.shaders.Shaders");
        } catch (ClassNotFoundException e) {
            LOGGER.debug("OptiFine Not Found");
        }

        if (OptiFine != null) {
            try {
                SHADER_LOADER = Class.forName("misanthropy.brute_force_culling_revived.util.IrisLoaderImpl")
                        .asSubclass(ShaderLoader.class)
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ignored) {
            } catch (InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }


        if (ModLoader.hasIris()) {
            try {
                SHADER_LOADER = Class.forName("misanthropy.brute_force_culling_revived.util.IrisLoaderImpl")
                        .asSubclass(ShaderLoader.class)
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void onWorldUnload(Level world) {
        if (world != Minecraft.getInstance().level) {
            cleanup();
        }
    }

    protected static void cleanup() {
        tick = 0;
        clientTickCount = 0;
        visibleEntity.clear();
        visibleBlock.clear();

        ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        if (chunkMap != null) {
            chunkMap.cleanup();
            CHUNK_CULLING_MAP = null;
        }

        EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap != null) {
            entityMap.cleanup();
            ENTITY_CULLING_MAP = null;
        }

        SHADER_DEPTH_BUFFER_ID.clear();
        ModLoader.pauseAsync();
        if(ModLoader.hasSodium()) {
            SodiumSectionAsyncUtil.pauseAsync();
        }
    }

    public static boolean shouldRenderChunk(@Nullable IRenderSectionVisibility section, boolean checkForChunk) {
        if (section == null) {
            return false;
        }

        ChunkCullingMap chunkCullingMap = CHUNK_CULLING_MAP;
        if (chunkCullingMap == null) {
            return true;
        }

        if (DEBUG < 2) {
            if (!useOcclusionCulling) {
                return true;
            }
            if (!section.bruteForceRenderingRevived$shouldCheckVisibility(lastVisibleUpdatedFrame)) {
                return true;
            } else if (chunkCullingMap.isChunkOffsetCameraVisible(section.bruteForceRenderingRevived$getPositionX(), section.bruteForceRenderingRevived$getPositionY(), section.bruteForceRenderingRevived$getPositionZ(), checkForChunk)) {
                section.bruteForceRenderingRevived$updateVisibleTick(lastVisibleUpdatedFrame);
                return true;
            }
            return false;
        }

        if (Config.getAsyncChunkRebuild() && !useOcclusionCulling) {
            return true;
        }

        boolean render;
        boolean actualRender = false;

        if (!section.bruteForceRenderingRevived$shouldCheckVisibility(lastVisibleUpdatedFrame)) {
            render = true;
        } else {
            actualRender = chunkCullingMap.isChunkOffsetCameraVisible(section.bruteForceRenderingRevived$getPositionX(), section.bruteForceRenderingRevived$getPositionY(), section.bruteForceRenderingRevived$getPositionZ(), checkForChunk);
            render = actualRender;
        }

        if (actualRender) {
            section.bruteForceRenderingRevived$updateVisibleTick(lastVisibleUpdatedFrame);
        }

        return render;
    }

    public static boolean shouldSkipBlockEntity(@NotNull BlockEntity blockEntity, @SuppressWarnings("unused") AABB aabb, @NotNull BlockPos pos) {
        blockCount++;

        if (CAMERA.getPosition().distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) >
                Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * 2) {
            return false;
        }

        EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap == null || !Config.getCullBlockEntity()) return false;

        ResourceLocation key = BlockEntityType.getKey(blockEntity.getType());
        if (key == null) return false;
        String type = key.toString();

        if (Config.getBlockEntitiesSkip().contains(type))
            return false;

        boolean visible = false;
        boolean actualVisible;

        if (DEBUG < 2) {
            if (entityMap.isObjectVisible(blockEntity)) {
                visibleBlock.updateUsageTick(pos, clientTickCount);
                visible = true;
            } else if (visibleBlock.contains(pos)) {
                visible = true;
            }
            return !visible;
        }

        long time = System.nanoTime();

        actualVisible = entityMap.isObjectVisible(blockEntity);

        if (actualVisible) {
            visible = true;
        } else if (visibleBlock.contains(pos)) {
            visible = true;
        }

        preBlockCullingTime += System.nanoTime() - time;

        if (checkCulling)
            visible = !visible;

        if (!visible) {
            blockCulling++;
        } else if (actualVisible) {
            visibleBlock.updateUsageTick(pos, clientTickCount);
        }

        return !visible;
    }

    public static boolean shouldSkipEntity(Entity entity) {
        entityCount++;
        if (entity instanceof Player || entity.isCurrentlyGlowing()) return false;
        if (entity.distanceToSqr(CAMERA.getPosition()) < 4) return false;
        if (Config.getEntitiesSkip().contains(entity.getType().getDescriptionId()))
            return false;

        EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap == null || !Config.getCullEntity()) return false;

        boolean visible = false;
        boolean actualVisible;

        if (DEBUG < 2) {
            if (entityMap.isObjectVisible(entity)) {
                visibleEntity.updateUsageTick(entity, clientTickCount);
                visible = true;
            } else if (visibleEntity.contains(entity)) {
                visible = true;
            }
            return !visible;
        }

        long time = System.nanoTime();

        actualVisible = entityMap.isObjectVisible(entity);

        if (actualVisible) {
            visible = true;
        } else if (visibleEntity.contains(entity)) {
            visible = true;
        }

        preEntityCullingTime += System.nanoTime() - time;

        if (checkCulling)
            visible = !visible;

        if (!visible) {
            entityCulling++;
        } else if (actualVisible) {
            visibleEntity.updateUsageTick(entity, clientTickCount);
        }

        return !visible;
    }

    public static void onProfilerPopPush(@NotNull String s) {
        switch (s) {
            case "beforeRunTick" -> {
                if (((AccessorLevelRender) Minecraft.getInstance().levelRenderer).getNeedsFullRenderChunkUpdate() && Minecraft.getInstance().level != null) {
                    if (ModLoader.hasMod("embeddium")) {
                        ModLoader.pauseAsync();
                    }
                    LEVEL_SECTION_RANGE = Minecraft.getInstance().level.getMaxSection() - Minecraft.getInstance().level.getMinSection();
                    LEVEL_MIN_SECTION_ABS = Math.abs(Minecraft.getInstance().level.getMinSection());
                    LEVEL_MIN_POS = Minecraft.getInstance().level.getMinBuildHeight();
                    LEVEL_POS_RANGE = Minecraft.getInstance().level.getMaxBuildHeight() - Minecraft.getInstance().level.getMinBuildHeight();
                }
            }
            case "afterRunTick" -> {
                ++frame;
                updateMapData();
                OcclusionCullerThread.shouldUpdate();
            }
            case "captureFrustum" -> {
                AccessorLevelRender levelFrustum = (AccessorLevelRender) Minecraft.getInstance().levelRenderer;
                Frustum frustum;
                if (levelFrustum.getCapturedFrustum() != null) {
                    frustum = levelFrustum.getCapturedFrustum();
                } else {
                    frustum = levelFrustum.getCullingFrustum();
                }
                CullingStateManager.FRUSTUM = new Frustum(frustum).offsetToFullyIncludeCameraCube(32);

                ChunkCullingMap chunkMap = CullingStateManager.CHUNK_CULLING_MAP;
                if (chunkMap != null) {
                    chunkMap.updateCamera();
                }
                checkShader();
            }
            case "destroyProgress" -> {
                updatingDepth = true;
                updateDepthMap();
                readMapData();
                CullingRenderEvent.updateCullingMap();
                updatingDepth = false;
            }
        }
    }

    public static void onProfilerPush(@NotNull String s) {
        if (s.equals("onKeyboardInput")) {
            ModLoader.onKeyPress();
        } else if (s.equals("center")) {
            CAMERA = Minecraft.getInstance().gameRenderer.getMainCamera();
            int thisTick = clientTickCount % 20;
            nextTick = new boolean[20];

            if (tick != thisTick) {
                tick = thisTick;
                nextTick[thisTick] = true;
            }

            entityCulling = 0;
            entityCount = 0;
            blockCulling = 0;
            blockCount = 0;

            if (anyNextTick() && fullChunkUpdateCooldown > 0) {
                fullChunkUpdateCooldown--;
            }

            if (anyNextTick() && continueUpdateCount > 0) {
                continueUpdateCount--;
            }

            if (isNextLoop()) {
                visibleBlock.tick(clientTickCount, 3);
                visibleEntity.tick(clientTickCount, 3);

                EntityCullingMap entityMap = CullingStateManager.ENTITY_CULLING_MAP;
                if(entityMap != null) {
                    entityMap.getEntityTable().tickTemp(clientTickCount);
                }

                applyFrustumTime = preApplyFrustumTime;
                preApplyFrustumTime = 0;

                entityCullingTime = preEntityCullingTime;
                preEntityCullingTime = 0;

                blockCullingTime = preBlockCullingTime;
                preBlockCullingTime = 0;

                chunkCullingInitTime = preChunkCullingInitTime;
                preChunkCullingInitTime = 0;

                cullingInitCount = preCullingInitCount;
                preCullingInitCount = 0;

                entityCullingInitTime = preEntityCullingInitTime;
                preEntityCullingInitTime = 0;

                ChunkCullingMap chunkMap = CullingStateManager.CHUNK_CULLING_MAP;
                if (chunkMap != null) {
                    chunkMap.lastQueueUpdateCount = chunkMap.queueUpdateCount;
                    chunkMap.queueUpdateCount = 0;
                }

                if (preChunkCullingTime != 0) {
                    chunkCullingTime = preChunkCullingTime;
                    preChunkCullingTime = 0;
                }
            }
        }
    }

    public static void readMapData() {
        if (!checkCulling) {
            if (Config.getCullChunk()) {
                long time = System.nanoTime();
                ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
                if (chunkMap != null && chunkMap.isTransferred()) {
                    chunkMap.readData();
                    lastVisibleUpdatedFrame = frame;
                }
                preChunkCullingInitTime += System.nanoTime() - time;
            }

            if (Config.doEntityCulling()) {
                long time = System.nanoTime();
                EntityCullingMap entityMap = ENTITY_CULLING_MAP;
                if (entityMap != null && entityMap.isTransferred()) {
                    entityMap.readData();
                    lastVisibleUpdatedFrame = frame;
                }
                preEntityCullingInitTime += System.nanoTime() - time;
            }
        }
    }

    public static void checkShader() {
        if (SHADER_LOADER != null) {
            boolean clear = false;
            if (SHADER_LOADER.enabledShader() && !usingShader) {
                clear = true;
                usingShader = true;
            }

            if (!SHADER_LOADER.enabledShader() && usingShader) {
                clear = true;
                usingShader = false;
            }

            if (SHADER_LOADER.enabledShader() && OptiFine != null) {
                String shaderPack = "";
                try {
                    Field field = CullingStateManager.OptiFine.getDeclaredField("currentShaderName");
                    field.setAccessible(true);
                    shaderPack = (String) field.get(null);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.fillInStackTrace();
                }
                if (!Objects.equals(shaderName, shaderPack)) {
                    shaderName = shaderPack;
                    clear = true;
                }
            }

            if (clear) {
                cleanup();
            }
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.@NotNull ComputeCameraAngles event) {
        float roll = event.getRoll();
        float pitch = event.getPitch();
        float yaw = event.getYaw();

        PoseStack viewMatrix = new PoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        viewMatrix.mulPose(Axis.ZP.rotationDegrees(roll));
        viewMatrix.mulPose(Axis.XP.rotationDegrees(pitch));
        viewMatrix.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
        viewMatrix.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);

        CullingStateManager.VIEW_MATRIX = new Matrix4f(viewMatrix.last().pose());
    }

    public static void updateDepthMap() {
        CullingStateManager.PROJECTION_MATRIX = new Matrix4f(RenderSystem.getProjectionMatrix());
        if (anyCulling() && !checkCulling && anyNeedTransfer() && continueUpdateDepth()) {
            float sampling = (float) Config.getSampling();
            Window window = Minecraft.getInstance().getWindow();
            int width = window.getWidth();
            int height = window.getHeight();

            runOnDepthFrame((depthContext) -> {
                int scaleWidth = Math.max(1, (int) (width * sampling * depthContext.scale()));
                int scaleHeight = Math.max(1, (int) (height * sampling * depthContext.scale()));
                if (depthContext.frame().width != scaleWidth || depthContext.frame().height != scaleHeight) {
                    depthContext.frame().resize(scaleWidth, scaleHeight, Minecraft.ON_OSX);
                }
            });

            int depthTexture = Minecraft.getInstance().getMainRenderTarget().getDepthTextureId();
            if (SHADER_LOADER != null && SHADER_LOADER.enabledShader()) {
                if (!SHADER_DEPTH_BUFFER_ID.containsKey(SHADER_LOADER.getFrameBufferID())) {
                    RenderSystem.assertOnRenderThreadOrInit();
                    GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, SHADER_LOADER.getFrameBufferID());

                    int attachmentType = GL_DEPTH_ATTACHMENT;
                    int[] attachmentObjectType = new int[1];
                    glGetFramebufferAttachmentParameteriv(GL_FRAMEBUFFER, attachmentType, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, attachmentObjectType);

                    if (attachmentObjectType[0] == GL_TEXTURE) {
                        int[] depthTextureID = new int[1];
                        glGetFramebufferAttachmentParameteriv(GL_FRAMEBUFFER, attachmentType, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, depthTextureID);
                        depthTexture = depthTextureID[0];
                        SHADER_DEPTH_BUFFER_ID.put(SHADER_LOADER.getFrameBufferID(), depthTexture);
                    }
                } else {
                    depthTexture = SHADER_DEPTH_BUFFER_ID.get(SHADER_LOADER.getFrameBufferID());
                }
            }

            MAIN_DEPTH_TEXTURE = depthTexture;

            runOnDepthFrame((depthContext) -> {
                useShader(CullingStateManager.COPY_DEPTH_SHADER);
                depthContext.frame().clear(Minecraft.ON_OSX);
                depthContext.frame().bindWrite(false);
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferbuilder = tesselator.getBuilder();
                bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                bufferbuilder.vertex(-1.0f, -1.0f, 0.0f).endVertex();
                bufferbuilder.vertex(1.0f, -1.0f, 0.0f).endVertex();
                bufferbuilder.vertex(1.0f, 1.0f, 0.0f).endVertex();
                bufferbuilder.vertex(-1.0f, 1.0f, 0.0f).endVertex();
                RenderSystem.setShaderTexture(0, depthContext.lastTexture());
                tesselator.end();
                DEPTH_TEXTURE[depthContext.index()] = depthContext.frame().getColorTextureId();
            });

            bindMainFrameTarget();
        }
    }

    public static void updateMapData() {
        if (anyCulling()) {
            if (anyNeedTransfer()) {
                preCullingInitCount++;
            }

            if (Config.getCullChunk()) {
                int renderingDiameter = Minecraft.getInstance().options.getEffectiveRenderDistance() * 2 + 1;
                int maxSize = renderingDiameter * LEVEL_SECTION_RANGE * renderingDiameter;
                int cullingSize = (int) Math.sqrt(maxSize) + 1;

                ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;

                if (CHUNK_CULLING_MAP_TARGET.width != cullingSize || CHUNK_CULLING_MAP_TARGET.height != cullingSize) {
                    CHUNK_CULLING_MAP_TARGET.resize(cullingSize, cullingSize, Minecraft.ON_OSX);
                    if (chunkMap != null) {
                        chunkMap.cleanup();
                    }
                    chunkMap = new ChunkCullingMap(CHUNK_CULLING_MAP_TARGET.width, CHUNK_CULLING_MAP_TARGET.height);
                    CHUNK_CULLING_MAP = chunkMap;
                    chunkMap.generateIndex(Minecraft.getInstance().options.getEffectiveRenderDistance());
                }

                if (chunkMap == null) {
                    chunkMap = new ChunkCullingMap(CHUNK_CULLING_MAP_TARGET.width, CHUNK_CULLING_MAP_TARGET.height);
                    CHUNK_CULLING_MAP = chunkMap;
                    EntityCullingMap entityMap = ENTITY_CULLING_MAP;
                    if (entityMap != null) {
                        entityMap.syncDelay(chunkMap);
                    }
                    chunkMap.generateIndex(Minecraft.getInstance().options.getEffectiveRenderDistance());
                }

                long time = System.nanoTime();
                chunkMap.transferData();
                preChunkCullingInitTime += System.nanoTime() - time;
            }

            if (Config.doEntityCulling()) {
                EntityCullingMap entityMap = ENTITY_CULLING_MAP;

                if (entityMap == null) {
                    entityMap = new EntityCullingMap(ENTITY_CULLING_MAP_TARGET.width, ENTITY_CULLING_MAP_TARGET.height);
                    ENTITY_CULLING_MAP = entityMap;
                    ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
                    if (chunkMap != null) {
                        chunkMap.syncDelay(entityMap);
                    }
                }

                int cullingSize = (entityMap.getEntityTable().size() / 64 * 64 + 64) / 8 + 1;
                if (CullingStateManager.ENTITY_CULLING_MAP_TARGET.width != 8 || CullingStateManager.ENTITY_CULLING_MAP_TARGET.height != cullingSize) {
                    CullingStateManager.ENTITY_CULLING_MAP_TARGET.resize(8, cullingSize, Minecraft.ON_OSX);

                    EntityCullingMap temp = entityMap;
                    entityMap = new EntityCullingMap(ENTITY_CULLING_MAP_TARGET.width, ENTITY_CULLING_MAP_TARGET.height);
                    ENTITY_CULLING_MAP = entityMap;
                    entityMap.getEntityTable().copyTemp(temp.getEntityTable(), clientTickCount);
                    temp.cleanup();
                }

                long time = System.nanoTime();
                entityMap.transferData();
                preEntityCullingInitTime += System.nanoTime() - time;

                if (Minecraft.getInstance().level != null) {
                    entityMap.getEntityTable().clearIndexMap();
                    Iterable<Entity> entities = Minecraft.getInstance().level.entitiesForRendering();

                    EntityCullingMap finalEntityMap = entityMap;
                    entities.forEach(entity -> finalEntityMap.getEntityTable().addObject(entity));
                    for (Object levelrenderer$renderchunkinfo : ((IEntitiesForRender) Minecraft.getInstance().levelRenderer).bruteForceRenderingRevived$renderChunksInFrustum()) {
                        List<BlockEntity> list = ((IRenderChunkInfo) levelrenderer$renderchunkinfo).bruteForceRenderingRevived$getRenderChunk().getCompiledChunk().getRenderableBlockEntities();
                        list.forEach(entity -> finalEntityMap.getEntityTable().addObject(entity));
                    }

                    entityMap.getEntityTable().addAllTemp();
                }
            }

            fps = ((AccessorMinecraft) Minecraft.getInstance()).getFps();
        } else {
            EntityCullingMap entityMap = ENTITY_CULLING_MAP;
            if (entityMap != null) {
                entityMap.cleanup();
                ENTITY_CULLING_MAP = null;
            }
            ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
            if (chunkMap != null) {
                chunkMap.cleanup();
                CHUNK_CULLING_MAP = null;
            }
        }
    }

    public static void useShader(ShaderInstance instance) {
        RenderSystem.setShader(() -> instance);
    }

    public static void bindMainFrameTarget() {
        if (SHADER_LOADER != null && SHADER_LOADER.renderingShaderPass()) {
            SHADER_LOADER.bindDefaultFrameBuffer();
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    public static void runOnDepthFrame(@NotNull Consumer<DepthContext> consumer) {
        float f = 1.0f;
        for (DEPTH_INDEX = 0; DEPTH_INDEX < DEPTH_BUFFER_TARGET.length; ++DEPTH_INDEX) {
            int lastTexture = DEPTH_INDEX == 0 ? MAIN_DEPTH_TEXTURE : DEPTH_BUFFER_TARGET[DEPTH_INDEX - 1].getColorTextureId();
            consumer.accept(new DepthContext(DEPTH_BUFFER_TARGET[DEPTH_INDEX], DEPTH_INDEX, f, lastTexture));
            f *= 0.35f;
        }
    }

    public static void callDepthTexture() {
        CullingStateManager.runOnDepthFrame(depthContext -> RenderSystem.setShaderTexture(depthContext.index(), CullingStateManager.DEPTH_TEXTURE[depthContext.index()]));
    }

    public static boolean renderingIris() {
        return renderingShader() && OptiFine == null;
    }

    public static boolean renderingShader() {
        return SHADER_LOADER != null && SHADER_LOADER.renderingShaderPass();
    }

    public static boolean enabledShader() {
        return SHADER_LOADER != null && SHADER_LOADER.enabledShader();
    }

    public static boolean anyNextTick() {
        for (int i = 0; i < 20; ++i) {
            if (nextTick[i])
                return true;
        }
        return false;
    }

    public static boolean isNextLoop() {
        return nextTick[0];
    }

    public static boolean anyCulling() {
        return Config.getCullChunk() || Config.doEntityCulling();
    }

    public static boolean anyNeedTransfer() {
        EntityCullingMap entityMap = CullingStateManager.ENTITY_CULLING_MAP;
        ChunkCullingMap chunkMap = CullingStateManager.CHUNK_CULLING_MAP;
        return (entityMap != null && entityMap.needTransferData()) ||
                (chunkMap != null && chunkMap.needTransferData());
    }

    private static int gl33 = -1;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean gl33() {
        if (RenderSystem.isOnRenderThread()) {
            if (gl33 < 0)
                gl33 = (GL.getCapabilities().OpenGL33 || Checks.checkFunctions(GL.getCapabilities().glVertexAttribDivisor)) ? 1 : 0;
        }
        return gl33 == 1;
    }

    public static boolean needPauseRebuild() {
        return fullChunkUpdateCooldown > 0;
    }

    public static int mapChunkY(double posY) {
        double offset = posY - LEVEL_MIN_POS;
        double mappingRatio = offset / LEVEL_POS_RANGE;

        return (int) Math.floor(mappingRatio * LEVEL_SECTION_RANGE);
    }

    public static void updating() {
        continueUpdateCount = 10;
        lastUpdate = true;
    }

    public static boolean continueUpdateChunk() {
        if (continueUpdateCount > 0) {
            return true;
        } else if (lastUpdate) {
            lastUpdate = false;
            return true;
        }

        return false;
    }

    public static boolean continueUpdateDepth() {
        return continueUpdateCount > 0;
    }
}