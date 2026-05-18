package misanthropy.brute_force_culling_revived.mixin;

import com.hollingsworth.arsnouveau.client.renderer.tile.SkyBlockRenderer;
import com.hollingsworth.arsnouveau.common.block.tile.SkyBlockTile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SkyBlockRenderer.class)
public class SkyBlockRendererMixin {
    @Redirect(
            method = "render(Lcom/hollingsworth/arsnouveau/common/block/tile/SkyBlockTile;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/client/renderer/tile/SkyBlockRenderer;renderCube(Lcom/hollingsworth/arsnouveau/common/block/tile/SkyBlockTile;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
            ),
            remap = false
    )
    private void bruteForceCulling$drawWhiteCube(
            SkyBlockRenderer instance,
            SkyBlockTile tileEntityIn,
            Matrix4f p_228883_4_,
            VertexConsumer p_228883_5_,
            SkyBlockTile pTileEntityIn,
            float partialTicks,
            PoseStack pPoseStack,
            MultiBufferSource bufferIn,
            int combinedLightIn,
            int combinedOverlayIn
    ) {
        VertexConsumer buffer = bufferIn.getBuffer(RenderType.lightning());
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 0.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 0.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 1.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 1.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 1.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 1.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 0.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 0.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 1.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 1.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 0.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 0.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 0.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 0.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 1.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 1.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 0.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 0.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 0.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 0.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 1.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 1.0f, 1.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 1.0f, 1.0f, 0.0f);
        brute_Force_Rendering_Culling_Revived$vertex(buffer, p_228883_4_, 0.0f, 1.0f, 0.0f);
    }

    @Unique
    private static void brute_Force_Rendering_Culling_Revived$vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z) {
        buffer.vertex(matrix, x, y, z).color(255, 255, 255, 255).endVertex();
    }
}