package misanthropy.brute_force_culling_revived.util;

import com.mojang.blaze3d.platform.GlStateManager;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class OptiFineLoaderImpl implements ShaderLoader {
    public static @Nullable Class<?> glState = null;
    private static Field dfbField;
    private static Field dfbGlFramebufferField;
    private static Field shaderPackLoadedField;
    private static Field activeFramebufferField;
    private static Field stateGlFramebufferField;

    @Override
    public int getFrameBufferID() {
        try {
            if (dfbField == null) {
                dfbField = CullingStateManager.OptiFine.getDeclaredField("dfb");
                dfbField.setAccessible(true);
            }
            Object dfb = dfbField.get(null);

            if (dfb != null) {
                if (dfbGlFramebufferField == null) {
                    dfbGlFramebufferField = dfb.getClass().getDeclaredField("glFramebuffer");
                    dfbGlFramebufferField.setAccessible(true);
                }
                return (int) dfbGlFramebufferField.get(dfb);
            }
        } catch (Exception e) {
            CullingStateManager.LOGGER.error("OptiFine reflection failed", e);
        }

        return Minecraft.getInstance().getMainRenderTarget().frameBufferId;
    }

    @Override
    public boolean renderingShaderPass() {
        try {
            if (shaderPackLoadedField == null) {
                shaderPackLoadedField = CullingStateManager.OptiFine.getDeclaredField("shaderPackLoaded");
                shaderPackLoadedField.setAccessible(true);
            }
            return (Boolean) shaderPackLoadedField.get(null);
        } catch (Exception e) {
            return false;
        }
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
            if (glState != null) {
                if (activeFramebufferField == null) {
                    activeFramebufferField = glState.getDeclaredField("activeFramebuffer");
                    activeFramebufferField.setAccessible(true);
                }
                Object buffer = activeFramebufferField.get(null);

                if (buffer != null) {
                    if (stateGlFramebufferField == null) {
                        stateGlFramebufferField = buffer.getClass().getDeclaredField("glFramebuffer");
                        stateGlFramebufferField.setAccessible(true);
                    }
                    GlStateManager._glBindFramebuffer(36160, (int) stateGlFramebufferField.get(buffer));
                    GlStateManager._viewport(0, 0, Minecraft.getInstance().getMainRenderTarget().viewWidth, Minecraft.getInstance().getMainRenderTarget().viewHeight);
                    return;
                }
            }
        } catch (Exception e) {
            CullingStateManager.LOGGER.error("OptiFine bind failed", e);
        }
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }
}