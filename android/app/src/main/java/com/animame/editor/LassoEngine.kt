package com.animame.editor

import android.graphics.PointF
import kotlin.math.abs

object LassoEngine {
 fun normalize(points:List<PointF>,close:Boolean=true):List<PointF>{if(points.isEmpty())return emptyList();val out=ArrayList<PointF>(points.size+1);for(p in points){if(out.isEmpty()||abs(p.x-out.last().x)>.5f||abs(p.y-out.last().y)>.5f)out+=PointF(p.x,p.y)};if(close&&out.size>2){val f=out.first();val e=out.last();if(abs(f.x-e.x)>.5f||abs(f.y-e.y)>.5f)out+=PointF(f.x,f.y)};return out}
 fun contains(points:List<PointF>,point:PointF):Boolean{val p=normalize(points,false);if(p.size<3)return false;var winding=0;for(i in p.indices){val a=p[i];val b=p[(i+1)%p.size];if(a.y<=point.y){if(b.y>point.y&&cross(a,b,point)>0)winding++}else if(b.y<=point.y&&cross(a,b,point)<0)winding--};return winding!=0}
 private fun cross(a:PointF,b:PointF,p:PointF):Float=(b.x-a.x)*(p.y-a.y)-(p.x-a.x)*(b.y-a.y)
 fun bounds(points:List<PointF>):FloatArray{if(points.isEmpty())return floatArrayOf(0f,0f,0f,0f);var l=points[0].x;var t=points[0].y;var r=l;var b=t;for(p in points.drop(1)){l=minOf(l,p.x);t=minOf(t,p.y);r=maxOf(r,p.x);b=maxOf(b,p.y)};return floatArrayOf(l,t,r,b)}
}
