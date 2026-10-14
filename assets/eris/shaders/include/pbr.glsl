#ifndef _t
#define _t
#include <eris:pbr_scene.glsl>
const float ib = 3.14159265;
vec3 jb() {
  float kb = -ModelViewMat[2][2];
  return kb > 0.0
    ? _v
    : _w;
}
vec3 lb(vec3 F) {
  return pow(F, vec3(2.2));
}
vec3 mb(vec3 F) {
  return pow(clamp(F, 0.0, 1.0), vec3(1.0 / 2.2));
}
mat3 nb(vec3 ab, vec3 Ya, vec2 Sa) {
  vec3 ob = dFdx(Ya),
    pb = dFdy(Ya);
  vec2 qb = dFdx(Sa),
    rb = dFdy(Sa);
  vec3 sb = cross(pb, ab),
    tb = cross(ab, ob);
  vec3 U = sb * qb.x + tb * rb.x;
  vec3 Va = sb * qb.y + tb * rb.y;
  float ub = inversesqrt(max(max(dot(U, U), dot(Va, Va)), 1e-20));
  return mat3(U * ub, Va * ub, ab);
}
float vb = 0.0,
  wb = 0.0;
vec3 xb = vec3(0.0, 1.0, 0.0);
float yb(float zb, float X) {
  float Ab = X * X;
  float Wa = zb * zb * (Ab - 1.0) + 1.0;
  return Ab / (ib * Wa * Wa);
}
float Bb(float Cb, float Db, float Eb) {
  float xa = (Eb + 1.0) * (Eb + 1.0) / 8.0;
  return Cb / (Cb * (1.0 - xa) + xa) * Db / (Db * (1.0 - xa) + xa);
}
vec3 Fb(float F, vec3 Gb) {
  return Gb + (1.0 - Gb) * pow(1.0 - F, 5.0);
}
vec3 Hb(vec3 Wa, float Eb) {
  float Ib = smoothstep(mix(0.97, 0.6, Eb), 1.0, Wa.y);
  return vec3(0.025, 0.022, 0.018) * Ib * (1.0 - Eb);
}
vec3 Jb(
  vec3 ab,
  vec3 sa,
  vec3 Ya,
  vec3 Kb,
  vec3 Lb,
  vec3 Mb,
  float Eb,
  float Nb,
  vec3 Gb
) {
  vec3 Ob = Kb - Ya;
  float Pb = dot(Ob, Ob);
  vec3 Qb = Ob * inversesqrt(Pb);
  vec3 aa = normalize(Qb + sa);
  float Db = max(dot(ab, Qb), 0.0),
    Cb = max(dot(ab, sa), 1e-4),
    zb = max(dot(ab, aa), 0.0),
    Rb = max(dot(sa, aa), 0.0);
  vec3 Sb = Fb(Rb, Gb);
  vec3 Tb = yb(zb, Eb * Eb) * Bb(Cb, Db, Eb) * Sb / (4.0 * Cb * max(Db, 1e-4));
  vec3 Ub = (1.0 - Sb) * (1.0 - Nb);
  float Vb = max((dot(ab, Qb) + vb) / (1.0 + vb), 0.0);
  vec3 Wb = Ub * Mb / ib * Vb;
  float Xb = smoothstep(-0.2, 0.5, dot(-sa, Qb));
  float Yb = max(dot(xb, sa), 1e-4);
  float Zb = pow(1.0 - Yb, 2.2) * Xb * smoothstep(-0.3, 0.2, dot(xb, Qb) + 0.3);
  vec3 ac = mix(Mb, vec3(1.0), 0.5) * wb * Zb;
  return (Wb + Tb * Db + ac) * Lb / max(Pb, 0.06);
}
vec3 bc(
  vec3 ab,
  vec3 sa,
  vec3 Ya,
  vec3 Kb,
  vec3 cc,
  vec2 ec,
  vec3 Lb,
  vec3 Mb,
  float Eb,
  float Nb,
  vec3 Gb
) {
  float xa = smoothstep(ec.x, ec.y, dot(normalize(Ya - Kb), cc));
  return xa <= 0.0
    ? vec3(0.0)
    : Jb(ab, sa, Ya, Kb, Lb * xa, Mb, Eb, Nb, Gb);
}
float fc(float va) {
  float U = va * 1200.0;
  float gc = 0.97 + 0.03 * sin(U * 23.0) * sin(U * 7.3);
  float xa = floor(U * 8.0);
  float hc = fract(sin(xa * 91.17) * 43758.5453) > 0.96 ? 0.82 : 1.0;
  return gc * hc;
}
vec3 jc(vec3 Mb, vec3 ab, float Eb, float Nb, float kc, vec3 Qa) {
  vec3 sa = normalize(-Qa);
  vec3 Gb = mix(vec3(0.04), Mb, Nb);
  vec3 mc = jb();
  vec3 A = vec3(0.0);
  for (int ua = 0; ua < _x; ua++) {
    float xa = ua == 0 ? fc(GameTime) : 1.0;
    A += bc(
      ab,
      sa,
      Qa,
      Fc[ua] - mc,
      Gc[ua],
      Ic[ua],
      Hc[ua] * xa,
      Mb,
      Eb,
      Nb,
      Gb
    );
  }
  float Cb = max(dot(ab, sa), 0.0);
  vec3 uc = Gb + (max(vec3(1.0 - Eb), Gb) - Gb) * pow(1.0 - Cb, 5.0);
  vec3 vc =
    Hb(ab, 1.0) * Mb * (1.0 - Nb) * (1.0 - uc) + Hb(reflect(-sa, ab), Eb) * uc;
  A += vc * kc;
  A = A / (1.0 + A);
  A = pow(A, vec3(1.35));
  return mb(A);
}
vec3 wc(sampler2D La, vec2 Sa, float xc, vec3 Qa, vec3 Pa) {
  vec2 yc = xc / vec2(textureSize(La, 0));
  vec3 Mb = lb(texture(La, Sa).rgb);
  vec3 zc = texture(La, Sa + vec2(yc.x, 0.0)).rgb * 2.0 - 1.0;
  vec3 Ac = texture(La, Sa + vec2(0.0, yc.y)).rgb;
  vec3 ab = normalize(nb(Pa, Qa, Sa) * zc);
  return jc(Mb, ab, clamp(Ac.r, 0.05, 1.0), Ac.g, mix(0.5, 1.0, Ac.b), Qa);
}
vec3 Bc(sampler2D La, vec2 Sa, vec3 Qa, vec3 Pa, vec2 Cc) {
  vec3 Mb = lb(texture(La, Sa).rgb);
  vec2 Sb = Cc * 2.0 - 1.0;
  vec2 Dc = sign(Sb) * smoothstep(0.5, 1.0, abs(Sb)) * 0.36;
  vec3 ab = normalize(nb(Pa, Qa, Cc) * normalize(vec3(Dc, 1.0)));
  xb = Pa;
  vb = 0.45;
  wb = 4.0;
  vec3 F = jc(Mb, ab, 0.75, 0.0, 1.0, Qa);
  vb = 0.0;
  wb = 0.0;
  xb = vec3(0.0, 1.0, 0.0);
  return F;
}
vec3 Ec(sampler2D La, vec2 Sa, vec3 Qa, vec3 Pa) {
  vec3 Mb = lb(texture(La, Sa).rgb);
  vec3 ab = dot(Pa, Qa) > 0.0 ? -Pa : Pa;
  return jc(Mb, ab, 0.9, 0.0, 1.0, Qa);
}
#endif
