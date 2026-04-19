package misanthropy.brute_force_culling_revived.util;

import com.mojang.blaze3d.platform.GlStateManager;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
public class OptiFineLoaderImpl implements ShaderLoader {
    public static @Nullable Class<?> glState = null;

    @Override
    public int getFrameBufferID() {
        try {
            assert CullingStateManager.OptiFine != null;
            Field f = CullingStateManager.OptiFine.getDeclaredField("dfb");
            f.setAccessible(true);
            Object dfb = f.get(null);
            Field buffer = dfb.getClass().getDeclaredField("glFramebuffer");
            buffer.setAccessible(true);
            return (int) buffer.get(dfb);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.fillInStackTrace();
        }

        return Minecraft.getInstance().getMainRenderTarget().frameBufferId;
    }

    @Override
    public boolean renderingShaderPass() {
        try {
            assert CullingStateManager.OptiFine != null;
            Field field = CullingStateManager.OptiFine.getDeclaredField("shaderPackLoaded");
            field.setAccessible(true);
            return (Boolean) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.fillInStackTrace();
        }
        return false;
    }

    @Override
    public boolean enabledShader() {
        return renderingShaderPass();
    }

    @Override
    public void bindDefaultFrameBuffer() {
        if (glState == null) {
            try {
                glState = Class.forName("net.optifine.shaders.GlState");
            } catch (ClassNotFoundException e) {
                CullingStateManager.LOGGER.debug("GlState Not Found");
            }
        }
        try {
            Field field = glState.getDeclaredField("activeFramebuffer");
            field.setAccessible(true);
            Object buffer = field.get(null);
            Field glFramebuffer = buffer.getClass().getDeclaredField("glFramebuffer");
            glFramebuffer.setAccessible(true);
            GlStateManager._glBindFramebuffer(36160, (int) glFramebuffer.get(buffer));
            GlStateManager._viewport(0, 0, Minecraft.getInstance().getMainRenderTarget().viewWidth, Minecraft.getInstance().getMainRenderTarget().viewHeight);
            return;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.fillInStackTrace();
        }
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }
}
