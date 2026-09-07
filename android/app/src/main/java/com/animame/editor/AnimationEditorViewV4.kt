package com.animame.editor

import android.graphics.*
import android.view.MotionEvent
import kotlin.math.*

/** Production interaction layer: Smooth, Stabilizer and Anti-alias are independent. */
class AnimationEditorViewV4(context: android.content.Context) : AnimationEditorViewV5(context) {
    private var adjusting=false
    private var adjustKind=""
    private var smoothEnabled=true
    private val rawStroke=mutableListOf<Stabilizer.Sample>()

    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        if(rawStroke.isNotEmpty() && isDrawingTool()) drawRealtimePreview(canvas)
    }

    private fun get(name:String):Any?=runCatching{val f=AnimationEditorViewV5::class.java.getDeclaredField(name);f.isAccessible=true;f.get(this)}.getOrNull()
    private fun set(name:String,value:Any?){runCatching{val f=AnimationEditorViewV5::class.java.getDeclaredField(name);f.isAccessible=true;f.set(this,value)}}
    private fun invoke(name:String,vararg args:Any?):Any?=runCatching{val ms=AnimationEditorViewV5::class.java.declaredMethods.filter{it.name==name&&it.parameterTypes.size==args.size};val m=ms.firstOrNull{it.parameterTypes.indices.all{i->args[i]==null||it.parameterTypes[i].isAssignableFrom(args[i]!!::class.java)}}?:return null;m.isAccessible=true;m.invoke(this,*args)}.getOrNull()

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
        val color=(get("selectedColor") as? Int)?:Color.BLACK
        val s=StrokeData(brushId=selected?.id?:"preview",color=color,size=size,opacity=alpha,brushSettings=brush.copy(size=size,opacity=alpha,eraser=tool.endsWith("ERASER")),samples=previewSamples.map{StrokeSample(it.point.x,it.point.y,it.pressure,it.timeMs,it.tilt,it.orientation)}.toMutableList())
        c.save();c.clipRect(r);c.concat(m);drawContinuousStroke(c,s);c.restore()
    }

    private fun drawContinuousStroke(c:Canvas,s:StrokeData){
        if(s.samples.isEmpty())return
        val brush=get("brushSettings") as? BrushSettings ?: s.brushSettings
        val aa=(get("antiAlias") as? Boolean)?:true
        ContinuousStrokeRenderer.drawPressureAware(c,s.samples,s.size,s.opacity,brush.copy(size=s.size,opacity=s.opacity,eraser=s.brushSettings.eraser),s.color,smoothEnabled,aa)
    }

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.pointerCount>=2)return super.onTouchEvent(e)
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{
                if(routeUi(e.x,e.y,true))return true
                rawStroke.clear()
                if(isDrawingTool())rawStroke+=sample(e,docPoint(e.x,e.y))
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
        if(y<50f&&x<48f){set("menu",true);invalidate();return true}
        if(y<50f&&x>width-200f){invoke("openColorPopup");return true}
        if(y in 50f..150f){val f=((x-190f)/30f).roundToInt();if(f in 0 until ((get("document") as? AnimationDocument)?.duration?:0)){(get("document") as? AnimationDocument)?.currentFrame=f;invalidate();return true}}
        if(x in 70f..180f&&y>=155f&&y<473f){val i=((y-160f)/53f).toInt();if(i in 0..5){val cur=get("panel")?.toString();val names=listOf("OPTIONS","BRUSHES","LAYERS","TIMELINE","CAMERA","RULERS");if(cur?.endsWith(names[i])==true)set("panel",null)else set("panel",enumVal("Panel",i));invalidate();return true}}
        if(x<62f&&y>=155f){val i=((y-160f)/41f).toInt();if(i in 0 until 11){set("tool",enumVal("Tool",i));set("panel",enumVal("Panel",0));invalidate();return true}}
        val right=width-300f
        val p=get("panel")?.toString() ?: ""
        if(p.endsWith("BRUSHES")&&x>=right&&y>185f){val b=brushAt(y);if(b!=null){set("selectedBrush",b);set("brushSettings",b.defaults.copy());set("brushSize",b.defaults.size);set("opacity",b.defaults.opacity);invalidate();return true}}
        if(p.endsWith("OPTIONS")&&x>=right){
            if(y in 258f..284f){adjusting=down;adjustKind="size";updateAdjustment(x);return true}
            if(y in 284f..310f){adjusting=down;adjustKind="opacity";updateAdjustment(x);return true}
            if(y in 390f..414f){if(down)smoothEnabled=!smoothEnabled;invalidate();return true}
            if(y in 414f..438f){adjusting=down;adjustKind="stabilizer";updateAdjustment(x);return true}
            if(y in 438f..464f){if(down)set("antiAlias",!((get("antiAlias") as? Boolean)?:true));invalidate();return true}
            if(y in 190f..225f){invoke("openColorPopup");return true}
        }
        if(p.endsWith("TIMELINE")&&x>=right&&y in 220f..310f){(get("document") as? AnimationDocument)?.let{it.onion.enabled=!it.onion.enabled};invalidate();return true}
        if(p.endsWith("LAYERS")&&x>=right&&y in 185f..220f){invoke("addLayer");return true}
        if(p.endsWith("LAYERS")&&x>=right&&y>220f){invoke("selectLayer",y);return true}
        if(p.endsWith("RULERS")&&x>=right&&y>185f){val i=((y-206f)/24f).toInt();if(i in 0..8)set("ruler",i+1);invalidate();return true}
        if(p.endsWith("CAMERA")&&x>=right&&y in 285f..340f){invoke("resetCamera");invalidate();return true}
        return false
    }

    private fun updateAdjustment(x:Float){val t=((x-(width-285f))/255f).coerceIn(0f,1f);when(adjustKind){"size"->set("brushSize",1f+t*255f);"opacity"->set("opacity",t);"stabilizer"->set("stabilizer",t*100f)};invalidate()}
    private fun brushAt(y:Float):BrushPreset?{var yy=206f;for(f in BrushCatalog.families){yy+=18f;for(p in BrushCatalog.presets.filter{it.family==f}){if(y in yy-16f..yy+2f)return p;yy+=16f};yy+=4f};return null}
    private fun enumVal(n:String,i:Int):Any{val c=Class.forName("com.animame.editor.AnimationEditorViewV5$"+n);return c.enumConstants[i]!!}
}