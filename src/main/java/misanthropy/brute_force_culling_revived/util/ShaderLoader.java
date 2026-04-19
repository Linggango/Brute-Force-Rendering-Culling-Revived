package misanthropy.brute_force_culling_revived.util;

public interface ShaderLoader {
    int getFrameBufferID();

    boolean renderingShaderPass();

    boolean enabledShader();

    void bindDefaultFrameBuffer();
}
