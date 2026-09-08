package com.animame.editor

import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Original procedural renderer. It does not contain or redistribute proprietary brush assets.
 * The renderer consumes BrushSettings so the large preset library has real visual behavior.
 */
object AdvancedBrushRenderer {
    fun draw(c: Canvas, stroke: StrokeData, tint: Int? = null) {
        val pts = stroke.samples
        if (pts.isEmpty()) return
        val s = stroke.brushSettings.copy(size = stroke.size, opacity = stroke.opacity).normalized()
        val color = tint ?: stroke.color
        val seed = stroke.id.hashCode().toLong()
        val family = family(stroke.brushId, s.brushType)
        when {
            s.eraser || family == "eraser" -> renderEraser(c, pts, s)
            family == "air" -> renderAir(c, pts, color, s)
            family == "water" -> renderWater(c, pts, color, s, seed)
            family == "paint" -> renderPaint(c, pts, color, s, seed)
            family == "pencil" -> renderPencil(c, pts, color, s, seed)
            family == "ink" -> renderInk(c, pts, color, s)
            family == "marker" -> renderMarker(c, pts, color, s)
            family == "chalk" -> renderChalk(c, pts, color, s, seed)
            family == "smudge" -> renderSmudge(c, pts, color, s)
            family == "particle" -> renderParticles(c, pts, color, s, seed)
            family == "nature" -> renderNature(c, pts, color, s, seed, stroke.brushId)
            family == "effect" -> renderEffect(c, pts, color, s, seed, stroke.brushId)
            family == "texture" -> renderTexture(c, pts, color, s, seed)
            else -> renderBasic(c, pts, color, s)
        }
    }

    private fun family(id: String, type: String): String {
        val x = (id + " " + type).lowercase()
        return when {
            x.contains("eraser") -> "eraser"
            x.contains("water") || x.contains("gouache") || x.contains("wash") -> "water"
            x.contains("oil") || x.contains("acrylic") || x.contains("impasto") || x.contains("paint") -> "paint"
            x.contains("pencil") || x.contains("graphite") || x.contains("sketch") || x.contains("crayon") -> "pencil"
            x.contains("ink") || x.contains("pen") || x.contains("g-pen") || x.contains("fountain") -> "ink"
            x.contains("marker") || x.contains("felt") || x.contains("highlighter") -> "marker"
            x.contains("chalk") || x.contains("pastel") || x.contains("charcoal") -> "chalk"
            x.contains("airbrush") || x.contains("air") -> "air"
            x.contains("smudge") || x.contains("blend") || x.contains("mix") -> "smudge"
            x.contains("spray") || x.contains("particle") || x.contains("stamp") || x.contains("speckle") -> "particle"
            x.contains("grass") || x.contains("leaf") || x.contains("leaves") || x.contains("fur") || x.contains("hair") || x.contains("cloud") || x.contains("vine") || x.contains("flower") || x.contains("branch") || x.contains("bamboo") -> "nature"
            x.contains("fire") || x.contains("light") || x.contains("glow") || x.contains("neon") || x.contains("star") || x.contains("spark") || x.contains("glitter") || x.contains("bloom") -> "effect"
            x.contains("texture") || x.contains("canvas") || x.contains("paper") || x.contains("noise") -> "texture"
            else -> "basic"
        }
    }

    private fun basePaint(color: Int, alpha: Int, aa: Boolean, mode: PorterDuff.Mode? = null) = Paint().apply {
        isAntiAlias = aa
        this.color = color
        this.alpha = alpha.coerceIn(0, 255)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        if (mode != null) xfermode = PorterDuffXfermode(mode)
    }

    private fun renderBasic(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings) {
        if (pts.size == 1) { dab(c, pts[0].x, pts[0].y, color, s, pts[0].pressure); return }
        val p = basePaint(color, 255, s.antialias)
        var dist = 0f
        val total = length(pts)
        for (i in 1 until pts.size) {
            val a = pts[i - 1]; val b = pts[i]
            val d = hypot(b.x - a.x, b.y - a.y)
            val pr = ((a.pressure + b.pressure) * .5f).coerceIn(.05f, 1.5f)
            p.strokeWidth = s.radiusFor(pr, (a.tilt + b.tilt) * .5f, speed(a, b))
            p.alpha = alpha(s.opacityFor(pr, (a.tilt + b.tilt) * .5f, dist, total, speed(a, b)))
            c.drawLine(a.x, a.y, b.x, b.y, p)
            dist += d
        }
        if (s.spacing > .2f || s.jitter > 0f || s.scatter) dabs(c, pts, color, s)
    }

    private fun renderInk(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings) {
        val p = basePaint(color, 255, s.antialias)
        val total = length(pts); var dist = 0f
        for (i in 1 until pts.size) {
            val a = pts[i - 1]; val b = pts[i]; val d = hypot(b.x-a.x,b.y-a.y); val t = if(total==0f) 1f else dist/total
            p.strokeWidth = s.radiusFor((a.pressure+b.pressure)*.5f,(a.tilt+b.tilt)*.5f,speed(a,b))*(1.12f-.3f*t)
            p.alpha = alpha(s.opacityFor((a.pressure+b.pressure)*.5f,0f,dist,total,speed(a,b)))
            c.drawLine(a.x,a.y,b.x,b.y,p); dist += d
        }
    }

    private fun renderPencil(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings, seed: Long) {
        val p = basePaint(color, alpha(s.opacity*.72f), s.antialias)
        p.strokeWidth = max(1f, s.size*.55f); strokePath(c,pts,p)
        val r=Random(seed); repeat(3) {
            val q=basePaint(color, alpha(s.opacity*.22f), false); q.strokeWidth=max(.5f,s.size*.08f)
            val off=s.size*.22f; val shifted=pts.map{StrokeSample(it.x+(r.nextFloat()-.5f)*off,it.y+(r.nextFloat()-.5f)*off,it.pressure,it.timeMs,it.tilt,it.orientation)}
            strokePath(c,shifted,q)
        }
    }

    private fun renderMarker(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings) {
        val p=basePaint(color,alpha(s.opacity),s.antialias); p.strokeWidth=s.size; p.strokeCap=Paint.Cap.SQUARE
        strokePath(c,pts,p)
        if(s.feather>.2f){p.alpha=alpha(s.opacity*.12f);p.strokeWidth=s.size*1.12f;strokePath(c,pts,p)}
    }

    private fun renderPaint(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings, seed: Long) {
        val r=Random(seed)
        val under=basePaint(color,alpha(s.opacity*.42f),s.antialias); under.strokeWidth=s.size*1.18f; strokePath(c,pts,under)
        val body=basePaint(color,alpha(s.opacity*.72f),s.antialias); body.strokeWidth=s.size*.88f; strokePath(c,pts,body)
        val hi=basePaint(lighten(color,.18f),alpha(s.opacity*.16f),s.antialias); hi.strokeWidth=max(1f,s.size*.12f)
        val shifted=pts.map{StrokeSample(it.x+(r.nextFloat()-.5f)*s.size*.15f,it.y+(r.nextFloat()-.5f)*s.size*.15f,it.pressure,it.timeMs,it.tilt,it.orientation)}
        strokePath(c,shifted,hi)
        dabs(c,pts,color,s.copy(spacing=max(.08f,s.spacing),jitter=max(.04f,s.jitter)),seed)
    }

    private fun renderWater(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings, seed: Long) {
        val p=basePaint(color,alpha(s.opacity*.25f),s.antialias); p.strokeWidth=s.size*1.35f; p.maskFilter=BlurMaskFilter(s.size*(.2f+.65f*s.feather),BlurMaskFilter.Blur.NORMAL);strokePath(c,pts,p)
        val body=basePaint(color,alpha(s.opacity*.32f),s.antialias);body.strokeWidth=s.size*.72f;strokePath(c,pts,body)
        dabs(c,pts,color,s.copy(opacity=s.opacity*.35f,spacing=max(.08f,s.spacing)),seed)
        if(s.textureStrength>.05f) textureDabs(c,pts,color,s,seed)
    }

    private fun renderAir(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings) {
        val total=length(pts); var d=0f
        for(i in pts.indices){if(i>0)d+=dist(pts[i-1],pts[i]);val q=pts[i];val radius=s.radiusFor(q.pressure,q.tilt);val p=Paint().apply{isAntiAlias=s.antialias;this.color=color;style=Paint.Style.FILL;alpha=alpha(s.opacityFor(q.pressure,q.tilt,d,total))};p.shader=RadialGradient(q.x,q.y,radius,intArrayOf(Color.argb(p.alpha,color.red(),color.green(),color.blue()),Color.argb(0,color.red(),color.green(),color.blue())),floatArrayOf(0f,1f),Shader.TileMode.CLAMP);c.drawCircle(q.x,q.y,radius,p)}
    }

    private fun renderChalk(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings, seed: Long) {
        val p=basePaint(color,alpha(s.opacity*.7f),false);p.strokeWidth=s.size*.7f;strokePath(c,pts,p)
        textureDabs(c,pts,color,s.copy(opacity=s.opacity*.35f,textureStrength=max(.45f,s.textureStrength)),seed)
    }

    private fun renderSmudge(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings) {
        val p=basePaint(color,alpha(s.opacity*.22f),s.antialias);p.strokeWidth=s.size*1.15f;strokePath(c,pts,p)
        val q=basePaint(lighten(color,.08f),alpha(s.opacity*.12f),s.antialias);q.strokeWidth=s.size*.72f;strokePath(c,pts,q)
    }

    private fun renderParticles(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings, seed: Long) {
        val r=Random(seed);val density=max(1,(s.particleDensity*8f+s.size/10f).roundToInt())
        for(i in pts.indices){val q=pts[i];repeat(density){val rr=s.particleSize.coerceAtLeast(s.size*.18f)*(.45f+r.nextFloat());val j=s.size*(s.jitterPosition+.15f);val x=q.x+(r.nextFloat()-.5f)*j;val y=q.y+(r.nextFloat()-.5f)*j;val p=Paint().apply{isAntiAlias=s.antialias;style=Paint.Style.FILL;this.color=color;alpha=alpha(s.opacity*.45f)};c.drawCircle(x,y,rr*.5f,p)}}
    }

    private fun renderNature(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings, seed: Long, id: String) {
        val r=Random(seed); for(i in pts.indices step 2){val q=pts[i];val n=3+(s.particleDensity*5).roundToInt();repeat(n){val x=q.x+(r.nextFloat()-.5f)*s.size;val y=q.y+(r.nextFloat()-.5f)*s.size;val p=Paint().apply{isAntiAlias=true;style=Paint.Style.FILL;this.color=color;alpha=alpha(s.opacity*.7f)};when{ id.contains("grass")||id.contains("bamboo") -> {p.style=Paint.Style.STROKE;p.strokeWidth=max(1f,s.size*.08f);c.drawLine(x,y,x+(r.nextFloat()-.5f)*s.size*.6f,y-s.size*(.5f+r.nextFloat()*1.5f),p)}; id.contains("fur")||id.contains("hair") -> {p.style=Paint.Style.STROKE;p.strokeWidth=max(1f,s.size*.07f);c.drawLine(x,y,x+(r.nextFloat()-.5f)*s.size,y-s.size*(.2f+r.nextFloat()),p)}; else -> {c.save();c.rotate(r.nextFloat()*180f,q.x,q.y);c.drawOval(x-s.size*.3f,y-s.size*.16f,x+s.size*.3f,y+s.size*.16f,p);c.restore()}}}}
    }

    private fun renderEffect(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings, seed: Long, id: String) {
        val p=basePaint(color,alpha(s.opacity*.18f),s.antialias);p.strokeWidth=s.size*1.8f;p.maskFilter=BlurMaskFilter(s.size*.9f,BlurMaskFilter.Blur.NORMAL);strokePath(c,pts,p)
        val core=basePaint(if(id.contains("fire"))Color.rgb(255,150,35) else Color.WHITE,alpha(s.opacity*.9f),s.antialias);core.strokeWidth=max(1f,s.size*.25f);strokePath(c,pts,core)
        if(id.contains("star")||id.contains("spark")||id.contains("glitter")){val r=Random(seed);repeat(max(2,pts.size/3)){val q=pts[r.nextInt(pts.size)];val a=Paint().apply{isAntiAlias=true;style=Paint.Style.FILL;color=Color.WHITE;alpha=alpha(s.opacity)};c.drawCircle(q.x+(r.nextFloat()-.5f)*s.size*2f,q.y+(r.nextFloat()-.5f)*s.size*2f,max(1f,s.size*.12f),a)}}
    }

    private fun renderTexture(c: Canvas, pts: List<StrokeSample>, color: Int, s: BrushSettings, seed: Long) {
        renderBasic(c,pts,color,s.copy(opacity=s.opacity*.7f));textureDabs(c,pts,color,s.copy(opacity=s.opacity*.45f,textureStrength=max(.4f,s.textureStrength)),seed)
    }

    private fun renderEraser(c: Canvas, pts: List<StrokeSample>, s: BrushSettings) {
        val p=basePaint(Color.TRANSPARENT,255,s.antialias,PorterDuff.Mode.DST_OUT);p.strokeWidth=s.size;strokePath(c,pts,p);p.xfermode=null
    }

    private fun dabs(c:Canvas,pts:List<StrokeSample>,color:Int,s:BrushSettings,seed:Long=0L){val r=Random(seed);val step=max(1f,s.size*s.spacing);var carry=0f;for(i in 1 until pts.size){val a=pts[i-1];val b=pts[i];val d=dist(a,b);var t=(step-carry).coerceAtLeast(0f);while(t<=d){val u=if(d==0f)0f else t/d;val x=a.x+(b.x-a.x)*u;val y=a.y+(b.y-a.y)*u;val j=s.size*s.jitterPosition;dab(c,x+(r.nextFloat()-.5f)*j,y+(r.nextFloat()-.5f)*j,color,s,(a.pressure+b.pressure)*.5f);t+=step};carry=(carry+d)%step}}

    private fun textureDabs(c:Canvas,pts:List<StrokeSample>,color:Int,s:BrushSettings,seed:Long){val r=Random(seed+31);val count=max(2,(s.size*s.textureStrength/2f).roundToInt());for(q in pts step 2)repeat(count){val j=s.size*(.25f+.75f*s.textureStrength);val p=Paint().apply{isAntiAlias=false;style=Paint.Style.FILL;this.color=color;alpha=alpha(s.opacity*.12f)};c.drawCircle(q.x+(r.nextFloat()-.5f)*j,q.y+(r.nextFloat()-.5f)*j,max(.4f,s.size*.015f),p)}}

    private fun dab(c:Canvas,x:Float,y:Float,color:Int,s:BrushSettings,pressure:Float){val p=Paint().apply{isAntiAlias=s.antialias;style=Paint.Style.FILL;this.color=color;alpha=alpha(s.opacityFor(pressure,0f,0f,0f))};val rx=s.radiusFor(pressure,0f)/2f;val ry=rx*s.aspect.coerceIn(.05f,4f);c.drawOval(x-rx,y-ry,x+rx,y+ry,p)}
    private fun strokePath(c:Canvas,pts:List<StrokeSample>,p:Paint){if(pts.isEmpty())return;val q=Path();q.moveTo(pts[0].x,pts[0].y);for(i in 1 until pts.size){val a=pts[i-1];val b=pts[i];q.quadTo(a.x,a.y,(a.x+b.x)/2f,(a.y+b.y)/2f)};if(pts.size>1)q.lineTo(pts.last().x,pts.last().y);c.drawPath(q,p)}
    private fun length(p:List<StrokeSample>):Float{var n=0f;for(i in 1 until p.size)n+=dist(p[i-1],p[i]);return n}
    private fun dist(a:StrokeSample,b:StrokeSample)=hypot(b.x-a.x,b.y-a.y)
    private fun speed(a:StrokeSample,b:StrokeSample):Float{val dt=max(1L,b.timeMs-a.timeMs);return (dist(a,b)/dt*0.08f).coerceIn(0f,1f)}
    private fun alpha(v:Float)= (v*255f).roundToInt().coerceIn(0,255)
    private fun lighten(c:Int,amount:Float):Int=Color.rgb((c.red()+(255-c.red())*amount).roundToInt(),(c.green()+(255-c.green())*amount).roundToInt(),(c.blue()+(255-c.blue())*amount).roundToInt())
    private fun Int.red()=Color.red(this); private fun Int.green()=Color.green(this); private fun Int.blue()=Color.blue(this)
}
