package com.animame.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class AnimationEditorView(context: Context) : View(context) {
    private enum class Tool { BRUSH, PENCIL, ERASER, LASSO, FILL, PICKER, MOVE, TRANSFORM, LINE, RECTANGLE, ELLIPSE }
    private enum class Panel { OPTIONS, BRUSHES, LAYERS, TIMELINE, CAMERA, GUIDES }

    private val guides = PerspectiveGuideEngine()
    private var tool = Tool.BRUSH
    private var panel: Panel? = Panel.OPTIONS
    private var brushSize = 12f
    private var opacity = 1f
    private var selectedBrush = BrushCatalog.presets.first()
    private var current: Path? = null
    private val strokes = mutableListOf<Pair<Path, Paint>>()
    private val ui = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)

    init { setBackgroundColor(Color.rgb(16,17,19)); setLayerType(View.LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        val left = 62f; val right = if (panel == null) 62f else 302f; val top = 50f; val bottom = 112f
        ui.color = Color.rgb(27,29,32); c.drawRect(0f,0f,w,top,ui)
        label(c,"ANIMA-ME",14f,32f,Color.WHITE,15f); label(c,"Untitled",116f,32f,Color.LTGRAY,12f); label(c,"24 FPS",w-116f,32f,Color.LTGRAY,12f)
        drawTabs(c,w)
        val cl=left+18f; val ct=top+18f; val cr=w-right-18f; val cb=h-bottom-18f
        ui.color=Color.rgb(244,244,244); c.drawRect(cl,ct,cr,cb,ui)
        drawGuides(c,cl,ct,cr,cb)
        strokes.forEach { c.drawPath(it.first,it.second) }; current?.let { c.drawPath(it,makePaint()) }
        drawRail(c,top,cb); if(panel!=null) drawPanel(c,w-right,top,right,h-bottom); drawTimeline(c,w,h)
    }

    private fun drawTabs(c:Canvas,w:Float){
        val names=listOf("OPTIONS","BRUSHES","LAYERS","TIMELINE","CAMERA","GUIDES")
        names.forEachIndexed { i,n -> val x=190f+i*82f; if(x>w-92f)return@forEachIndexed; ui.color=if(panel==Panel.values()[i])Color.rgb(68,73,81)else Color.rgb(43,46,50); c.drawRoundRect(x,9f,x+76f,41f,7f,7f,ui); label(c,n,x+8f,29f,Color.WHITE,8f) }
    }

    private fun drawRail(c:Canvas,top:Float,bottom:Float){
        ui.color=Color.rgb(31,33,36); c.drawRect(0f,top,62f,bottom,ui)
        Tool.values().forEachIndexed { i,t -> val y=top+22f+i*41f; if(y<bottom-10f){if(t==tool){ui.color=Color.rgb(68,73,81);c.drawRoundRect(5f,y-16f,57f,y+16f,7f,7f,ui)}; label(c,t.name.take(2),18f,y+4f,Color.WHITE,9f)} }
    }

    private fun drawGuides(c:Canvas,l:Float,t:Float,r:Float,b:Float){
        if(guides.gridVisible){ui.color=Color.argb(45,90,90,90);ui.strokeWidth=1f;var x=l;while(x<r){c.drawLine(x,t,x,b,ui);x+=max(12f,(r-l)*guides.gridSpacing)};var y=t;while(y<b){c.drawLine(l,y,r,y,ui);y+=max(12f,(b-t)*guides.gridSpacing)}}
        if(guides.fieldGuideVisible){ui.style=Paint.Style.STROKE;ui.color=Color.argb(150,100,100,100);ui.strokeWidth=1f;c.drawRect(l+(r-l)*.1f,t+(b-t)*.1f,r-(r-l)*.1f,b-(b-t)*.1f,ui);ui.style=Paint.Style.FILL}
        if(guides.safeAreaVisible){ui.style=Paint.Style.STROKE;ui.color=Color.argb(120,150,150,150);c.drawRect(l+(r-l)*.05f,t+(b-t)*.05f,r-(r-l)*.05f,b-(b-t)*.05f,ui);ui.style=Paint.Style.FILL}
        if(guides.guidesVisible){ui.color=Color.argb(150,80,120,220);guides.customGuides.forEach{g->if(g.horizontal)c.drawLine(l,t+(b-t)*g.position,r,t+(b-t)*g.position,ui)else c.drawLine(l+(r-l)*g.position,t,l+(r-l)*g.position,b,ui)}}
        if(guides.state.mode!=PerspectiveMode.OFF){
            ui.color=Color.argb(90,80,120,255);ui.strokeWidth=1f
            val vps=guides.vanishingPoints();vps.forEach{vp->val px=l+(r-l)*vp.x;val py=t+(b-t)*vp.y;c.drawCircle(px,py,7f,ui);c.drawLine(l,py,r,py,ui);c.drawLine(px,t,px,b,ui)}
            if(guides.state.mode==PerspectiveMode.THREE_POINT){val a=vps[0];val bvp=vps[1];val d=vps[2];c.drawLine(l+(r-l)*a.x,t+(b-t)*a.y,l+(r-l)*d.x,t+(b-t)*d.y,ui);c.drawLine(l+(r-l)*bvp.x,t+(b-t)*bvp.y,l+(r-l)*d.x,t+(b-t)*d.y,ui)}
            label(c,"${guides.state.mode.name.replace('_',' ')}  SNAP ${if(guides.state.snap)"ON" else "OFF"}",l+8f,t+18f,Color.DKGRAY,9f)
        }
        if(guides.rulersVisible){ui.color=Color.rgb(215,215,215);c.drawRect(l,t-16f,r,t,ui);c.drawRect(l-16f,t,l,b,ui);ui.color=Color.GRAY;for(i in 0..10){val x=l+(r-l)*i/10f;c.drawLine(x,t-16f,x,t,ui);val y=t+(b-t)*i/10f;c.drawLine(l-16f,y,l,y,ui)}}
    }

    private fun drawPanel(c:Canvas,x:Float,t:Float,w:Float,b:Float){
        ui.color=Color.rgb(29,31,34);c.drawRect(x,t,x+w,b,ui);val title=when(panel){Panel.OPTIONS->"TOOL OPTIONS";Panel.BRUSHES->"BRUSH LIBRARY";Panel.LAYERS->"LAYERS";Panel.TIMELINE->"TIMELINE";Panel.CAMERA->"CAMERA / TRANSFORM";Panel.GUIDES->"RULERS & PERSPECTIVE";null->""};label(c,title,x+14f,t+27f,Color.WHITE,13f);label(c,"TAP AGAIN TO CLOSE",x+w-125f,t+27f,Color.GRAY,8f)
        when(panel){Panel.OPTIONS->drawOptions(c,x+14f,t+58f);Panel.BRUSHES->drawBrushes(c,x+14f,t+58f,b);Panel.LAYERS->drawLayers(c,x+14f,t+58f);Panel.TIMELINE->drawTimelinePanel(c,x+14f,t+58f);Panel.CAMERA->drawCamera(c,x+14f,t+58f);Panel.GUIDES->drawGuidePanel(c,x+14f,t+58f);null->Unit}
    }

    private fun drawOptions(c:Canvas,x:Float,y:Float){label(c,"BRUSH  ${selectedBrush.name}",x,y,Color.WHITE,12f);label(c,"SIZE  ${brushSize.toInt()} px",x,y+32,Color.LTGRAY,11f);label(c,"OPACITY  ${(opacity*100).toInt()}%",x,y+60,Color.LTGRAY,11f);label(c,"PRESSURE  ON",x,y+88,Color.LTGRAY,11f);label(c,"SMOOTHING  50%",x,y+116,Color.LTGRAY,11f);label(c,"ENGINE  ${selectedBrush.engine.name}",x,y+144,Color.GRAY,9f)}
    private fun drawBrushes(c:Canvas,x:Float,y:Float,b:Float){var yy=y;BrushCatalog.families.forEach{family->label(c,family,x,yy,Color.WHITE,11f);yy+=20;BrushCatalog.presets.filter{it.family==family}.forEach{p->if(yy<b-30){label(c,if(p.id==selectedBrush.id)"• ${p.name}" else "  ${p.name}",x+8,yy,if(p.id==selectedBrush.id)Color.WHITE else Color.GRAY,10f);yy+=18}};yy+=5}}
    private fun drawLayers(c:Canvas,x:Float,y:Float){listOf("+ NEW LAYER","EYE  Layer 3","EYE  Layer 2","EYE  Layer 1","LOCK  OPACITY  BLEND").forEachIndexed{i,s->label(c,s,x,y+i*28f,Color.LTGRAY,11f)}}
    private fun drawTimelinePanel(c:Canvas,x:Float,y:Float){label(c,"ONION SKIN  ON",x,y,Color.LTGRAY,11f);label(c,"PREVIOUS 2   NEXT 2",x,y+28,Color.LTGRAY,11f);label(c,"24 FPS   PLAYBACK  |< < PLAY > >|",x,y+56,Color.WHITE,10f)}
    private fun drawCamera(c:Canvas,x:Float,y:Float){label(c,"POSITION  0,0",x,y,Color.LTGRAY,11f);label(c,"SCALE 100%",x,y+28,Color.LTGRAY,11f);label(c,"ROTATION 0°",x,y+56,Color.LTGRAY,11f);label(c,"FLIP H / FLIP V",x,y+84,Color.LTGRAY,11f)}
    private fun drawGuidePanel(c:Canvas,x:Float,y:Float){label(c,"PERSPECTIVE",x,y,Color.WHITE,12f);label(c,"1P   2P   3P   OFF",x,y+30,Color.LTGRAY,12f);label(c,"SNAP  ${if(guides.state.snap)"ON" else "OFF"}",x,y+58,Color.LTGRAY,11f);label(c,"RULERS  ${if(guides.rulersVisible)"ON" else "OFF"}",x,y+84,Color.LTGRAY,11f);label(c,"GUIDES  ${if(guides.guidesVisible)"ON" else "OFF"}",x,y+110,Color.LTGRAY,11f);label(c,"GRID  ${if(guides.gridVisible)"ON" else "OFF"}",x,y+136,Color.LTGRAY,11f);label(c,"FIELD GUIDE  ${if(guides.fieldGuideVisible)"ON" else "OFF"}",x,y+162,Color.LTGRAY,11f);label(c,"SAFE AREA  ${if(guides.safeAreaVisible)"ON" else "OFF"}",x,y+188,Color.LTGRAY,11f);label(c,"CUSTOM GUIDE: tap ruler edge",x,y+224,Color.GRAY,9f)}
    private fun drawTimeline(c:Canvas,w:Float,h:Float){ui.color=Color.rgb(27,29,32);c.drawRect(0f,h-112,w,h,ui);label(c,"LAYERS",12f,h-82,Color.LTGRAY,10f);label(c,"Layer 1    1  2  3  4  5  6  7  8",70f,h-82,Color.WHITE,11f);label(c,"|<  <  PLAY  >  >|",w-150f,h-38,Color.WHITE,10f)}

    override fun onTouchEvent(e:MotionEvent):Boolean{
        val x=e.x;val y=e.y;val w=width.toFloat();val h=height.toFloat()
        if(e.action==MotionEvent.ACTION_DOWN){
            if(panel!=null&&x>w-302f&&y in 50f..88f){panel=null;invalidate();return true}
            if(y in 8f..44f&&x>=190f){val i=((x-190f)/82f).toInt();if(i in Panel.values().indices){val p=Panel.values()[i];panel=if(panel==p)null else p;invalidate();return true}}
            if(x<62f&&y in 50f..h-112f){val i=((y-53f)/41f).toInt().coerceIn(0,Tool.values().lastIndex);tool=Tool.values()[i];panel=Panel.OPTIONS;invalidate();return true}
            if(panel==Panel.GUIDES&&x>w-302f){handleGuidePanel(x-(w-302f),y-108f);invalidate();return true}
            val cl=80f;val ct=68f;val cr=w-(if(panel==null)62f else 302f)-18f;val cb=h-130f
            if(x>=cl&&x<=cr&&y>=ct&&y<=cb){current=Path();val p=snapped(PointF(x,y),cl,ct,cr,cb);current!!.moveTo(p.x,p.y);return true}
        }
        if(current!=null&&(e.action==MotionEvent.ACTION_MOVE||e.action==MotionEvent.ACTION_UP)){
            val cl=80f;val ct=68f;val cr=w-(if(panel==null)62f else 302f)-18f;val cb=h-130f;val p=snapped(PointF(x,y),cl,ct,cr,cb)
            if(e.action==MotionEvent.ACTION_MOVE){if(tool==Tool.LINE){current!!.rewind();current!!.moveTo(current!!.bounds.left,current!!.bounds.top);current!!.lineTo(p.x,p.y)}else current!!.lineTo(p.x,p.y);invalidate();return true}
            strokes+=current!! to makePaint();current=null;invalidate();return true
        }
        return true
    }

    private fun snapped(p:PointF,l:Float,t:Float,r:Float,b:Float):PointF{val n=PointF(((p.x-l)/(r-l)).coerceIn(0f,1f),((p.y-t)/(b-t)).coerceIn(0f,1f));val s=if(tool==Tool.LASSO||tool==Tool.BRUSH||tool==Tool.PENCIL||tool==Tool.LINE)guides.snap(n) else n;return PointF(l+s.x*(r-l),t+s.y*(b-t))}

    private fun handleGuidePanel(x:Float,y:Float){if(y in 15f..55f){when{x<75f->guides.state.mode=PerspectiveMode.ONE_POINT;x<150f->guides.state.mode=PerspectiveMode.TWO_POINT;x<225f->guides.state.mode=PerspectiveMode.THREE_POINT;else->guides.state.mode=PerspectiveMode.OFF}}else if(y in 55f..85f)guides.state.snap=!guides.state.snap else if(y in 85f..112f)guides.rulersVisible=!guides.rulersVisible else if(y in 112f..140f)guides.guidesVisible=!guides.guidesVisible else if(y in 140f..168f)guides.gridVisible=!guides.gridVisible else if(y in 168f..196f)guides.fieldGuideVisible=!guides.fieldGuideVisible else if(y in 196f..224f)guides.safeAreaVisible=!guides.safeAreaVisible}

    private fun makePaint()=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;alpha=(255*opacity).toInt();style=Paint.Style.STROKE;strokeWidth=max(1f,brushSize);strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;if(tool==Tool.ERASER)xfermode=PorterDuffXfermode(PorterDuff.Mode.CLEAR)}
    private fun label(c:Canvas,s:String,x:Float,y:Float,color:Int,size:Float){text.color=color;text.textSize=size;c.drawText(s,x,y,text)}
}
