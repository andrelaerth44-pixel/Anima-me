package com.animame.editor

import android.graphics.*
import android.view.MotionEvent
import kotlin.math.*

/** Production interaction layer: Smooth, Stabilizer and Anti-alias are independent. */
class AnimationEditorViewV4(context: android.content.Context) : AnimationEditorViewV5(context) {
    private var downX=0f
    private var downY=0f
    private var adjusting=false
    private var adjustKind=""
    private var smoothEnabled=true
    private val rawStroke=mutableListOf<Stabilizer.Sample>()

    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        if(rawStroke.isNotEmpty() && isDrawingTool()) drawRealtimePreview(canvas)
        drawCorrectedLabels(canvas)
    }

    private fun get(name:String):Any?=runCatching{
        val f=AnimationEditorViewV5::class.java.getDeclaredField(name);f.isAccessible=true;f.get(this)
    }.getOrNull()
    private fun set(name:String,value:Any?){runCatching{
        val f=AnimationEditorViewV5::class.java.getDeclaredField(name);f.isAccessible=true;f.set(this,value)
    }}
    private fun invoke(name:String,vararg args:Any?):Any?=runCatching{
        val ms=AnimationEditorViewV5::class.java.declaredMethods.filter{it.name==name&&it.parameterTypes.size==args.size}
        val m=ms.firstOrNull{it.parameterTypes.indices.all{i->args[i]==null||it.parameterTypes[i].isAssignableFrom(args[i]!!::class.java)}}?:return null
        m.isAccessible=true;m.invoke(this,*args)
    }.getOrNull()

    /** Smooth is applied to the raw live input while Stabilizer remains a separate stage. */
    private fun drawRealtimePreview(c:Canvas){
        val r=invoke("canvasRect") as? RectF ?: return
        val m=invoke("editorMatrix",r) as? Matrix ?: return
        val brush=get("brushSettings") as? BrushSettings ?: return
        val size=(get("brushSize") as? Float)?:brush.size
        val alpha=(get("opacity") as? Float)?:brush.opacity
        val tool=get("tool")?.toString() ?: ""
        val selected=get("selectedBrush") as? BrushPreset
        val previewSamples=StrokeProcessingEngine.process(rawStroke,smoothEnabled,0f,true)
        if(previewSamples.isEmpty())return
        val s=StrokeData(
            brushId=selected?.id?:"preview",
            color=Color.BLACK,
            size=size,
            opacity=alpha,
            brushSettings=brush.copy(size=size,opacity=alpha,eraser=tool.endsWith("ERASER")),
            samples=previewSamples.map{StrokeSample(it.point.x,it.point.y,it.pressure,it.timeMs,it.tilt,it.orientation)}.toMutableList()
        )
        c.save();c.clipRect(r);c.concat(m)
        drawContinuousStroke(c,s)
        c.restore()
    }

    private fun drawContinuousStroke(c:Canvas,s:StrokeData){
        if(s.samples.isEmpty())return
        val brush=get("brushSettings") as? BrushSettings ?: s.brushSettings
        val aa=(get("antiAlias") as? Boolean)?:true
        ContinuousStrokeRenderer.drawPressureAware(
            canvas=c,
            samples=s.samples,
            baseSize=s.size,
            baseOpacity=s.opacity,
            settings=brush.copy(size=s.size,opacity=s.opacity,eraser=s.brushSettings.eraser),
            color=s.color,
            smooth=smoothEnabled,
            antiAlias=aa
        )
    }

    private fun drawCorrectedLabels(c:Canvas){
        if(get("panel")?.toString()?.endsWith("OPTIONS")!=true)return
        val x=width-286f
        val bg=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.FILL;color=Color.rgb(29,31,34)}
        c.drawRect(x-2f,321f,width.toFloat(),391f,bg)
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.LTGRAY;textSize=10f}
        c.drawText("SMOOTH  ${if(smoothEnabled)"ON" else "OFF"}",x,334f,p)
        val st=(get("stabilizer") as? Float)?:20f;val rt=(get("realTimeStabilizer") as? Boolean)?:true
        c.drawText("STABILIZER  ${st.roundToInt()}%  ${if(rt)"REAL TIME" else "AFTER"}",x,356f,p)
        val aa=(get("antiAlias") as? Boolean)?:true
        c.drawText("ANTI-ALIAS  ${if(aa)"ON" else "OFF"}",x,378f,p)
    }

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.pointerCount>=2)return super.onTouchEvent(e)
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{
                downX=e.x;downY=e.y
                if(routeUi(e.x,e.y,true))return true
                rawStroke.clear()
                val p=docPoint(e.x,e.y)
                if(isDrawingTool())rawStroke+=sample(e,p)
                val handled=super.onTouchEvent(e);invalidate();return handled
            }
            MotionEvent.ACTION_MOVE->{
                if(adjusting){updateAdjustment(e.x);return true}
                if(isDrawingTool())rawStroke+=sample(e,docPoint(e.x,e.y))
                val handled=super.onTouchEvent(e);invalidate();return handled
            }
            MotionEvent.ACTION_UP->{
                if(adjusting){updateAdjustment(e.x);adjusting=false;adjustKind="";invalidate();return true}
                val handled=super.onTouchEvent(e)
                if(isDrawingTool()&&rawStroke.size>1)commitProcessed()
                rawStroke.clear();invalidate();return handled
            }
            MotionEvent.ACTION_CANCEL->{adjusting=false;adjustKind="";rawStroke.clear();return super.onTouchEvent(e)}
        }
        return super.onTouchEvent(e)
    }

    private fun isDrawingTool():Boolean{val t=get("tool")?.toString() ?: "";return t.endsWith("BRUSH")||t.endsWith("PENCIL")||t.endsWith("ERASER")}
    private fun docPoint(x:Float,y:Float):PointF{val r=invoke("canvasRect") as? RectF?:return PointF(x,y);val m=invoke("editorMatrix",r) as? Matrix?:return PointF(x,y);val inv=Matrix();if(!m.invert(inv))return PointF(x,y);val a=floatArrayOf(x,y);inv.mapPoints(a);return PointF(a[0],a[1])}
    private fun sample(e:MotionEvent,p:PointF)=Stabilizer.Sample(PointF(p.x,p.y),e.pressure.coerceIn(.05f,1.5f),e.eventTime,e.tilt,e.orientation)

    private fun commitProcessed(){
        val d=get("document") as? AnimationDocument?:return
        val id=get("selectedLayerId") as? String?:return
        val layer=d.layers.firstOrNull{it.id==id}?:return
        val frame=layer.frameAt(d.currentFrame)?:return
        val stroke=frame.strokes.lastOrNull()?:return
        val st=(get("stabilizer") as? Float)?:20f
        val rt=(get("realTimeStabilizer") as? Boolean)?:true
        val processed=StrokeProcessingEngine.process(rawStroke,smoothEnabled,st,rt)
        stroke.samples.clear();stroke.samples.addAll(processed.map{StrokeSample(it.point.x,it.point.y,it.pressure,it.timeMs,it.tilt,it.orientation)})
    }

    private fun routeUi(x:Float,y:Float,down:Boolean):Boolean{
        if(x<66f&&y>=54f&&y<height-112f){val i=((y-57f)/41f).toInt();if(i in 0 until 11){set("tool",enumVal("Tool",i));set("panel",enumVal("Panel",0));invalidate();return true}}
        if(y in 5f..47f&&x>=186f&&x<675f){val i=((x-190f)/78f).toInt();if(i in 0 until 6){set("panel",enumVal("Panel",i));invalidate();return true}}
        val right=width-300f
        if(get("panel")?.toString()?.endsWith("BRUSHES")==true&&x>=right&&y>=96f&&y<height-112f){val b=brushAt(y);if(b!=null){set("selectedBrush",b);set("brushSettings",b.defaults.copy());set("brushSize",b.defaults.size);set("opacity",b.defaults.opacity);invalidate();return true}}
        if(get("panel")?.toString()?.endsWith("OPTIONS")==true&&x>=right){
            if(y in 132f..158f){adjusting=down;adjustKind="size";updateAdjustment(x);return true}
            if(y in 158f..184f){adjusting=down;adjustKind="opacity";updateAdjustment(x);return true}
            if(y in 316f..344f){if(down)smoothEnabled=!smoothEnabled;invalidate();return true}
            if(y in 344f..370f){adjusting=down;adjustKind="stabilizer";updateAdjustment(x);return true}
            if(y in 370f..398f){if(down)set("antiAlias",!((get("antiAlias") as? Boolean)?:true));invalidate();return true}
        }
        if(get("panel")?.toString()?.endsWith("TIMELINE")==true&&x>=right&&y in 120f..190f){adjusting=down;adjustKind="onion";updateAdjustment(x);return true}
        return false
    }

    private fun updateAdjustment(x:Float){val t=((x-(width-285f))/255f).coerceIn(0f,1f);when(adjustKind){"size"->set("brushSize",1f+t*255f);"opacity"->set("opacity",t);"stabilizer"->set("stabilizer",t*100f);"onion"->(get("document") as? AnimationDocument)?.let{it.onion.opacity=(t*100f).roundToInt()}};invalidate()}
    private fun brushAt(y:Float):BrushPreset?{var yy=108f;for(f in BrushCatalog.families){yy+=18f;for(p in BrushCatalog.presets.filter{it.family==f}){if(y in yy-16f..yy+2f)return p;yy+=16f};yy+=4f};return null}
    private fun enumVal(n:String,i:Int):Any{val c=Class.forName("com.animame.editor.AnimationEditorViewV5$"+n);return c.enumConstants[i]!!}
    private fun strokeLength(s:List<StrokeSample>):Float{var d=0f;for(i in 1 until s.size)d+=hypot(s[i].x-s[i-1].x,s[i].y-s[i-1].y);return max(.001f,d)}
}