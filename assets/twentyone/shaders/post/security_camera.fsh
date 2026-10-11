#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

#include <minecraft:globals.glsl>

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float noise(vec2 position) {
    return fract(sin(dot(position, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 centered = texCoord - 0.5;
    float radius = dot(centered, centered);
    vec2 warped = centered * (1.0 + radius * 0.10) + 0.5;
    vec3 color = texture(InSampler, clamp(warped, 0.0, 1.0)).rgb;

    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 green = vec3(luminance * 0.72, luminance, luminance * 0.58);

    vec2 resolution = vec2(textureSize(InSampler, 0));
    float scanline = 0.90 + 0.10 * sin(warped.y * resolution.y * 3.14159265);
    float rollingBand = 0.97 + 0.03 * sin(GameTime * 900.0 + warped.y * 20.0);
    float vignette = 1.0 - smoothstep(0.28, 0.78, radius);
    float grain = (noise(gl_FragCoord.xy + GameTime * 37.0) - 0.5) * 0.08;

    green = green * scanline * rollingBand * (0.72 + vignette * 0.28);
    green += vec3(grain);

    fragColor = vec4(clamp(green, 0.0, 1.0), 1.0);
}
