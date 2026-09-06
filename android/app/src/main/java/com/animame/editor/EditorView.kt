package com.animame.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class EditorView(context: Context) : View(context) {
    private enum class Tool { BRUSH, PENCIL, ERASER, LASSO, FILL, PICKER, MOVE, TRANSFORM, LINE, RECTANGLE, ELLIPSE }
    private enum class Panel { OPTIONS, BRUSHES, LAYERS, TIMELINE, CAMERA, RULERS }
    private enum class Ruler { NONE, STRAIGHT, CIRCULAR, ELLIPSE, RADIAL, MIRROR, KALEIDOSCOPE, ROTATION, ARRAY, PERSPECTIVE_ARRAY }

    private var tool=Tool.BRUSH
    private var panel:Panel?=Panel.OPTIONS
    private var ruler=Ruler.NONE
    private var perspective=0
    private var perspectiveSnap=true
    private var mirrorAngle=0f
    private var radialCenter=PointF(0f,0f)
    private var symmetryCount=6
    private var stabilizer=55f
    private var realTime=true
    private var brushSize=12f
    private var opacity=1f
    private var flow=1f
    private var spacing=.12f
    private var antialias=true
    private var pressure=true
    private var gapRecognition=true
    private var fillExpansion=2
    private var fillStrength=24
    private var selectedBrush=BrushCatalog.presets.first()
    private var fillColor=Color.rgb(25,25,25)
    private var bitmap:Bitmap?=null
    private var bcanvas:Canvas?=null
    private val points=mutableListOf<PointF>()
    private var lasso=Path()
    private val ui=Paint(Paint.ANTI_ALIAS_FLAG)
    private val text=Paint(Paint.ANTI_ALIAS_FLAG)

    init { setLayerType(View.LAYER_TYPE_SOFTWARE,null); text.typeface=Typeface.create("sans",Typeface.NORMAL) }
    override fun onSizeChanged(w:Int,h:Int,ow:Int,oh:Int) { if(w>0&&h>0){bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bitmap!!.eraseColor(Color.WHITE);bcanvas=Canvas(bitmap!!)} }
    private fun rect():RectF { val right=if(panel==null)62f else 300f; return RectF(72f,60f,width-right-10f,height-122f) }

    override fun onDraw(c:Canvas) {
        val w=width.toFloat(); val h=height.toFloat()
        ui.style=Paint.Style.FILL;ui.color=Color.rgb(16,17,19);c.drawRect(0f,0f,w,h,ui)
        ui.color=Color.rgb(27,29,32);c.drawRect(0f,0f,w,50f,ui)
        txt(c,"ANIMA-ME",14f,32f,Color.WHITE,15f);txt(c,"Untitled",116f,32f,Color.LTGRAY,12f);txt(c,"24 FPS",w-116f,32f,Color.LTGRAY,12f)
        tabs(c,w);rail(c,h);val r=rect();ui.color=Color.WHITE;c.drawRect(r,ui)
        bitmap?.let{c.save();c.clipRect(r);c.drawBitmap(it,null,r,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG));c.restore()}
        drawRuler(c,r)
        if(tool==Tool.LASSO&&!lasso.isEmpty){ui.style=Paint.Style.STROKE;ui.strokeWidth=2f;ui.color=Color.rgb(40,120,255);c.drawPath(lasso,ui);ui.style=Paint.Style.FILL}
        if(panel!=null)panel(c,w,h);timeline(c,w,h)
    }

    private fun tabs(c:Canvas,w:Float){val ns=listOf("OPTIONS","BRUSHES","LAYERS","TIMELINE","CAMERA","RULERS");var x=190f;ns.forEachIndexed{i,n->if(x<w-55){ui.color=if(panel==Panel.values()[i])Color.rgb(68,73,81)else Color.rgb(43,46,50);c.drawRoundRect(x,9f,x+74f,41f,7f,7f,ui);txt(c,n,x+7f,29f,Color.WHITE,8f);x+=78f}}}
    private fun rail(c:Canvas,h:Float){ui.color=Color.rgb(31,33,36);c.drawRect(0f,50f,62f,h-112f,ui);Tool.values().forEachIndexed{i,t->val y=73f+i*42f;if(y<h-130f){if(t==tool){ui.color=Color.rgb(68,73,81);c.drawRoundRect(5f,y-17f,57f,y+17f,8f,8f,ui)};icon(c,t,31f,y)}}}

    private fun panel(c:Canvas,w:Float,h:Float){val x=w-300f;ui.color=Color.rgb(29,31,34);c.drawRect(x,50f,w,h-112f,ui);val title=when(panel){Panel.OPTIONS->"TOOL OPTIONS";Panel.BRUSHES->"BRUSH LIBRARY";Panel.LAYERS->"LAYERS";Panel.TIMELINE->"TIMELINE";Panel.CAMERA->"CAMERA / TRANSFORM";Panel.RULERS->"RULERS / PERSPECTIVE";null->""};txt(c,title,x+14f,78f,Color.WHITE,13f);txt(c,"TAP HEADER TO CLOSE",w-132f,78f,Color.GRAY,7f);when(panel){Panel.OPTIONS->options(c,x+14f,106f);Panel.BRUSHES->brushes(c,x+14f,106f);Panel.LAYERS->layers(c,x+14f,106f);Panel.TIMELINE->timelinePanel(c,x+14f,106f);Panel.CAMERA->camera(c,x+14f,106f);Panel.RULERS->rulers(c,x+14f,106f);null->Unit}}

    private fun options(c:Canvas,x:Float,y:Float){if(tool==Tool.BRUSH||tool==Tool.PENCIL||tool==Tool.ERASER){row(c,"BRUSH",selectedBrush.name,x,y);row(c,"SIZE","${brushSize.toInt()} px",x,y+45);row(c,"OPACITY","${(opacity*100).toInt()}%",x,y+90);row(c,"FLOW","${(flow*100).toInt()}%",x,y+135);row(c,"SPACING","${(spacing*100).toInt()}%",x,y+180);row(c,"STABILIZER","${stabilizer.toInt()}%",x,y+225);txt(c,"REAL-TIME STABILIZER   ${if(realTime)"ON" else "OFF"}",x,y+289,Color.LTGRAY,10f);txt(c,"PRESSURE   ${if(pressure)"ON" else "OFF"}",x,y+315,Color.LTGRAY,10f);txt(c,"ANTI-ALIAS   ${if(antialias)"ON" else "OFF"}",x,y+341,Color.LTGRAY,10f);txt(c,"ENGINE: ${selectedBrush.engine.name}",x,y+367,Color.LTGRAY,9f)}else if(tool==Tool.FILL){row(c,"STRENGTH","$fillStrength",x,y);row(c,"EXPANSION","${fillExpansion}px",x,y+45);txt(c,"GAP RECOGNITION   ${if(gapRecognition)"ON" else "OFF"}",x,y+110,Color.LTGRAY,10f);txt(c,"UNDER LINE   ON",x,y+136,Color.LTGRAY,10f);txt(c,"REFERENCE   CANVAS",x,y+162,Color.LTGRAY,10f);txt(c,"CONTINUOUS FILL   ON",x,y+188,Color.LTGRAY,10f)}else{row(c,"TOOL",tool.name,x,y);row(c,"MODE","STANDARD",x,y+45)}}

    private fun brushes(c:Canvas,x:Float,y:Float){txt(c,"EACH BRUSH HAS ITS OWN STAMP / TEXTURE ENGINE",x,y,Color.LTGRAY,8f);var yy=y+22;for(f in BrushCatalog.families){txt(c,f,x,yy,Color.WHITE,10f);yy+=18;for(p in BrushCatalog.presets.filter{it.family==f}){txt(c,if(p.id==selectedBrush.id)"• ${p.name}" else "  ${p.name}",x+6,yy,if(p.id==selectedBrush.id)Color.WHITE else Color.GRAY,9f);yy+=16;if(yy>height-135)return};yy+=3}}
    private fun layers(c:Canvas,x:Float,y:Float){txt(c,"+ NEW LAYER",x,y,Color.WHITE,11f);listOf("Layer 3","Layer 2","Layer 1").forEachIndexed{i,s->txt(c,"EYE   $s",x,y+32+i*27,Color.LTGRAY,10f)};txt(c,"LOCK   OPACITY   BLEND",x,y+115,Color.GRAY,9f)}
    private fun timelinePanel(c:Canvas,x:Float,y:Float){txt(c,"ONION SKIN   ON",x,y,Color.LTGRAY,10f);txt(c,"PREVIOUS 2   NEXT 2",x,y+28,Color.LTGRAY,10f);txt(c,"24 FPS",x,y+56,Color.LTGRAY,10f);txt(c,"1  2  3  4  5  6  7  8",x,y+84,Color.WHITE,10f)}
    private fun camera(c:Canvas,x:Float,y:Float){txt(c,"POSITION   0,0",x,y,Color.LTGRAY,10f);txt(c,"SCALE      100%",x,y+28,Color.LTGRAY,10f);txt(c,"ROTATION   0°",x,y+56,Color.LTGRAY,10f);txt(c,"FLIP H / FLIP V",x,y+84,Color.LTGRAY,10f)}

    private fun rulers(c:Canvas,x:Float,y:Float){txt(c,"RULER ENGINE",x,y,Color.WHITE,11f);val names=listOf("STRAIGHT","CIRCULAR","ELLIPSE","RADIAL","MIRROR","KALEIDOSCOPE","ROTATION","ARRAY","PERSPECTIVE ARRAY");names.forEachIndexed{i,n->{val rr=Ruler.values()[i+1];txt(c,if(ruler==rr)"• $n" else "  $n",x,y+27+i*23,if(ruler==rr)Color.WHITE else Color.LTGRAY,9f)}};txt(c,"1 / 2 / 3 POINT PERSPECTIVE: $perspective",x,y+240,Color.LTGRAY,9f);txt(c,"SNAP TO RULER   ${if(perspectiveSnap)"ON" else "OFF"}",x,y+263,Color.LTGRAY,9f)}

    private fun drawRuler(c:Canvas,r:RectF){
        if(ruler==Ruler.NONE&&perspective==0)return
        ui.style=Paint.Style.STROKE;ui.strokeWidth=1f;ui.color=Color.rgb(105,140,175)
        when(ruler){
            Ruler.STRAIGHT->{val yy=r.centerY();c.drawLine(r.left,yy,r.right,yy,ui)}
            Ruler.CIRCULAR->c.drawCircle(r.centerX(),r.centerY(),min(r.width(),r.height())*.32f,ui)
            Ruler.ELLIPSE->c.drawOval(r.centerX()-r.width()*.3f,r.centerY()-r.height()*.22f,r.centerX()+r.width()*.3f,r.centerY()+r.height()*.22f,ui)
            Ruler.RADIAL->{val o=if(radialCenter.x==0f)PointF(r.centerX(),r.centerY()) else radialCenter;for(i in 0 until 24){val a=i*PI/12;c.drawLine(o.x,o.y,o.x+cos(a).toFloat()*r.width(),o.y+sin(a).toFloat()*r.height(),ui)};c.drawCircle(o.x,o.y,5f,ui)}
            Ruler.MIRROR->{val p=Path();p.moveTo(r.centerX(),r.top);p.lineTo(r.centerX(),r.bottom);c.save();c.rotate(mirrorAngle,r.centerX(),r.centerY());c.drawPath(p,ui);c.restore()}
            Ruler.KALEIDOSCOPE,Ruler.ROTATION->{val cx=r.centerX();val cy=r.centerY();c.drawCircle(cx,cy,5f,ui);for(i in 0 until symmetryCount){c.save();c.rotate(i*360f/symmetryCount,cx,cy);c.drawLine(cx,cy,cx,r.top,ui);c.restore()}}
            Ruler.ARRAY,Ruler.PERSPECTIVE_ARRAY->{for(i in 1..4){val xx=r.left+r.width()*i/5f;c.drawLine(xx,r.top,xx,r.bottom,ui)};for(i in 1..3){val yy=r.top+r.height()*i/4f;c.drawLine(r.left,yy,r.right,yy,ui)}}
            else->Unit
        }
        if(perspective>0){val h=r.centerY();val l=r.left;val rr=r.right;val t=r.top;val b=r.bottom;if(perspective==1){val vx=r.centerX();for(i in 1..8){val xx=l+r.width()*i/9f;c.drawLine(vx,h,xx,t,ui);c.drawLine(vx,h,xx,b,ui)}}else{val a=PointF(l+r.width()*.22f,h);val z=PointF(rr-r.width()*.22f,h);c.drawCircle(a.x,a.y,5f,ui);c.drawCircle(z.x,z.y,5f,ui);for(i in 0..7){val yy=t+r.height()*i/7f;c.drawLine(a.x,a.y,rr,yy,ui);c.drawLine(z.x,z.y,l,yy,ui)};if(perspective==3){val v=PointF(r.centerX(),t-90f);for(xx in listOf(l,r.centerX(),rr))c.drawLine(v.x,v.y,xx,b,ui)}}}
        ui.style=Paint.Style.FILL
    }

    private fun timeline(c:Canvas,w:Float,h:Float){ui.color=Color.rgb(27,29,32);c.drawRect(0f,h-112f,w,h,ui);txt(c,"LAYERS",12f,h-82f,Color.LTGRAY,10f);txt(c,"Layer 1",70f,h-82f,Color.WHITE,10f);txt(c,"1  2  3  4  5  6  7  8",170f,h-82f,Color.LTGRAY,10f);txt(c,"|<   <   PLAY   >   >|",w-175f,h-38f,Color.WHITE,10f)}
    private fun row(c:Canvas,l:String,v:String,x:Float,y:Float){txt(c,l,x,y,Color.GRAY,8f);txt(c,v,x,y+19,Color.WHITE,11f);ui.color=Color.rgb(56,59,64);c.drawRect(x,y+27,x+270,y+28,ui)}

    private fun snapPoint(p:PointF,r:RectF):PointF{
        var x=p.x;var y=p.y
        when(ruler){Ruler.STRAIGHT->y=r.centerY();Ruler.RADIAL->{val o=if(radialCenter.x==0f)PointF(r.centerX(),r.centerY()) else radialCenter;val a=atan2(y-o.y,x-o.x);val aa=round(a/(PI/12))*(PI/12);val d=hypot(x-o.x,y-o.y);x=o.x+cos(aa).toFloat()*d;y=o.y+sin(aa).toFloat()*d};else->Unit}
        if(perspectiveSnap&&perspective==1){val cx=r.centerX();val cy=r.centerY();val a=atan2(y-cy,x-cx);val d=hypot(x-cx,y-cy);x=cx+cos(a).toFloat()*d;y=cy+sin(a).toFloat()*d}
        return PointF(x,y)
    }

    private fun drawStroke(raw:List<PointF>){val r=rect();val snapped=raw.map{snapPoint(it,r)};val smooth=Stabilizer.smooth(snapped,stabilizer/100f);if(tool==Tool.ERASER){val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.TRANSPARENT;style=Paint.Style.STROKE;strokeWidth=max(1.5f,brushSize);strokeCap=Paint.Cap.ROUND;xfermode=PorterDuffXfermode(PorterDuff.Mode.CLEAR)};bcanvas?.drawPath(smooth,p)}else{BrushEngine.draw(bcanvas!!,snapped,selectedBrush,brushSize,opacity,flow,antialias)}}

    override fun onTouchEvent(e:MotionEvent):Boolean{
        val x=e.x;val y=e.y;val w=width.toFloat();val h=height.toFloat()
        if(e.action==MotionEvent.ACTION_DOWN){
            if(panel!=null&&x>=w-300&&y<92){panel=null;invalidate();return true}
            if(y in 8f..44f&&x>=190){val i=((x-190)/78).toInt();if(i in Panel.values().indices){val p=Panel.values()[i];panel=if(panel==p)null else p;invalidate();return true}}
            if(x<62&&y in 50f..h-112f){val i=((y-53)/42).toInt().coerceIn(0,Tool.values().lastIndex);tool=Tool.values()[i];panel=Panel.OPTIONS;invalidate();return true}
            if(panel==Panel.BRUSHES&&x>=w-300&&y>92){selectBrushAt(y);return true}
            if(panel==Panel.RULERS&&x>=w-300&&y>92){selectRulerAt(y);return true}
            if(!rect().contains(x,y))return true
            if(radialCenter.x==0f)radialCenter=PointF(rect().centerX(),rect().centerY())
            points.clear();points.add(PointF(x,y))
            if(tool==Tool.FILL){BucketEngine.fill(bitmap!!,x.toInt(),y.toInt(),fillColor,BucketEngine.Settings(fillStrength,fillExpansion,gapRecognition,3,true));invalidate();return true}
            if(tool==Tool.LASSO){lasso.reset();lasso.moveTo(x,y);invalidate();return true};return true
        }
        if(e.action==MotionEvent.ACTION_MOVE){if(tool==Tool.LASSO){lasso.lineTo(x,y);invalidate();return true};points.add(PointF(x,y));if(realTime&&points.size>=4){drawStroke(points.takeLast(10));points.clear();points.add(PointF(x,y))};invalidate();return true}
        if(e.action==MotionEvent.ACTION_UP){if(tool==Tool.LASSO){lasso.close();invalidate();return true};if(points.size>=2)drawStroke(points);points.clear();invalidate();return true}
        return true
    }

    private fun selectBrushAt(y:Float){val idx=((y-128f)/16f).toInt();if(idx in BrushCatalog.presets.indices){selectedBrush=BrushCatalog.presets[idx];panel=Panel.OPTIONS};invalidate()}
    private fun selectRulerAt(y:Float){val i=((y-133f)/23f).toInt();val rs=listOf(Ruler.STRAIGHT,Ruler.CIRCULAR,Ruler.ELLIPSE,Ruler.RADIAL,Ruler.MIRROR,Ruler.KALEIDOSCOPE,Ruler.ROTATION,Ruler.ARRAY,Ruler.PERSPECTIVE_ARRAY);if(i in rs.indices){ruler=rs[i];panel=Panel.OPTIONS};invalidate()}

    private fun icon(c:Canvas,t:Tool,x:Float,y:Float){ui.color=Color.WHITE;ui.style=Paint.Style.STROKE;ui.strokeWidth=2f;when(t){Tool.BRUSH->c.drawCircle(x,y,8f,ui);Tool.PENCIL->c.drawLine(x-8,y+8,x+8,y-8,ui);Tool.ERASER->c.drawRect(x-9,y-6,x+9,y+7,ui);Tool.LASSO->c.drawOval(x-10,y-8,x+10,y+8,ui);Tool.FILL->c.drawRect(x-8,y-7,x+6,y+6,ui);Tool.PICKER->c.drawCircle(x,y,8f,ui);Tool.MOVE->{c.drawLine(x,y-9,x,y+9,ui);c.drawLine(x-9,y,x+9,y,ui)};Tool.TRANSFORM->c.drawRect(x-8,y-8,x+8,y+8,ui);Tool.LINE->c.drawLine(x-9,y+8,x+9,y-8,ui);Tool.RECTANGLE->c.drawRect(x-9,y-7,x+9,y+7,ui);Tool.ELLIPSE->c.drawOval(x-9,y-7,x+9,y+7,ui)};ui.style=Paint.Style.FILL}
    private fun txt(c:Canvas,s:String,x:Float,y:Float,color:Int,size:Float){text.color=color;text.textSize=size;c.drawText(s,x,y,text)}
}
