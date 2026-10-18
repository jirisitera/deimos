#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
uniform sampler2D InSampler;
uniform sampler2D CursorSampler;
uniform sampler2D BloomSampler;
uniform sampler2D AoSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
#define _jd 16
#define _e 19
#define _kd 1.0
#define _ld 1.05
#define _md 0.9
#define _nd 3.0
#define _od 16.0
#define _pd 0.35
#define _qd 0.07
#define _rd 1.0
#define _sd 0.2
#define _td vec3(0.95, 1.03, 0.9)
#define _ud 0.15
vec2 gLookAngles = vec2(0.0);
bool gHasLook = false;
bool monitorActive(int m) {
  int rowY = _e - m;
  return ivec3(round(texelFetch(InSampler, ivec2(40, rowY), 0).rgb * 255.0)) ==
    ivec3(91, 126, 209) &&
    ivec3(round(texelFetch(InSampler, ivec2(41, rowY), 0).rgb * 255.0)) ==
    ivec3(91, 126, 209);
}
vec2 decodeAngles(ivec2 p0, ivec2 p1, float pitchRange) {
  ivec3 X = ivec3(round(texelFetch(InSampler, p0, 0).rgb * 255.0));
  ivec3 Va = ivec3(round(texelFetch(InSampler, p1, 0).rgb * 255.0));
  return vec2(
    float(X.r * 256 + X.g) / 65535.0 * 360.0 - 180.0,
    float(X.b * 256 + Va.r) / 65535.0 * pitchRange - pitchRange * 0.5
  );
}
bool vg(out vec2 wg) {
  wg = vec2(0.0);
  ivec2 p0 = ivec2(0);
  ivec2 p1 = ivec2(0);
  bool found = false;
  for (int m = 0; m < 3; m++) {
    if (monitorActive(m)) {
      int rowY = _e - m;
      p0 = ivec2(42, rowY);
      p1 = ivec2(43, rowY);
      found = true;
      break;
    }
  }
  if (!found) {
    return false;
  }
  vec2 bias = decodeAngles(p0, p1, 360.0);
  if (
    ivec3(round(texelFetch(InSampler, ivec2(28, _jd), 0).rgb * 255.0)) ==
    ivec3(91, 126, 209)
  ) {
    vec2 rawLook = decodeAngles(ivec2(26, _jd), ivec2(27, _jd), 180.0);
    wg = vec2(
      mod(mod(rawLook.x + bias.x + 180.0, 360.0) + 360.0, 360.0) - 180.0,
      rawLook.y + bias.y
    );
  } else {
    wg = bias;
  }
  return true;
}
float xg(vec2 Ya) {
  const int hd[16] = int[](
    0,
    8,
    2,
    10,
    12,
    4,
    14,
    6,
    3,
    11,
    1,
    9,
    15,
    7,
    13,
    5
  );
  ivec2 ua = ivec2(mod(Ya, 4.0));
  return (float(hd[ua.y * 4 + ua.x]) + 0.5) / 16.0;
}
// Camera buttons on the center monitor: 2x2 matrix, measured in monitor-height units so they stay square.
// Must match MapProbe.hoveredButton on the server.
#define BTN_HALF 0.18
#define BTN_CENTER 0.22
int buttonAt(vec2 uv, out vec2 local) {
  vec2 p = (uv - 0.5) * vec2(11.0 / 8.0, 1.0);
  vec2 d = abs(p) - vec2(BTN_CENTER);
  local = (d * sign(p)) / (BTN_HALF * 2.0) + 0.5;
  if (max(abs(d.x), abs(d.y)) > BTN_HALF) return -1;
  return (p.x > 0.0 ? 1 : 0) + (p.y > 0.0 ? 2 : 0);
}
bool digitPixel(int digit, vec2 local) {
  // 3x5 bitmap glyphs, each row is 3 bits (left = MSB)
  const int glyphs[20] = int[](
    2, 6, 2, 2, 7,
    7, 1, 7, 4, 7,
    7, 1, 7, 1, 7,
    5, 5, 7, 1, 1
  );
  vec2 g = (local - vec2(0.32, 0.25)) / vec2(0.36, 0.5) * vec2(3.0, 5.0);
  if (any(lessThan(g, vec2(0.0))) || any(greaterThanEqual(g, vec2(3.0, 5.0)))) return false;
  ivec2 c = ivec2(g);
  return ((glyphs[digit * 5 + c.y] >> (2 - c.x)) & 1) == 1;
}
vec3 renderButton(int index, vec2 local, bool hovered) {
  const float bevel = 0.07;
  vec3 col = hovered ? vec3(0.45, 0.5, 0.45) : vec3(0.22);
  if (local.x < bevel || local.y < bevel) {
    col = hovered ? vec3(0.75, 0.8, 0.75) : vec3(0.5);
  } else if (local.x > 1.0 - bevel || local.y > 1.0 - bevel) {
    col = vec3(0.08);
  }
  if (digitPixel(index, local)) {
    col = hovered ? vec3(1.0) : vec3(0.75);
  }
  return col;
}
vec3 renderMonitor(int m, vec2 monitorUv) {
  int rowY = _e - m;
  vec3 screenCol = vec3(0.0);
  if (monitorActive(m)) {
    vec2 aspect = vec2(11.0 / 8.0, 1.0);
    if (m == 1) {
      vec2 local;
      int button = buttonAt(monitorUv, local);
      if (button >= 0) {
        int hovered = -1;
        if (gHasLook) {
          float virtualX = clamp((gLookAngles.x + 54.0) / 108.0 * 3.0, 0.0, 2.9999);
          if (int(floor(virtualX)) == 1) {
            vec2 unused;
            hovered = buttonAt(
              vec2(fract(virtualX), clamp((gLookAngles.y + 25.0) / 50.0, 0.0, 1.0)),
              unused
            );
          }
        }
        screenCol = renderButton(button, local, hovered == button);
      }
    }
    float dotRadius = 0.04;
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
  }
  if (gHasLook) {
    float virtualX = clamp((gLookAngles.x + 54.0) / 108.0 * 3.0, 0.0, 2.9999);
    int cursorMonitor = int(floor(virtualX));
    if (cursorMonitor == m) {
      vec2 cursorLocal = vec2(
        fract(virtualX),
        clamp((gLookAngles.y + 25.0) / 50.0, 0.0, 1.0)
      );
      vec2 cursorSize = vec2(0.18 * 8.0 / 11.0, 0.18);
      vec2 cursorTexel = floor(
        (monitorUv - cursorLocal) / cursorSize * 32.0
      );
      if (
        all(greaterThanEqual(cursorTexel, vec2(0.0))) &&
        all(lessThan(cursorTexel, vec2(32.0)))
      ) {
        vec4 F = texelFetch(CursorSampler, ivec2(cursorTexel), 0);
        screenCol = mix(screenCol, F.rgb, F.a);
      }
    }
  }
  return screenCol;
}
vec3 yg(vec2 Sa) {
  vec2 ga = vec2(textureSize(InSampler, 0));
  ivec2 na = clamp(ivec2(Sa * ga), ivec2(0), ivec2(ga) - 1);
  if (na.y >= _jd - 2 && na.y <= _e + 2 && na.x < 48) {
    Sa.y += 9.0 / ga.y;
    na = clamp(ivec2(Sa * ga), ivec2(0), ivec2(ga) - 1);
  }
  ivec3 raw = ivec3(round(texelFetch(InSampler, na, 0).rgb * 255.0));
  if (raw.r >= 250 && raw.r <= 252) {
    return renderMonitor(raw.r - 250, vec2(raw.gb) / 255.0);
  }
  return texture(InSampler, Sa).rgb;
}
float kg(vec2 Sa) {
  vec2 tg = 1.0 / vec2(textureSize(AoSampler, 0));
  float og = 0.0;
  for (int la = -1; la <= 1; la++)
    for (int bd = -1; bd <= 1; bd++)
      og += texture(AoSampler, Sa + vec2(bd, la) * tg * 1.5).r;
  return og / 9.0;
}
vec3 zg(vec3 F, float fd) {
  float Qb = dot(F, vec3(0.2126, 0.7152, 0.0722));
  return mix(vec3(Qb), F, fd);
}
void main() {
  vec2 ga = vec2(textureSize(InSampler, 0));
  vec2 na = 1.0 / ga;
  float U = GameTime * 1200.0;
  gHasLook = vg(gLookAngles);
  vec2 Wc = na * _nd;
  vec2 Sa = floor(texCoord / Wc) * Wc + Wc * 0.5;
  vec2 cc = Sa - 0.5;
  float mg = length(cc);
  float Ag = _qd * mg * mg;
  vec3 Nd =
    vec3(yg(Sa + cc * Ag).r, yg(Sa).g, yg(Sa - cc * Ag).b) * kg(Sa) +
    texture(BloomSampler, Sa).rgb;
  Nd *= _kd;
  Nd = mix(Nd, Nd * _td, _ud);
  float Va = xg(floor(Sa / Wc));
  Nd = floor(Nd * _od + Va * _pd) / (_od - 1.0);
  Nd = (Nd - 0.5) * _ld + 0.5;
  Nd = clamp(zg(Nd, _md), 0.0, 1.0);
  Nd *= clamp(1.0 - smoothstep(0.5, 0.9, mg) * _rd, 0.0, 1.0);
  float ab = fract(
    sin(dot(floor(Sa / Wc), vec2(12.9898, 78.233)) + U * 37.0) * 43758.5453
  );
  Nd += (ab - 0.5) * 0.08 * _sd;
  fragColor = vec4(clamp(Nd, 0.0, 1.0), 1.0);
}
