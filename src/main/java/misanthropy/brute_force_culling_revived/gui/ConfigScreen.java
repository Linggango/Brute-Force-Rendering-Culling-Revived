package misanthropy.brute_force_culling_revived.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import misanthropy.brute_force_culling_revived.api.Config;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import misanthropy.brute_force_culling_revived.api.ModLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfigScreen extends Screen {
    private boolean release = false;
    int heightScale;
    int textWidth;

    public ConfigScreen(@NotNull Component titleIn) {
        super(titleIn);
        heightScale = (int) (Minecraft.getInstance().font.lineHeight * 2f + 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static float u(int width) {
        return (float) width / Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static float v(int height) {
        return 1.0f - ((float) height / (Minecraft.getInstance().getWindow().getGuiScaledHeight()));
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth() / 2;
        int widthScale = textWidth / 2 + 15;
        int right = width - widthScale;
        int left = width + widthScale;
        int bottom = (int) (minecraft.getWindow().getGuiScaledHeight() * 0.8) + 20;
        int top = bottom - heightScale * children().size() - 10;

        float bgColor = 1.0f;
        float bgAlpha = 0.3f;

        guiGraphics.flush();
        RenderSystem.disableDepthTest();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.1f);
        CullingStateManager.useShader(CullingStateManager.REMOVE_COLOR_SHADER);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferbuilder.vertex(right - 1, bottom + 1, 0.0D).color(bgColor, bgColor, bgColor, bgAlpha).uv(u(right - 1), v(bottom + 1)).endVertex();
        bufferbuilder.vertex(left + 1, bottom + 1, 0.0D).color(bgColor, bgColor, bgColor, bgAlpha).uv(u(left + 1), v(bottom + 1)).endVertex();
        bufferbuilder.vertex(left + 1, top - 1, 0.0D).color(bgColor, bgColor, bgColor, bgAlpha).uv(u(left + 1), v(top - 1)).endVertex();
        bufferbuilder.vertex(right - 1, top - 1, 0.0D).color(bgColor, bgColor, bgColor, bgAlpha).uv(u(right - 1), v(top - 1)).endVertex();

        RenderSystem.setShaderTexture(0, minecraft.getMainRenderTarget().getColorTextureId());
        BufferUploader.drawWithShader(bufferbuilder.end());

        bgAlpha = 1.0f;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(right, bottom, 0.0D).color(bgColor, bgColor, bgColor, bgAlpha).endVertex();
        bufferbuilder.vertex(left, bottom, 0.0D).color(bgColor, bgColor, bgColor, bgAlpha).endVertex();
        bufferbuilder.vertex(left, top, 0.0D).color(bgColor, bgColor, bgColor, bgAlpha).endVertex();
        bufferbuilder.vertex(right, top, 0.0D).color(bgColor, bgColor, bgColor, bgAlpha).endVertex();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ZERO);
        BufferUploader.drawWithShader(bufferbuilder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0f);
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.minecraft != null && (this.minecraft.options.keyInventory.matches(key, scan) || this.minecraft.options.keyPlayerList.matches(key, scan))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (ModLoader.CONFIG_KEY.matches(key, scan)) {
            if (release) {
                this.onClose();
                return true;
            } else {
                release = true;
            }
        }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    protected void init() {
        if (this.minecraft == null || this.minecraft.player == null) {
            onClose();
            return;
        }

        if (this.minecraft.player.getName().getString().equals("Dev")) {
            addConfigButton(() -> CullingStateManager.checkCulling, (b) -> CullingStateManager.checkCulling = b, () -> Component.literal("Debug"))
                    .setDetailMessage(() -> Component.translatable("brute_force_culling_revived.detail.debug"));

            addConfigButton(() -> CullingStateManager.checkTexture, (b) -> CullingStateManager.checkTexture = b, () -> Component.literal("Check Texture"))
                    .setDetailMessage(() -> Component.translatable("brute_force_culling_revived.detail.check_texture"));
        }

        addConfigButton(Config::getSampling, (value) -> {
            double format = Mth.floor(value * 20) * 0.05;
            format = Double.parseDouble(String.format("%.2f", format));
            Config.setSampling(format);
            return format;
        }, (value) -> Mth.floor(value * 100) + "%", () -> Component.translatable("brute_force_culling_revived.sampler"))
                .setDetailMessage(() -> Component.translatable("brute_force_culling_revived.detail.sampler"));

        addConfigButton(() -> Config.getDepthUpdateDelay() / 10d, (value) -> {
            int format = Mth.floor(value * 10);
            if (format > 0) format -= Config.getShaderDynamicDelay();
            Config.setDepthUpdateDelay(format);
            format += Config.getShaderDynamicDelay();
            return format * 0.1;
        }, (value) -> String.valueOf(Mth.floor(value * 10)), () -> Component.translatable("brute_force_culling_revived.culling_map_update_delay"))
                .setDetailMessage(() -> Component.translatable("brute_force_culling_revived.detail.culling_map_update_delay"));

        addConfigButton(Config::getAutoDisableAsync, Config::setAutoDisableAsync, () -> Component.translatable("brute_force_culling_revived.auto_shader_async"))
                .setDetailMessage(() -> Component.translatable("brute_force_culling_revived.detail.auto_shader_async"));

        addConfigButton(() -> Config.getCullChunk() && ModLoader.hasSodium() && !ModLoader.hasNvidium(), Config::getAsyncChunkRebuild, Config::setAsyncChunkRebuild, () -> Component.translatable("brute_force_culling_revived.async"))
                .setDetailMessage(() -> {
                    if (ModLoader.hasNvidium()) return Component.translatable("brute_force_culling_revived.detail.nvidium");
                    if (!ModLoader.hasSodium()) return Component.translatable("brute_force_culling_revived.detail.sodium");
                    return Component.translatable("brute_force_culling_revived.detail.async");
                });

        addConfigButton(Config::getCullChunk, Config::setCullChunk, () -> Component.translatable("brute_force_culling_revived.cull_chunk"))
                .setDetailMessage(() -> Component.translatable("brute_force_culling_revived.detail.cull_chunk"));

        addConfigButton(Config::getCullBlockEntity, Config::setCullBlockEntity, () -> Component.translatable("brute_force_culling_revived.cull_block_entity"))
                .setDetailMessage(() -> CullingStateManager.gl33() ? Component.translatable("brute_force_culling_revived.detail.cull_block_entity") : Component.translatable("brute_force_culling_revived.detail.gl33"));

        addConfigButton(Config::getCullEntity, Config::setCullEntity, () -> Component.translatable("brute_force_culling_revived.cull_entity"))
                .setDetailMessage(() -> CullingStateManager.gl33() ? Component.translatable("brute_force_culling_revived.detail.cull_entity") : Component.translatable("brute_force_culling_revived.detail.gl33"));

        super.init();
    }

    public @NotNull NeatButton addConfigButton(Supplier<Boolean> getter, @NotNull Consumer<Boolean> setter, @NotNull Supplier<Component> displayText) {
        int w = 150;
        int x = this.width / 2 - w / 2;
        NeatButton button = new NeatButton(x, (int) ((height * 0.8) - heightScale * children().size()), w, 14, getter, setter, displayText);
        this.addRenderableWidget(button);
        this.textWidth = Math.max(Math.max(w, font.width(displayText.get()) + 40), this.textWidth);
        button.setTextWidthGetter(() -> this.textWidth);
        return button;
    }

    public @NotNull NeatButton addConfigButton(Supplier<Boolean> enable, Supplier<Boolean> getter, @NotNull Consumer<Boolean> setter, @NotNull Supplier<Component> displayText) {
        int w = 150;
        int x = this.width / 2 - w / 2;
        NeatButton button = new NeatButton(x, (int) ((height * 0.8) - heightScale * children().size()), w, 14, enable, getter, setter, displayText);
        this.addRenderableWidget(button);
        this.textWidth = Math.max(Math.max(w, font.width(displayText.get()) + 40), this.textWidth);
        button.setTextWidthGetter(() -> this.textWidth);
        return button;
    }

    public @NotNull NeatSliderButton addConfigButton(@NotNull Supplier<Double> getter, @NotNull Function<Double, Double> setter, @NotNull Function<Double, String> display, @NotNull Supplier<MutableComponent> displayText) {
        int w = 150;
        int x = this.width / 2 - w / 2;
        NeatSliderButton button = new NeatSliderButton(x, (int) ((height * 0.8) - heightScale * children().size()), w, 14, getter, setter, display, displayText);
        this.addRenderableWidget(button);
        this.textWidth = Math.max(Math.max(w, font.width(displayText.get()) + 40), this.textWidth);
        button.setTextWidthGetter(() -> this.textWidth);
        return button;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        for (GuiEventListener child : children()) {
            Component details = null;
            if (child instanceof NeatButton b) details = b.getDetails();
            else if (child instanceof NeatSliderButton b) details = b.getDetails();

            if (details != null) {
                renderButtonDetails(guiGraphics, details);
            }
        }
    }

    private void renderButtonDetails(@NotNull GuiGraphics guiGraphics, @NotNull Component details) {
        String[] parts = details.getString().split("\\n");
        int totalHeight = 0;
        int maxTextWidth = Math.min(this.width - 20, 202);

        for (String part : parts) {
            String cleaned = part.replace("warn:", "");
            List<FormattedCharSequence> lines = font.split(Component.literal(cleaned), maxTextWidth);
            totalHeight += lines.isEmpty() ? font.lineHeight / 2 : lines.size() * font.lineHeight + font.lineHeight / 4;
        }

        int x = this.width / 2 - maxTextWidth / 2;
        int y = 4;

        guiGraphics.fill(x - 2, y - 2, x + maxTextWidth + 2, y + totalHeight + 2, 0xB0000000);

        int currentY = y;
        for (String part : parts) {
            boolean isWarning = part.contains("warn:");
            String cleaned = part.replace("warn:", "");
            List<FormattedCharSequence> lines = font.split(Component.literal(cleaned), maxTextWidth);

            for (FormattedCharSequence line : lines) {
                guiGraphics.drawString(font, line, x, currentY, isWarning ? 0xFFFF5555 : 0xFFFFFFFF, false);
                currentY += font.lineHeight;
            }
            if (lines.isEmpty()) currentY += font.lineHeight / 2;
            else currentY += font.lineHeight / 4;
        }
    }
}