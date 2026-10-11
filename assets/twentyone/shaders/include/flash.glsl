#ifndef _g
#define _g
#define _h 200
#define _i 6
#define _j 190
#define _k vec3(0.92,0.95,1.0)
#define _l 9.0
int Aa(int Ba){uint aa=uint(Ba)*0x27d4eb2du;aa^=aa>>15u;aa*=0x85ebca6bu;aa^=aa>>13u;return int(aa&0x7fffffffu);}float Ca(float va){float Da=va*24000.0;int Ea=int(floor(Da/float(_h)));int aa=Aa(Ea);if(aa%100>=_i)return 0.0;float U=(Da-float(Ea*_h+(aa/100)%_j))/20.0;if(U<0.0||U>2.0)return 0.0;int Fa=1+Aa(Ea*7+1)%4;float Ga=0.0,Ha=0.0;for(int ua=0;ua<4;ua++){if(ua>=Fa)break;int xa=Aa(Ea*13+ua*5+3);float Ia=ua==0?1.0:0.45+float(xa%55)/100.0;float Ja=10.0+float((xa/55)%14);Ga=max(Ga,Ia*exp(-pow((U-Ha)*Ja,2.0)));Ha+=0.08+float((xa/800)%30)/100.0;}Ga=max(Ga,0.15*exp(-max(U-Ha,0.0)*3.0)*step(0.0,U));return Ga;}
#endif
