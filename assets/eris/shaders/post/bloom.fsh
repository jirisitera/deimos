#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform BloomConfig {
  vec2 Step;
  float Bright;
};
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
vec3 pg(vec2 Sa) {
  if (Bright < 0.5) return texture(InSampler, Sa).rgb;
  ivec2 na = ivec2(Sa * vec2(textureSize(InSampler, 0)));
  if (na.y >= 14 && na.y <= 21 && na.x < 48) return vec3(0.0);
  vec3 F = texelFetch(InSampler, na, 0).rgb;
  ivec3 D = ivec3(round(F * 255.0));
  if (D.r >= 250 && D.r <= 252) {
    int m = D.r - 250;
    int rowY = 19 - m;
    if (
      ivec3(round(texelFetch(InSampler, ivec2(40, rowY), 0).rgb * 255.0)) !=
      ivec3(91, 126, 209) ||
      ivec3(round(texelFetch(InSampler, ivec2(41, rowY), 0).rgb * 255.0)) !=
      ivec3(91, 126, 209)
    )
      return vec3(0.0);
    vec2 monitorUv = vec2(D.gb) / 255.0;
    vec2 aspect = vec2(11.0 / 8.0, 1.0);
    float dotRadius = 0.04;
    vec3 screenCol = vec3(0.0);
    for (int i = 0; i < 16; i++) {
      ivec3 posPixel = ivec3(
        round(texelFetch(InSampler, ivec2(8 + i * 2, rowY), 0).rgb * 255.0)
      );
      if (posPixel == ivec3(255, 255, 255)) continue;
      int rawX = (posPixel.r << 4) | (posPixel.g >> 4);
      int rawY = ((posPixel.g & 15) << 8) | posPixel.b;
      vec2 dotPos = vec2(float(rawX), float(rawY)) / 4094.0;
      float dist = length((monitorUv - dotPos) * aspect);
      if (dist <= dotRadius) {
        vec3 dotColor = texelFetch(
          InSampler,
          ivec2(8 + i * 2 + 1, rowY),
          0
        ).rgb;
        float alpha = 1.0 - smoothstep(dotRadius * 0.65, dotRadius, dist);
        screenCol = mix(screenCol, dotColor, alpha);
      }
    }
    return screenCol;
  }
  bool qg =
    (D.r & 1) == 1 &&
    (D.g & 1) == 0 &&
    (D.b & 1) == 1 &&
    max(F.r, max(F.g, F.b)) > 0.2;
  float rg = max(F.r, max(F.g, F.b)),
    sg = min(F.r, min(F.g, F.b));
  bool Jd = rg > 0.55 && rg - sg < 0.2 * rg;
  return qg || Jd
    ? F
    : vec3(0.0);
}
void main() {
  vec2 tg = Step / vec2(textureSize(InSampler, 0));
  vec3 ug = pg(texCoord) * 0.0798;
  float total = 0.0798;
  for (int ua = 1; ua <= 12; ua++) {
    float Ba = exp(-float(ua * ua) / 32.0);
    ug += (pg(texCoord + tg * float(ua)) + pg(texCoord - tg * float(ua))) * Ba;
    total += 2.0 * Ba;
  }
  fragColor = vec4(ug / total * 0.45, 1.0);
}
