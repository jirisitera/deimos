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
  vec3 F = texelFetch(
    InSampler,
    ivec2(Sa * vec2(textureSize(InSampler, 0))),
    0
  ).rgb;
  ivec3 D = ivec3(round(F * 255.0));
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
