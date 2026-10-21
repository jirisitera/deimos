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
// Camera buttons on the right monitor: 2x2 matrix, measured in monitor-height units so they stay square.
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
bool glyphPixel(int glyph, vec2 uv) {
  // 3x5 bitmap glyphs for 0..9 and % (index 10), each row is 3 bits (left = MSB)
  const int glyphs[55] = int[](
    7, 5, 5, 5, 7, // 0
    2, 6, 2, 2, 7, // 1
    7, 1, 7, 4, 7, // 2
    7, 1, 7, 1, 7, // 3
    5, 5, 7, 1, 1, // 4
    7, 4, 7, 1, 7, // 5
    7, 4, 7, 5, 7, // 6
    7, 1, 1, 1, 1, // 7
    7, 5, 7, 5, 7, // 8
    7, 5, 7, 1, 7, // 9
    5, 1, 2, 4, 5  // %
  );
  if (any(lessThan(uv, vec2(0.0))) || any(greaterThanEqual(uv, vec2(1.0)))) return false;
  ivec2 c = ivec2(uv * vec2(3.0, 5.0));
  return ((glyphs[glyph * 5 + c.y] >> (2 - c.x)) & 1) == 1;
}
vec3 renderButton(int index, vec2 local, bool hovered) {
  const float bevel = 0.07;
  vec3 col = hovered ? vec3(0.45, 0.5, 0.45) : vec3(0.22);
  if (local.x < bevel || local.y < bevel) {
    col = hovered ? vec3(0.75, 0.8, 0.75) : vec3(0.5);
  } else if (local.x > 1.0 - bevel || local.y > 1.0 - bevel) {
    col = vec3(0.08);
  }
  vec2 digitUv = (local - vec2(0.32, 0.25)) / vec2(0.36, 0.5);
  if (glyphPixel(index + 1, digitUv)) {
    col = hovered ? vec3(1.0) : vec3(0.75);
  }
  return col;
}
vec3 renderEnergyMonitor(int rowY, vec2 uv) {
  vec3 col = vec3(0.0);
  ivec3 posPixel = ivec3(round(texelFetch(InSampler, ivec2(8, rowY), 0).rgb * 255.0));
  int percent = 100;
  int usage = 2;
  int maxUsage = 5;
  if (posPixel != ivec3(255, 255, 255)) {
    ivec3 data = ivec3(round(texelFetch(InSampler, ivec2(9, rowY), 0).rgb * 255.0));
    percent = clamp(data.r, 0, 100);
    usage = clamp(data.g, 0, 16);
    maxUsage = max(1, data.b);
  }

  // 1. Percentage readout in the upper half (e.g. "100%", "85%", "7%")
  int chars[4];
  int charCount = 0;
  if (percent >= 100) {
    chars[0] = 1;
    chars[1] = 0;
    chars[2] = 0;
    chars[3] = 10;
    charCount = 4;
  } else if (percent >= 10) {
    chars[0] = percent / 10;
    chars[1] = percent - chars[0] * 10;
    chars[2] = 10;
    charCount = 3;
  } else {
    chars[0] = percent;
    chars[1] = 10;
    charCount = 2;
  }
  const float charW = 0.13;
  const float charH = 0.28;
  const float charGap = 0.035;
  float totalW = float(charCount) * charW + float(charCount - 1) * charGap;
  float startX = 0.5 - totalW * 0.5;
  float startY = 0.16;
  if (uv.y >= startY && uv.y < startY + charH && uv.x >= startX && uv.x < startX + totalW) {
    float relX = uv.x - startX;
    float stride = charW + charGap;
    int idx = int(floor(relX / stride));
    float localX = (relX - float(idx) * stride) / charW;
    float localY = (uv.y - startY) / charH;
    if (idx >= 0 && idx < charCount && localX < 1.0) {
      if (glyphPixel(chars[idx], vec2(localX, localY))) {
        col = percent <= 20 ? vec3(0.95, 0.25, 0.25) : vec3(0.85, 0.92, 0.85);
      }
    }
  }

  // 2. Segmented energy usage bar in the lower half
  vec2 barMin = vec2(0.12, 0.56);
  vec2 barMax = vec2(0.86, 0.84);
  vec2 border = vec2(0.02, 0.025);
  // Battery terminal nub on the right
  if (uv.x >= barMax.x && uv.x <= barMax.x + 0.035 && uv.y >= 0.64 && uv.y <= 0.76) {
    col = vec3(0.45);
  }
  if (all(greaterThanEqual(uv, barMin)) && all(lessThanEqual(uv, barMax))) {
    bool isBorder =
      uv.x < barMin.x + border.x || uv.x > barMax.x - border.x ||
      uv.y < barMin.y + border.y || uv.y > barMax.y - border.y;
    if (isBorder) {
      col = vec3(0.45);
    } else {
      vec2 innerMin = barMin + border * 1.8;
      vec2 innerMax = barMax - border * 1.8;
      if (all(greaterThanEqual(uv, innerMin)) && all(lessThanEqual(uv, innerMax))) {
        float normX = (uv.x - innerMin.x) / (innerMax.x - innerMin.x);
        float segCoord = normX * float(maxUsage);
        int seg = clamp(int(floor(segCoord)), 0, maxUsage - 1);
        float segFrac = fract(segCoord);
        // Leave a gap between adjacent segments
        if (segFrac >= 0.08 && segFrac <= 0.92) {
          if (seg < usage) {
            float t = maxUsage > 1 ? float(seg) / float(maxUsage - 1) : 0.0;
            vec3 lowCol = vec3(0.2, 0.88, 0.32);
            vec3 midCol = vec3(0.95, 0.82, 0.18);
            vec3 highCol = vec3(0.95, 0.22, 0.2);
            vec3 segCol = t < 0.5 ? mix(lowCol, midCol, t * 2.0) : mix(midCol, highCol, (t - 0.5) * 2.0);
            float localY = (uv.y - innerMin.y) / (innerMax.y - innerMin.y);
            if (localY < 0.18) {
              segCol = mix(segCol, vec3(1.0), 0.35);
            }
            col = segCol;
          } else {
            col = vec3(0.08);
          }
        }
      }
    }
  }
  return col;
}
vec3 renderMonitor(int m, vec2 monitorUv) {
  int rowY = _e - m;
  vec3 screenCol = vec3(0.0);
  if (monitorActive(m)) {
    vec2 aspect = vec2(11.0 / 8.0, 1.0);
    if (m == 1) {
      screenCol = renderEnergyMonitor(rowY, monitorUv);
    } else if (m == 2) {
      vec2 local;
      int button = buttonAt(monitorUv, local);
      if (button >= 0) {
        int hovered = -1;
        if (gHasLook) {
          float virtualX = clamp((gLookAngles.x + 54.0) / 108.0 * 3.0, 0.0, 2.9999);
          if (int(floor(virtualX)) == 2) {
            vec2 unused;
            hovered = buttonAt(
              vec2(fract(virtualX), clamp((gLookAngles.y + 25.0) / 50.0, 0.0, 1.0)),
              unused
            );
          }
        }
        screenCol = renderButton(button, local, hovered == button);
      }
    } else {
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
