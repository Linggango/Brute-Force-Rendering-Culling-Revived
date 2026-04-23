package misanthropy.brute_force_culling_revived.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class NeatButton extends Button {
    public Supplier<Boolean> getter;
    public Supplier<Component> name;
    private Supplier<Boolean> enable = () -> true;
    private Supplier<Component> detailMessage;
    private Supplier<Integer> textWidth;

    public NeatButton(int x, int y, int w, int h, Supplier<Boolean> getter, @NotNull Consumer<Boolean> setter, @NotNull Supplier<Component> name) {
        super(x, y, w, h, name.get(), (b) -> ((NeatButton) b).updateValue(setter), DEFAULT_NARRATION);
        this.getter = getter;
        this.name = name;
    }

    public NeatButton(int x, int y, int w, int h, Supplier<Boolean> shouldEnable, Supplier<Boolean> getter, @NotNull Consumer<Boolean> setter, @NotNull Supplier<Component> name) {
        this(x, y, w, h, getter, setter, name);
        this.enable = shouldEnable;
    }

    private void updateValue(@NotNull Consumer<Boolean> setter) {
        if (enable.get()) {
            setter.accept(!getter.get());
        }
    }

    public void setTextWidthGetter(Supplier<Integer> widthGetter) {
        this.textWidth = widthGetter;
    }

    public void setDetailMessage(Supplier<Component> detailMessage) {
        this.detailMessage = detailMessage;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (this.textWidth != null) {
            this.width = this.textWidth.get();
        }
        this.setX(minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2);

        Font font = minecraft.font;
        boolean activeValue = getter.get();
        int textColor = (activeValue && enable.get()) ? 0xFFFFFF : 0xA0A0A0;

        guiGraphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ZERO);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float color = activeValue ? (this.isHovered() ? 1.0f : 0.8f) : (this.isHovered() ? 0.7f : 0.5f);
        if (!enable.get()) color = 0.2f;

        buffer.vertex(this.getX(), this.getY() + height, 0.0D).color(color, color, color, 1.0f).endVertex();
        buffer.vertex(this.getX() + width, this.getY() + height, 0.0D).color(color, color, color, 1.0f).endVertex();
        buffer.vertex(this.getX() + width, this.getY(), 0.0D).color(color, color, color, 1.0f).endVertex();
        buffer.vertex(this.getX(), this.getY(), 0.0D).color(color, color, color, 1.0f).endVertex();

        float borderColor = enable.get() ? 0.7f : 0.2f;
        buffer.vertex(this.getX() - 1, this.getY() + height + 1, 0.0D).color(borderColor, borderColor, borderColor, 1.0f).endVertex();
        buffer.vertex(this.getX() + width + 1, this.getY() + height + 1, 0.0D).color(borderColor, borderColor, borderColor, 1.0f).endVertex();
        buffer.vertex(this.getX() + width + 1, this.getY() - 1, 0.0D).color(borderColor, borderColor, borderColor, 1.0f).endVertex();
        buffer.vertex(this.getX() - 1, this.getY() - 1, 0.0D).color(borderColor, borderColor, borderColor, 1.0f).endVertex();

        BufferUploader.drawWithShader(buffer.end());

        guiGraphics.flush();
        RenderSystem.defaultBlendFunc();

        guiGraphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor | 0xFF000000);
        guiGraphics.drawCenteredString(font, activeValue ? "■" : "□", this.getX() + 10, this.getY() + (this.height - 8) / 2, textColor | 0xFF000000);

        RenderSystem.enableDepthTest();
    }

    public @Nullable Component getDetails() {
        return isHovered && detailMessage != null ? detailMessage.get() : null;
    }
}