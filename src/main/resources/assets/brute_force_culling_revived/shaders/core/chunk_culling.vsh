#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;

uniform float CullingFrustum[24];
uniform float DepthSize[10];
uniform float RenderDistance;

flat out int spacePartitionSize;
flat out vec4 frustum[6];
flat out vec2 DepthScreenSize[5];

void main() {
    spacePartitionSize = 2 * int(RenderDistance) + 1;

    frustum[0] = vec4(CullingFrustum[0], CullingFrustum[1], CullingFrustum[2], CullingFrustum[3]);
    frustum[1] = vec4(CullingFrustum[4], CullingFrustum[5], CullingFrustum[6], CullingFrustum[7]);
    frustum[2] = vec4(CullingFrustum[8], CullingFrustum[9], CullingFrustum[10], CullingFrustum[11]);
    frustum[3] = vec4(CullingFrustum[12], CullingFrustum[13], CullingFrustum[14], CullingFrustum[15]);
    frustum[4] = vec4(CullingFrustum[16], CullingFrustum[17], CullingFrustum[18], CullingFrustum[19]);
    frustum[5] = vec4(CullingFrustum[20], CullingFrustum[21], CullingFrustum[22], CullingFrustum[23]);

    DepthScreenSize[0] = vec2(DepthSize[0], DepthSize[1]);
    DepthScreenSize[1] = vec2(DepthSize[2], DepthSize[3]);
    DepthScreenSize[2] = vec2(DepthSize[4], DepthSize[5]);
    DepthScreenSize[3] = vec2(DepthSize[6], DepthSize[7]);
    DepthScreenSize[4] = vec2(DepthSize[8], DepthSize[9]);

    gl_Position = vec4(Position, 1.0);
}