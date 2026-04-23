#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    float gray = (color.r + color.g + color.b) / 3.0;
    vec3 grayscale = vec3(gray);
    vec3 mixedColor = mix(grayscale, color.rgb, clamp(vertexColor.a, 0.0, 1.0));
    float modAlpha = clamp(ColorModulator.a, 0.0, 1.0);
    color.rgb = (vec3(1.0) - mixedColor + modAlpha) * (1.0 - modAlpha);
    color.rgb = clamp(color.rgb, 0.0, 1.0);

    fragColor = color;
}