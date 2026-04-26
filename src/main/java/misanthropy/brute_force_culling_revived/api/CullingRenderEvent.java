package misanthropy.brute_force_culling_revived.api;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.*;
import misanthropy.brute_force_culling_revived.api.data.ChunkCullingMap;
import misanthropy.brute_force_culling_revived.api.data.EntityCullingMap;
import misanthropy.brute_force_culling_revived.api.impl.ICullingShader;
import misanthropy.brute_force_culling_revived.instanced.EntityCullingInstanceRenderer;
import misanthropy.brute_force_culling_revived.mixin.AccessorFrustum;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CullingRenderEvent {

    public static EntityCullingInstanceRenderer ENTITY_CULLING_INSTANCE_RENDERER;

    private static final float[] VEC3_BUFFER = new float[3];
    private static final float[] FRUSTUM_BUFFER = new float[24];
    private static final float[] DEPTH_SIZE_BUFFER = new float[10];

    static {
        RenderSystem.recordRenderCall(() -> ENTITY_CULLING_INSTANCE_RENDERER = new EntityCullingInstanceRenderer());
    }

    private long lastDebugUpdateTime;
    private final List<String> cachedMonitorTexts = new ArrayList<>();
    private int cachedWidthScale = 80;
    private int cachedBottom = 0;

    protected static void updateCullingMap() {
        if (!CullingStateManager.anyCulling() || CullingStateManager.checkCulling)
            return;

        CullingStateManager.callDepthTexture();

        EntityCullingMap entityCullingMap = CullingStateManager.ENTITY_CULLING_MAP;
        if (Config.doEntityCulling() && entityCullingMap != null && entityCullingMap.needTransferData()) {
            CullingStateManager.ENTITY_CULLING_MAP_TARGET.clear(Minecraft.ON_OSX);
            CullingStateManager.ENTITY_CULLING_MAP_TARGET.bindWrite(false);
            entityCullingMap.getEntityTable().addEntityAttribute(CullingRenderEvent.ENTITY_CULLING_INSTANCE_RENDERER::addInstanceAttrib);
            ENTITY_CULLING_INSTANCE_RENDERER.drawWithShader(CullingStateManager.INSTANCED_ENTITY_CULLING_SHADER);
        }

        ChunkCullingMap chunkCullingMap = CullingStateManager.CHUNK_CULLING_MAP;
        if (Config.getCullChunk() && chunkCullingMap != null && chunkCullingMap.needTransferData()) {
            CullingStateManager.useShader(CullingStateManager.CHUNK_CULLING_SHADER);
            CullingStateManager.CHUNK_CULLING_MAP_TARGET.clear(Minecraft.ON_OSX);
            CullingStateManager.CHUNK_CULLING_MAP_TARGET.bindWrite(false);

            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuilder();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bufferbuilder.vertex(-1.0f, -1.0f, 0.0f).endVertex();
            bufferbuilder.vertex(1.0f, -1.0f, 0.0f).endVertex();
            bufferbuilder.vertex(1.0f, 1.0f, 0.0f).endVertex();
            bufferbuilder.vertex(-1.0f, 1.0f, 0.0f).endVertex();
            BufferUploader.drawWithShader(bufferbuilder.end());
        }

        CullingStateManager.bindMainFrameTarget();
    }

    private void renderText(@NotNull GuiGraphics guiGraphics, @NotNull List<String> list, int width, int height, Font font) {
        for (int i = 0; i < list.size(); ++i) {
            String text = list.get(i);
            guiGraphics.drawString(font, text, (int) (width - (font.width(text) / 2f)), height + font.lineHeight * i, 0xFFFFFF);
        }
    }

    @SuppressWarnings("resource")
    public static void setUniform(@NotNull ShaderInstance shader) {
        ICullingShader shaderInstance = (ICullingShader) shader;
        Minecraft mc = Minecraft.getInstance();
        GameRenderer gameRenderer = mc.gameRenderer;

        Uniform cameraPos = shaderInstance.bruteForceRenderingRevived$getCullingCameraPos();
        if (cameraPos != null) {
            Vec3 pos = gameRenderer.getMainCamera().getPosition();
            VEC3_BUFFER[0] = (float) pos.x;
            VEC3_BUFFER[1] = (float) pos.y;
            VEC3_BUFFER[2] = (float) pos.z;
            cameraPos.set(VEC3_BUFFER);
        }

        Uniform cameraDir = shaderInstance.bruteForceRenderingRevived$getCullingCameraDir();
        if (cameraDir != null) {
            Vector3f dir = gameRenderer.getMainCamera().getLookVector();
            VEC3_BUFFER[0] = dir.x;
            VEC3_BUFFER[1] = dir.y;
            VEC3_BUFFER[2] = dir.z;
            cameraDir.set(VEC3_BUFFER);
        }

        Uniform boxScale = shaderInstance.bruteForceRenderingRevived$getBoxScale();
        if (boxScale != null) boxScale.set(4.0f);

        if (CullingStateManager.FRUSTUM != null) {
            Uniform frustumPos = shaderInstance.bruteForceRenderingRevived$getFrustumPos();
            if (frustumPos != null) {
                AccessorFrustum accessor = (AccessorFrustum) CullingStateManager.FRUSTUM;
                VEC3_BUFFER[0] = (float) accessor.camX();
                VEC3_BUFFER[1] = (float) accessor.camY();
                VEC3_BUFFER[2] = (float) accessor.camZ();
                frustumPos.set(VEC3_BUFFER);
            }

            Uniform frustum = shaderInstance.bruteForceRenderingRevived$getCullingFrustum();
            if (frustum != null) {
                Vector4f[] frustumData = ModLoader.getFrustumPlanes(((AccessorFrustum) CullingStateManager.FRUSTUM).frustumIntersection());

                Arrays.fill(FRUSTUM_BUFFER, 0.0f);
                int planeCount = Math.min(frustumData.length, 6);
                for (int i = 0; i < planeCount; i++) {
                    Vector4f vec = frustumData[i];
                    FRUSTUM_BUFFER[i * 4]     = vec.x();
                    FRUSTUM_BUFFER[i * 4 + 1] = vec.y();
                    FRUSTUM_BUFFER[i * 4 + 2] = vec.z();
                    FRUSTUM_BUFFER[i * 4 + 3] = vec.w();
                }
                frustum.set(FRUSTUM_BUFFER);
            }
        }

        Uniform viewMat = shaderInstance.bruteForceRenderingRevived$getCullingViewMat();
        if (viewMat != null) viewMat.set(CullingStateManager.VIEW_MATRIX);

        Uniform projMat = shaderInstance.bruteForceRenderingRevived$getCullingProjMat();
        if (projMat != null) projMat.set(CullingStateManager.PROJECTION_MATRIX);

        Uniform renderDist = shaderInstance.bruteForceRenderingRevived$getRenderDistance();
        if (renderDist != null) {
            float distance = mc.options.getEffectiveRenderDistance();
            if (shader == CullingStateManager.COPY_DEPTH_SHADER) {
                distance = (CullingStateManager.DEPTH_INDEX > 0) ? 2.0f : 0.0f;
            }
            renderDist.set(distance);
        }

        if (shader == CullingStateManager.COPY_DEPTH_SHADER
                && CullingStateManager.DEPTH_INDEX > 0
                && shader.SCREEN_SIZE != null) {
            shader.SCREEN_SIZE.set(
                    (float) CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX - 1].width,
                    (float) CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX - 1].height);
        }

        Uniform depthSize = shaderInstance.bruteForceRenderingRevived$getDepthSize();
        if (depthSize != null) {

            Arrays.fill(DEPTH_SIZE_BUFFER, 0.0f);
            if (shader == CullingStateManager.COPY_DEPTH_SHADER) {
                DEPTH_SIZE_BUFFER[0] = (float) CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX].width;
                DEPTH_SIZE_BUFFER[1] = (float) CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX].height;
            } else {

                for (int i = 0; i < CullingStateManager.DEPTH_SIZE; ++i) {
                    DEPTH_SIZE_BUFFER[i * 2]     = (float) CullingStateManager.DEPTH_BUFFER_TARGET[i].width;
                    DEPTH_SIZE_BUFFER[i * 2 + 1] = (float) CullingStateManager.DEPTH_BUFFER_TARGET[i].height;
                }
            }
            depthSize.set(DEPTH_SIZE_BUFFER);
        }

        Uniform cullingSize = shaderInstance.bruteForceRenderingRevived$getCullingSize();
        if (cullingSize != null) {
            cullingSize.set(
                    (float) CullingStateManager.CHUNK_CULLING_MAP_TARGET.width,
                    (float) CullingStateManager.CHUNK_CULLING_MAP_TARGET.height);
        }

        Uniform entityCullingSize = shaderInstance.bruteForceRenderingRevived$getEntityCullingSize();
        if (entityCullingSize != null) {
            entityCullingSize.set(
                    (float) CullingStateManager.ENTITY_CULLING_MAP_TARGET.width,
                    (float) CullingStateManager.ENTITY_CULLING_MAP_TARGET.height);
        }

        Uniform levelHeightOffset = shaderInstance.bruteForceRenderingRevived$getLevelHeightOffset();
        if (levelHeightOffset != null) {
            levelHeightOffset.set(CullingStateManager.LEVEL_SECTION_RANGE);
        }

        Uniform levelMinSection = shaderInstance.bruteForceRenderingRevived$getLevelMinSection();
        if (levelMinSection != null) {
            Level level = mc.level;
            if (level != null) levelMinSection.set(level.getMinSection());
        }
    }

    @SubscribeEvent
    public void onOverlayRender(@NotNull RenderGuiOverlayEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || CullingStateManager.DEBUG <= 0) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HELMET.id())) return;

        long currentTime = Util.getMillis();
        Font font = mc.font;

        if (currentTime - lastDebugUpdateTime > 100) {
            lastDebugUpdateTime = currentTime;
            cachedMonitorTexts.clear();

            String fpsStr = mc.fpsString;
            int spaceIdx = fpsStr.indexOf(' ');
            cachedMonitorTexts.add("FPS: " + (spaceIdx != -1 ? fpsStr.substring(0, spaceIdx) : fpsStr));

            String on  = I18n.get("brute_force_culling_revived.enable");
            String off = I18n.get("brute_force_culling_revived.disable");

            cachedMonitorTexts.add(I18n.get("brute_force_culling_revived.cull_entity") + ": " + (Config.getCullEntity() ? on : off));
            cachedMonitorTexts.add(I18n.get("brute_force_culling_revived.entity_culling") + ": " +
                    CullingStateManager.entityCulling + "/" + CullingStateManager.entityCount +
                    " (" + String.format("%.2f", CullingStateManager.entityCullingTime / 1_000_000.0) + "ms)");

            cachedMonitorTexts.add(I18n.get("brute_force_culling_revived.cull_block_entity") + ": " + (Config.getCullBlockEntity() ? on : off));
            cachedMonitorTexts.add(I18n.get("brute_force_culling_revived.block_culling") + ": " +
                    CullingStateManager.blockCulling + "/" + CullingStateManager.blockCount +
                    " (" + String.format("%.2f", CullingStateManager.blockCullingTime / 1_000_000.0) + "ms)");

            cachedMonitorTexts.add(I18n.get("brute_force_culling_revived.cull_chunk") + ": " + (Config.getCullChunk() ? on : off));
            cachedMonitorTexts.add(I18n.get("brute_force_culling_revived.chunk_culling_time") + ": " +
                    String.format("%.2f", CullingStateManager.chunkCullingTime / 1_000_000.0) + "ms");
            cachedMonitorTexts.add(I18n.get("brute_force_culling_revived.chunk_culling_init") + ": " +
                    String.format("%.2f", CullingStateManager.chunkCullingInitTime / 1_000_000.0) + "ms (" + CullingStateManager.cullingInitCount + ")");

            int maxTextWidth = 0;
            for (String s : cachedMonitorTexts) {
                maxTextWidth = Math.max(maxTextWidth, font.width(s));
            }
            cachedWidthScale = Math.max(80, maxTextWidth / 2 + 10);
            cachedBottom = 20 + (font.lineHeight * cachedMonitorTexts.size());
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth() / 2;
        int height = 20;

        guiGraphics.fill(width - cachedWidthScale - 2, height - 2, width + cachedWidthScale + 2, cachedBottom + 2, 0x66000000);
        renderText(guiGraphics, cachedMonitorTexts, width, height, font);

        if (CullingStateManager.checkTexture) {
            renderTexturePreviews(guiGraphics, mc);
        }
    }

    private void renderTexturePreviews(GuiGraphics guiGraphics, Minecraft mc) {
        float scale = 0.4f;
        int screenH = mc.getWindow().getGuiScaledHeight();
        int screenW = mc.getWindow().getGuiScaledWidth();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        for (int i = 0; i < CullingStateManager.DEPTH_TEXTURE.length; i++) {
            int size = (int) (screenH * scale);
            drawTextureId(guiGraphics, CullingStateManager.DEPTH_TEXTURE[i], 0, screenH - size, size, size);
            scale *= 0.5f;
        }

        int mapSize = (int) (screenH * 0.25f);
        if (Config.doEntityCulling()) {
            drawTextureId(guiGraphics, CullingStateManager.ENTITY_CULLING_MAP_TARGET.getColorTextureId(), screenW - mapSize, 0, mapSize, mapSize);
        }
        if (Config.getCullChunk()) {
            drawTextureId(guiGraphics, CullingStateManager.CHUNK_CULLING_MAP_TARGET.getColorTextureId(), screenW - mapSize, mapSize, mapSize, mapSize);
        }
    }

    private void drawTextureId(GuiGraphics guiGraphics, int textureId, int x, int y, int width, int height) {
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix, (float) x,         (float) y + height, 0.0F).uv(0.0F, 1.0F).endVertex();
        bufferbuilder.vertex(matrix, (float) x + width, (float) y + height, 0.0F).uv(1.0F, 1.0F).endVertex();
        bufferbuilder.vertex(matrix, (float) x + width, (float) y,          0.0F).uv(1.0F, 0.0F).endVertex();
        bufferbuilder.vertex(matrix, (float) x,         (float) y,          0.0F).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
    }
}