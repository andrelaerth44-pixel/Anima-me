package com.animame.editor

import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

object BrushEngine {
    private fun paint(color:Int, size:Float, alpha:Int=255):Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color=color; this.alpha=alpha.coerceIn(0,255); strokeWidth=size; style=Paint.Style.STROKE
        strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND
    }

    fun draw(canvas:Canvas, points:List<PointF>, preset:BrushPreset, size:Float, opacity:Float, flow:Float, antialias:Boolean) {
        if(points.isEmpty()) return
        val p=Paint(if(antialias) Paint.ANTI_ALIAS_FLAG else 0).apply { strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND }
        val a=(255f*opacity*flow).toInt().coerceIn(0,255)
        when(preset.id) {
            "water", "aotz-water" -> water(canvas,points,size,a)
            "fire" -> fire(canvas,points,size,a)
            "light" -> light(canvas,points,size,a)
            "stars" -> stars(canvas,points,size,a)
            "chain" -> chain(canvas,points,size,a)
            "aotz-clouds" -> clouds(canvas,points,size,a)
            "aotz-grass" -> grass(canvas,points,size,a)
            "aotz-leaves" -> leaves(canvas,points,size,a)
            "aotz-fur" -> fur(canvas,points,size,a)
            "pencil" -> pencil(canvas,points,size,a)
            "ink", "aotz-ink" -> ink(canvas,points,size,a)
            "paint" -> paintStroke(canvas,points,size,a)
            else -> basic(canvas,points,size,a,preset.id=="eraser"||preset.id=="aotz-eraser")
        }
    }

    private fun basic(c:Canvas,pts:List<PointF>,s:Float,a:Int,eraser:Boolean) {
        val p=paint(Color.rgb(25,25,25),s,a); if(eraser)p.xfermode=PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        path(c,pts,p)
    }
    private fun ink(c:Canvas,pts:List<PointF>,s:Float,a:Int) {
        if(pts.size<2){basic(c,pts,s,a,false);return}; val p=paint(Color.rgb(20,20,24),s,a); val path=Path();path.moveTo(pts[0].x,pts[0].y)
        for(i in 1 until pts.size){val t=i.toFloat()/(pts.size-1);p.strokeWidth=s*(1.18f-0.35f*t);path.lineTo(pts[i].x,pts[i].y)};c.drawPath(path,p)
    }
    private fun pencil(c:Canvas,pts:List<PointF>,s:Float,a:Int) {
        val r=Random(37); val p=paint(Color.rgb(55,55,58),max(1f,s*.58f),a/2); path(c,pts,p)
        repeat(max(2,(s/2).toInt())) { val q=paint(Color.rgb(45,45,48),max(.5f,s*.06f),a/3); val jitter=s*.35f; val shifted=pts.map{PointF(it.x+r.nextFloat()*jitter-jitter/2,it.y+r.nextFloat()*jitter-jitter/2)}; path(c,shifted,q) }
    }
    private fun paintStroke(c:Canvas,pts:List<PointF>,s:Float,a:Int) {
        val p=paint(Color.rgb(25,25,25),s,a); p.strokeCap=Paint.Cap.ROUND; path(c,pts,p)
        val r=Random(91); repeat(3){ val q=paint(Color.WHITE,s*.06f,a/6); val shifted=pts.map{PointF(it.x+r.nextFloat()*s-s/2,it.y+r.nextFloat()*s-s/2)};path(c,shifted,q)}
    }
    private fun water(c:Canvas,pts:List<PointF>,s:Float,a:Int) {
        if(pts.size<2){return}; val r=Random(1001); val p=paint(Color.rgb(50,145,235),s*.72f,a/3);path(c,pts,p)
        for(i in pts.indices step 2){val q=pts[i];val rr=s*(.35f+r.nextFloat()*.65f);val wp=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(55,165,245);alpha=a/3;style=Paint.Style.STROKE;strokeWidth=max(1f,s*.08f)};c.drawOval(q.x-rr,q.y-rr*.35f,q.x+rr,q.y+rr*.35f,wp)}
        val hi=paint(Color.WHITE,max(1f,s*.055f),a/2); val hp=pts.mapIndexed{i,q->PointF(q.x,q.y-s*.18f*sin(i*.7f))};path(c,hp,hi)
    }
    private fun fire(c:Canvas,pts:List<PointF>,s:Float,a:Int) {
        val glow=paint(Color.rgb(255,70,10),s*1.25f,a/6);glow.maskFilter=BlurMaskFilter(s*.7f,BlurMaskFilter.Blur.NORMAL);path(c,pts,glow)
        val r=Random(2024); for(i in pts.indices){val q=pts[i];val wob=s*.55f*sin(i*.9f);val fp=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=if(i%2==0)Color.rgb(255,75,10) else Color.rgb(255,170,20);alpha=a;style=Paint.Style.FILL};val h=s*(.7f+r.nextFloat()*1.7f);val w=s*(.35f+r.nextFloat()*.65f);val path=Path();path.moveTo(q.x-w,q.y);path.quadTo(q.x+w,q.y-s*.35f,q.x+w*.2f,q.y-h);path.quadTo(q.x-w*.15f,q.y-h*.55f,q.x-w,q.y);c.drawPath(path,fp);if(i%3==0){val ember=Paint(Paint.ANTI_ALIAS_FLAG);ember.color=Color.YELLOW;ember.alpha=a/2;c.drawCircle(q.x+wob,q.y-h*.55f,max(1f,s*.09f),ember)}}
    }
    private fun light(c:Canvas,pts:List<PointF>,s:Float,a:Int) {
        val glow=paint(Color.WHITE,s*1.6f,a/4);glow.maskFilter=BlurMaskFilter(s*1.4f,BlurMaskFilter.Blur.NORMAL);path(c,pts,glow)
        val g=paint(Color.rgb(180,225,255),s*.8f,a/3);g.maskFilter=BlurMaskFilter(s*.5f,BlurMaskFilter.Blur.NORMAL);path(c,pts,g)
        path(c,pts,paint(Color.WHITE,max(1f,s*.22f),a))
    }
    private fun stars(c:Canvas,pts:List<PointF>,s:Float,a:Int) {
        val r=Random(77); val spacing=max(4f,s*1.8f); for(i in pts.indices step 2){val q=pts[i];val rr=s*(.35f+r.nextFloat()*.75f);star(c,q.x+(r.nextFloat()-.5f)*s,q.y+(r.nextFloat()-.5f)*s,rr,paint(Color.WHITE,1f,a))}
        if(pts.size>1){val p=paint(Color.rgb(180,200,255),max(1f,s*.08f),a/3);path(c,pts,p)}
    }
    private fun chain(c:Canvas,pts:List<PointF>,s:Float,a:Int) {
        if(pts.size<2)return; val step=max(6f,s*1.65f);var carry=0f;for(i in 1 until pts.size){val A=pts[i-1];val B=pts[i];val dx=B.x-A.x;val dy=B.y-A.y;val d=hypot(dx,dy);val ang=atan2(dy,dx);var t=carry+step/2;while(t<d){val x=A.x+dx*(t/d);val y=A.y+dy*(t/d);val rx=s*.75f;val ry=s*.45f;val q=paint(Color.rgb(70,72,78),max(1f,s*.13f),a);c.save();c.rotate(Math.toDegrees(ang.toDouble()).toFloat(),x,y);c.drawOval(x-rx,y-ry,x+rx,y+ry,q);c.restore();t+=step};carry=(t-d)}
    }
    private fun clouds(c:Canvas,pts:List<PointF>,s:Float,a:Int){val r=Random(4);for(i in pts.indices step 2){val q=pts[i];val p=Paint(Paint.ANTI_ALIAS_FLAG);p.color=Color.rgb(220,225,235);p.alpha=a/2;repeat(4){val rr=s*(.45f+r.nextFloat()*.65f);c.drawCircle(q.x+(r.nextFloat()-.5f)*s,q.y+(r.nextFloat()-.5f)*s,rr,p)}}}
    private fun grass(c:Canvas,pts:List<PointF>,s:Float,a:Int){val r=Random(6);val p=paint(Color.rgb(55,145,70),max(1f,s*.12f),a);for(q in pts step 2){val h=s*(1f+r.nextFloat()*2f);c.drawLine(q.x,q.y,q.x+(r.nextFloat()-.5f)*s,q.y-h,p)}}
    private fun leaves(c:Canvas,pts:List<PointF>,s:Float,a:Int){val r=Random(8);for(q in pts step 2){val p=Paint(Paint.ANTI_ALIAS_FLAG);p.color=Color.rgb(55,145,75);p.alpha=a;val w=s*(.55f+r.nextFloat()*.6f);val h=s*(.3f+r.nextFloat()*.45f);c.save();c.rotate(r.nextFloat()*90f-45f,q.x,q.y);c.drawOval(q.x-w,q.y-h,q.x+w,q.y+h,p);c.restore()}}
    private fun fur(c:Canvas,pts:List<PointF>,s:Float,a:Int){val r=Random(9);val p=paint(Color.rgb(85,70,55),max(1f,s*.1f),a);for(q in pts step 1){val h=s*(.4f+r.nextFloat()*1.3f);c.drawLine(q.x,q.y,q.x+(r.nextFloat()-.5f)*s*.7f,q.y-h,p)}}
    private fun path(c:Canvas,pts:List<PointF>,p:Paint){if(pts.isEmpty())return;val q=Path();q.moveTo(pts[0].x,pts[0].y);for(i in 1 until pts.size){val a=pts[i-1];val b=pts[i];q.quadTo(a.x,a.y,(a.x+b.x)/2f,(a.y+b.y)/2f)};if(pts.size>1)q.lineTo(pts.last().x,pts.last().y);c.drawPath(q,p)}
    private fun star(c:Canvas,x:Float,y:Float,r:Float,p:Paint){val path=Path();for(i in 0 until 10){val rr=if(i%2==0)r else r*.38f;val a=-Math.PI/2+i*Math.PI/5;val px=x+cos(a)*rr;val py=y+sin(a)*rr;if(i==0)path.moveTo(px,py)else path.lineTo(px,py)};path.close();p.style=Paint.Style.FILL;c.drawPath(path,p)}
}