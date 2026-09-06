package com.animame.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class AnimationEditorViewV3(context: Context) : View(context) {
    private enum class Mode { HOME, EDITOR }
    private enum class Panel { OPTIONS, BRUSHES, LAYERS, TIMELINE, CAMERA, RULERS }

    private var mode = Mode.HOME
    private var panel: Panel? = Panel.OPTIONS
    private var dialog = false
    private var project: AnimationProject? = null
    private var fps = 24
    private var cw = 1280
    private var ch = 720
    private var margin = 0
    private var projectName = "Untitled"

    private var selectedBrush = BrushCatalog.presets.first()
    private var brushSize = 12f
    private var opacity = 1f
    private var stabilizer = 55f
    private var realTimeStabilizer = true
    private var antiAlias = true
    private var ruler = 0
    private var symmetry = 6

    private val viewport = CanvasViewport()
    private val settings get() = EditorSettingsStore.current

    // Animation camera is separate from the canvas viewport.
    private var cameraX = 0f
    private var cameraY = 0f
    private var cameraScale = 1f
    private var cameraRotation = 0f

    private val strokes = mutableListOf<Pair<Path, Paint>>()
    private val samples = mutableListOf<Stabilizer.Sample>()
    private var current: Path? = null
    private var lastPoint: PointF? = null
    private var lastTime = 0L
    private val ui = Paint(Paint.ANTI_ALIAS_FLAG)

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(c: Canvas) {
        if (mode == Mode.HOME) drawHome(c) else drawEditor(c)
        if (dialog) drawDialog(c)
    }

    private fun bg(c: Canvas) {
        ui.style = Paint.Style.FILL
        ui.color = Color.rgb(16,17,19)
        c.drawRect(0f,0f,width.toFloat(),height.toFloat(),ui)
    }

    private fun drawHome(c: Canvas) {
        bg(c)
        ui.color = Color.rgb(27,29,32); c.drawRect(0f,0f,width.toFloat(),54f,ui)
        text(c,"RoughAnimator",22f,35f,Color.WHITE,18f)
        text(c,"Sort by date",width-115f,35f,Color.LTGRAY,10f)
        if(ProjectStore.projects.isEmpty()) text(c,"No projects",width/2f-35f,height/2f,Color.LTGRAY,14f)
        else {
            var y=80f
            ProjectStore.projects.forEach { p ->
                ui.color=Color.rgb(36,39,43); c.drawRoundRect(24f,y,300f,y+150f,8f,8f,ui)
                text(c,p.name,40f,y+122f,Color.WHITE,12f); text(c,"${p.fps} fps",40f,y+141f,Color.GRAY,9f); y+=170f
            }
        }
        ui.color=Color.rgb(52,55,60); c.drawRoundRect(width-190f,height-76f,width-24f,height-26f,10f,10f,ui)
        text(c,"New project",width-154f,height-45f,Color.WHITE,12f)
    }

    private fun drawDialog(c: Canvas) {
        ui.color=Color.argb(235,10,11,13); c.drawRect(0f,0f,width.toFloat(),height.toFloat(),ui)
        val l=width/2f-250f; val t=height/2f-195f
        ui.color=Color.rgb(38,40,44); c.drawRoundRect(l,t,l+500f,t+390f,12f,12f,ui)
        text(c,"New project",l+24f,t+35f,Color.WHITE,17f)
        field(c,"New project name",projectName,l+24f,t+68f)
        field(c,"Frames per second",fps.toString(),l+24f,t+126f)
        field(c,"Camera size","${cw} x ${ch}",l+24f,t+184f)
        field(c,"Margins",margin.toString(),l+24f,t+242f)
        field(c,"Canvas size","${cw+margin*2} x ${ch+margin*2}",l+24f,t+300f)
        text(c,"Cancel",l+300f,t+356f,Color.LTGRAY,11f); text(c,"New project",l+395f,t+356f,Color.WHITE,11f)
    }

    private fun field(c:Canvas,a:String,b:String,x:Float,y:Float){ text(c,a,x,y,Color.GRAY,8f); text(c,b,x,y+22f,Color.WHITE,12f); ui.color=Color.rgb(66,69,74); c.drawRect(x,y+29f,x+450f,y+30f,ui) }

    private fun drawEditor(c:Canvas) {
        bg(c); val w=width.toFloat(); val h=height.toFloat()
        ui.color=Color.rgb(27,29,32); c.drawRect(0f,0f,w,50f,ui)
        text(c,"ANIMA-ME",14f,32f,Color.WHITE,15f); text(c,project?.name?:"Untitled",116f,32f,Color.LTGRAY,12f); text(c,"${project?.fps?:24} FPS",w-116f,32f,Color.LTGRAY,12f)
        val r=canvasRect(); ui.color=Color.WHITE; c.drawRect(r,ui)
        c.save(); c.clipRect(r)
        c.concat(viewport.matrix(r.centerX(),r.centerY()))
        c.translate(cameraX,cameraY); c.scale(cameraScale,cameraScale,r.centerX(),r.centerY()); c.rotate(cameraRotation,r.centerX(),r.centerY())
        strokes.forEach{c.drawPath(it.first,it.second)}; current?.let{c.drawPath(it,brushPaint())}; c.restore()
        drawRuler(c,r); tabs(c); rail(c); if(panel!=null) drawPanel(c,w-300f,h); timeline(c,h)
    }

    private fun tabs(c:Canvas){ listOf("OPTIONS","BRUSHES","LAYERS","TIMELINE","CAMERA","RULERS").forEachIndexed{i,n->val x=190f+i*78f;ui.color=if(panel==Panel.values()[i])Color.rgb(68,73,81)else Color.rgb(43,46,50);c.drawRoundRect(x,9f,x+74f,41f,7f,7f,ui);text(c,n,x+7f,29f,Color.WHITE,8f)} }
    private fun rail(c:Canvas){ui.color=Color.rgb(31,33,36);c.drawRect(0f,50f,62f,height-112f,ui);listOf("BR","PE","ER","LA","FI","PI","MV","TR","LN","RE","EL").forEachIndexed{i,n->text(c,n,19f,78f+i*41f,Color.WHITE,9f)}}

    private fun drawPanel(c:Canvas,x:Float,h:Float){ui.color=Color.rgb(29,31,34);c.drawRect(x,50f,width.toFloat(),h-112f,ui);text(c,when(panel){Panel.OPTIONS->"TOOL OPTIONS";Panel.BRUSHES->"BRUSH LIBRARY";Panel.LAYERS->"LAYERS";Panel.TIMELINE->"TIMELINE";Panel.CAMERA->"CAMERA / TRANSFORM";Panel.RULERS->"RULERS";null->""},x+14f,78f,Color.WHITE,13f);when(panel){Panel.OPTIONS->options(c,x+14f);Panel.BRUSHES->brushes(c,x+14f);Panel.LAYERS->layers(c,x+14f);Panel.TIMELINE->timelinePanel(c,x+14f);Panel.CAMERA->camera(c,x+14f);Panel.RULERS->rulers(c,x+14f);null->Unit}}
    private fun options(c:Canvas,x:Float){text(c,"BRUSH  ${selectedBrush.name}",x,108f,Color.WHITE,12f);text(c,"SIZE  ${brushSize.roundToInt()} px",x,138f,Color.LTGRAY,11f);text(c,"OPACITY  ${(opacity*100).roundToInt()}%",x,166f,Color.LTGRAY,11f);text(c,"SPACING  2%",x,194f,Color.LTGRAY,11f);text(c,"SMOOTHING  ${stabilizer.roundToInt()}%",x,222f,Color.LTGRAY,11f);text(c,"STABILIZER  ${if(realTimeStabilizer)"REAL TIME" else "AFTER"}",x,250f,Color.LTGRAY,10f);text(c,"ANTI-ALIAS  ${if(antiAlias)"ON" else "OFF"}",x,274f,Color.LTGRAY,10f);text(c,"PRESSURE SENSITIVITY  ON",x,298f,Color.LTGRAY,10f);text(c,"VIEW ZOOM  ${(viewport.scale*100).roundToInt()}%",x,326f,Color.LTGRAY,10f);text(c,"VIEW ROTATION  ${viewport.rotation.roundToInt()}°",x,350f,Color.LTGRAY,10f);text(c,"PINCH ZOOM ${if(settings.allowPinchZoom)"ON" else "OFF"}",x,376f,Color.GRAY,9f);text(c,"PINCH ROTATION ${if(settings.allowPinchRotation)"ON" else "OFF"}",x,396f,Color.GRAY,9f)}
    private fun brushes(c:Canvas,x:Float){var y=108f;BrushCatalog.families.forEach{f->if(y<height-135){text(c,f,x,y,Color.WHITE,10f);y+=18f;BrushCatalog.presets.filter{it.family==f}.forEach{p->if(y<height-135){text(c,if(p.id==selectedBrush.id)"• ${p.name}" else "  ${p.name}",x+6f,y,if(p.id==selectedBrush.id)Color.WHITE else Color.GRAY,9f);y+=16f}};y+=4f}}}
    private fun layers(c:Canvas,x:Float){listOf("+ NEW LAYER","EYE   Layer 3","EYE   Layer 2","EYE   Layer 1","EYE   Background","LOCK   OPACITY   BLEND").forEachIndexed{i,s->text(c,s,x,108f+i*28f,Color.LTGRAY,10f)}}
    private fun timelinePanel(c:Canvas,x:Float){text(c,"ONION SKIN   ON",x,108f,Color.LTGRAY,10f);text(c,"PREVIOUS 2   NEXT 2",x,136f,Color.LTGRAY,10f);text(c,"${project?.fps?:24} FPS",x,164f,Color.LTGRAY,10f)}
    private fun camera(c:Canvas,x:Float){text(c,"POSITION  ${cameraX.roundToInt()}, ${cameraY.roundToInt()}",x,108f,Color.LTGRAY,10f);text(c,"SCALE  ${(cameraScale*100).roundToInt()}%",x,136f,Color.LTGRAY,10f);text(c,"ROTATION  ${cameraRotation.roundToInt()}°",x,164f,Color.LTGRAY,10f);text(c,"RESET CAMERA",x,196f,Color.WHITE,10f);text(c,"VIEWPORT  ${(viewport.scale*100).roundToInt()}% / ${viewport.rotation.roundToInt()}°",x,224f,Color.GRAY,9f)}
    private fun rulers(c:Canvas,x:Float){listOf("STRAIGHT RULER","CIRCULAR RULER","ELLIPSE RULER","RADIAL RULER","MIRROR RULER","KALEIDOSCOPE RULER","ROTATION RULER","ARRAY RULER","PERSPECTIVE ARRAY RULER").forEachIndexed{i,s->text(c,if(ruler==i+1)"• $s" else "  $s",x,108f+i*24f,if(ruler==i+1)Color.WHITE else Color.LTGRAY,9f)}}

    private fun drawRuler(c:Canvas,r:RectF){if(ruler==0)return;ui.style=Paint.Style.STROKE;ui.isAntiAlias=true;ui.color=Color.rgb(105,140,175);ui.strokeWidth=1f;when(ruler){1->c.drawLine(r.left,r.centerY(),r.right,r.centerY(),ui);2->c.drawCircle(r.centerX(),r.centerY(),min(r.width(),r.height())*.32f,ui);3->c.drawOval(r.centerX()-r.width()*.3f,r.centerY()-r.height()*.2f,r.centerX()+r.width()*.3f,r.centerY()+r.height()*.2f,ui);4->repeat(24){i->val a=i*PI/12;c.drawLine(r.centerX(),r.centerY(),r.centerX()+cos(a).toFloat()*r.width(),r.centerY()+sin(a).toFloat()*r.height(),ui)};5->c.drawLine(r.centerX(),r.top,r.centerX(),r.bottom,ui);6,7->repeat(symmetry){i->c.save();c.rotate(i*360f/symmetry,r.centerX(),r.centerY());c.drawLine(r.centerX(),r.centerY(),r.centerX(),r.top,ui);c.restore()};8->repeat(4){i->c.drawLine(r.left+r.width()*(i+1)/5f,r.top,r.left+r.width()*(i+1)/5f,r.bottom,ui)};9->{val vx=r.centerX();val vy=r.top-80f;repeat(9){i->val x=r.left+r.width()*i/8f;c.drawLine(vx,vy,x,r.bottom,ui)}}};ui.style=Paint.Style.FILL}
    private fun timeline(c:Canvas,h:Float){ui.color=Color.rgb(27,29,32);c.drawRect(0f,h-112f,width.toFloat(),h,ui);text(c,"LAYERS",12f,h-82f,Color.LTGRAY,10f);text(c,"Layer 1    1  2  3  4  5  6  7  8",70f,h-82f,Color.WHITE,10f);text(c,"|<   <   PLAY   >   >|",width-175f,h-38f,Color.WHITE,10f)}

    private fun brushPaint(pressure:Float=1f)=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;color=Color.BLACK;isAntiAlias=antiAlias;strokeWidth=(brushSize*pressure.coerceIn(.2f,1.5f)).coerceAtLeast(.5f);strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;alpha=(opacity*255f).roundToInt().coerceIn(1,255)}
    private fun canvasRect()=RectF(72f,60f,width.toFloat()-(if(panel==null)72f else 310f),height.toFloat()-122f)

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(dialog)return dialogTouch(e)
        if(mode==Mode.HOME)return homeTouch(e)
        if(e.pointerCount>=2){if(current!=null)finishStroke();when(e.actionMasked){MotionEvent.ACTION_POINTER_DOWN->viewport.beginGesture(e);MotionEvent.ACTION_MOVE->{viewport.updateGesture(e);invalidate()};MotionEvent.ACTION_POINTER_UP->viewport.endGesture();MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->viewport.endGesture()};return true}
        when(e.actionMasked){MotionEvent.ACTION_DOWN->{if(handleUi(e.x,e.y))return true;if(canvasRect().contains(e.x,e.y))beginStroke(e);return true};MotionEvent.ACTION_MOVE->{if(current!=null)addSample(e);return true};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(current!=null)finishStroke();return true}}
        return true
    }

    private fun homeTouch(e:MotionEvent):Boolean{if(e.actionMasked!=MotionEvent.ACTION_UP)return true;if(e.x>width-210f&&e.y>height-100f){dialog=true;invalidate();return true};if(ProjectStore.projects.isNotEmpty()&&e.y in 80f..230f){project=ProjectStore.projects.first();mode=Mode.EDITOR;viewport.reset();resetCamera();invalidate()};return true}

    private fun handleUi(x:Float,y:Float):Boolean{
        if(y<48f&&x>=190f){val i=((x-190f)/78f).toInt();if(i in Panel.values().indices){panel=if(panel==Panel.values()[i])null else Panel.values()[i];invalidate();return true}}
        if(x<62f&&y in 50f..height-112f){panel=Panel.OPTIONS;invalidate();return true}
        val right=width.toFloat()-300f
        if(panel==Panel.BRUSHES&&x>=right&&y>90f){selectBrush(y);return true}
        if(panel==Panel.RULERS&&x>=right&&y>90f){val i=((y-108f)/24f).toInt();if(i in 0..8)ruler=i+1;panel=Panel.OPTIONS;invalidate();return true}
        if(panel==Panel.CAMERA&&x>=right&&y in 175f..215f){resetCamera();invalidate();return true}
        if(panel==Panel.OPTIONS&&x>=right&&y in 205f..265f){realTimeStabilizer=!realTimeStabilizer;invalidate();return true}
        if(panel==Panel.OPTIONS&&x>=right&&y in 265f..310f){antiAlias=!antiAlias;invalidate();return true}
        return false
    }

    private fun beginStroke(e:MotionEvent){samples.clear();current=Path();val p=toDoc(e.x,e.y);val now=e.eventTime;samples+=Stabilizer.Sample(p,e.pressure.coerceIn(.05f,1.5f),now);lastPoint=p;lastTime=now;current!!.moveTo(p.x,p.y);invalidate()}

    private fun addSample(e:MotionEvent){if(!canvasRect().contains(e.x,e.y))return;for(i in 0 until e.historySize){samples+=Stabilizer.Sample(toDoc(e.getHistoricalX(i),e.getHistoricalY(i)),e.getHistoricalPressure(i).coerceIn(.05f,1.5f),e.getHistoricalEventTime(i))};val p=toDoc(e.x,e.y);samples+=Stabilizer.Sample(p,e.pressure.coerceIn(.05f,1.5f),e.eventTime)
        val prev=lastPoint;val dt=e.eventTime-lastTime;val speed=if(prev!=null&&dt>0)Stabilizer.distance(prev,p)*1000f/dt else 0f;val strength=Stabilizer.adaptiveStrength(speed,stabilizer*.45f,stabilizer);current=Stabilizer.smoothSamples(samples,if(realTimeStabilizer)strength else stabilizer,realTimeStabilizer);lastPoint=p;lastTime=e.eventTime;invalidate()}

    private fun finishStroke(){if(samples.isEmpty()){current=null;return};val path=Stabilizer.smoothSamples(samples,stabilizer,false);val pressure=samples.map{it.pressure}.average().toFloat();strokes+=path to brushPaint(pressure);current=null;samples.clear();lastPoint=null;invalidate()}
    private fun toDoc(x:Float,y:Float):PointF{val r=canvasRect();return viewport.inversePoint(x,y,r.centerX(),r.centerY())}
    private fun selectBrush(y:Float){var yy=108f;for(f in BrushCatalog.families){yy+=18f;for(p in BrushCatalog.presets.filter{it.family==f}){if(y in yy-16f..yy+2f){selectedBrush=p;panel=Panel.OPTIONS;invalidate();return};yy+=16f};yy+=4f}}
    private fun resetCamera(){cameraX=0f;cameraY=0f;cameraScale=1f;cameraRotation=0f}

    private fun dialogTouch(e:MotionEvent):Boolean{if(e.actionMasked!=MotionEvent.ACTION_UP)return true;val l=width/2f-250f;val t=height/2f-195f;if(e.x>l+380f&&e.y>t+325f){project=ProjectStore.create(projectName,fps,cw,ch,margin);mode=Mode.EDITOR;dialog=false;viewport.reset();resetCamera();invalidate();return true};if(e.x>l+270f&&e.x<l+370f&&e.y>t+325f){dialog=false;invalidate();return true};if(e.y in t+95f..t+150f){fps=when(fps){12->24;24->30;30->60;else->12};invalidate()};if(e.y in t+150f..t+210f){val sizes=arrayOf(1280 to 720,1920 to 1080,2048 to 1152,1080 to 1080);val idx=sizes.indexOfFirst{it.first==cw&&it.second==ch};val n=sizes[(idx+1).mod(sizes.size)];cw=n.first;ch=n.second;invalidate()};return true}
    private fun text(c:Canvas,s:String,x:Float,y:Float,color:Int,size:Float){ui.style=Paint.Style.FILL;ui.color=color;ui.textSize=size;ui.typeface=Typeface.create("sans",Typeface.NORMAL);c.drawText(s,x,y,ui)}
}
