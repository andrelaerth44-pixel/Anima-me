package com.animame.editor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.roundToInt

class AnimationEditorViewV4(context: android.content.Context) : AnimationEditorViewV5(context) {
    private var downX=0f; private var downY=0f; private var adjusting=false; private var adjustKind=""; private var smoothEnabled=true; private val rawStroke=mutableListOf<Stabilizer.Sample>()

    override fun onDraw(canvas: Canvas){super.onDraw(canvas);if(smoothEnabled)drawContinuousOverlay(canvas);drawCorrectedOptionLabels(canvas)}

    private fun drawContinuousOverlay(canvas: Canvas){
        val document=getPrivate("document") as? AnimationDocument ?: return
        val canvasRect=invokePrivate("canvasRect") as? RectF ?: return
        val matrix=invokePrivate("editorMatrix",RectF::class.java,canvasRect) as? Matrix ?: return
        canvas.save();canvas.clipRect(canvasRect);canvas.concat(matrix)
        document.layers.asReversed().filter{it.visible}.forEach{layer->layer.frameAt(document.currentFrame)?.strokes?.forEach{drawContinuousStroke(canvas,it)}}
        drawContinuousPreview(canvas);canvas.restore()
    }

    private fun drawContinuousPreview(canvas:Canvas){
        val list=getPrivate("samples") as? List<Stabilizer.Sample> ?: return
        if(list.isEmpty())return
        val brush=getPrivate("brushSettings") as? BrushSettings ?: return
        val size=getPrivate("brushSize") as? Float ?: brush.size; val alpha=getPrivate("opacity") as? Float ?: brush.opacity
        val toolName=getPrivate("tool")?.toString() ?: ""
        val stroke=StrokeData(brushId=(getPrivate("selectedBrush") as? BrushPreset)?.id ?: "preview",color=Color.BLACK,size=size,opacity=alpha,brushSettings=brush.copy(size=size,opacity=alpha,eraser=toolName.endsWith("ERASER")),samples=list.map{StrokeSample(it.point.x,it.point.y,it.pressure,it.timeMs,it.tilt)}.toMutableList())
        drawContinuousStroke(canvas,stroke)
    }

    private fun drawContinuousStroke(canvas:Canvas,stroke:StrokeData){
        val samples=stroke.samples;if(samples.isEmpty())return
        val bs=stroke.brushSettings.copy(size=stroke.size,opacity=stroke.opacity).normalized();var widthSum=0f;var alphaSum=0f;var weight=0f;var distance=0f
        for(i in samples.indices){val s=samples[i];val prev=samples.getOrNull(i-1);if(prev!=null)distance+=kotlin.math.hypot(s.x-prev.x,s.y-prev.y);val pressure=s.pressure.coerceIn(.05f,1.5f);widthSum+=bs.radiusFor(pressure,s.tilt);alphaSum+=bs.opacityFor(pressure,s.tilt,distance,kotlin.math.max(.001f,strokeLength(samples)));weight+=1f}
        val paint=Paint(if(bs.antialias&&(getPrivate("antiAlias") as? Boolean ?: true))Paint.ANTI_ALIAS_FLAG else 0).apply{style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND;strokeWidth=(widthSum/weight.coerceAtLeast(1f)).coerceAtLeast(.5f);alpha=(alphaSum/weight.coerceAtLeast(1f)*255f).roundToInt().coerceIn(0,255);color=stroke.color}
        if(bs.eraser)paint.xfermode=PorterDuffXfermode(PorterDuff.Mode.DST_OUT);ContinuousStrokeRenderer.draw(canvas,samples,paint,true);paint.xfermode=null
    }

    private fun drawCorrectedOptionLabels(canvas:Canvas){
        val panel=getPrivate("panel")?.toString() ?: return;if(!panel.endsWith("OPTIONS"))return
        val x=width-286f;val ys=334f;val yt=356f;val ya=378f
        val bg=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.FILL;color=Color.rgb(29,31,34)};canvas.drawRect(x-2f,ys-13f,width.toFloat(),ya+7f,bg)
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.LTGRAY;textSize=10f};canvas.drawText("SMOOTH  ${if(smoothEnabled)"ON" else "OFF"}",x,ys,p)
        val stabilizer=getPrivate("stabilizer") as? Float ?: 20f;val realtime=getPrivate("realTimeStabilizer") as? Boolean ?: true;canvas.drawText("STABILIZER  ${stabilizer.roundToInt()}%  ${if(realtime)"REAL TIME" else "AFTER"}",x,yt,p)
        val aa=getPrivate("antiAlias") as? Boolean ?: true;canvas.drawText("ANTI-ALIAS  ${if(aa)"ON" else "OFF"}",x,ya,p)
    }

    override fun onTouchEvent(event:MotionEvent):Boolean{
        if(event.pointerCount>=2)return false
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN->{downX=event.x;downY=event.y;if(routeUiTouch(event.x,event.y,true))return true;rawStroke.clear();val handled=super.onTouchEvent(event);captureAndProcessLive();return handled}
            MotionEvent.ACTION_MOVE->{if(adjusting){updateAdjustment(event.x,event.y);return true};val handled=super.onTouchEvent(event);captureAndProcessLive();return handled}
            MotionEvent.ACTION_UP->{if(adjusting){updateAdjustment(event.x,event.y);adjusting=false;adjustKind="";invalidate();return true};val handled=super.onTouchEvent(event);captureAndCommitProcessedStroke();if(abs(event.x-downX)<18f&&abs(event.y-downY)<18f)routeUiTouch(event.x,event.y,false);return handled}
            MotionEvent.ACTION_CANCEL->{adjusting=false;adjustKind="";rawStroke.clear();return super.onTouchEvent(event)}
        };return super.onTouchEvent(event)
    }

    private fun captureAndProcessLive(){val list=getPrivate("samples") as? MutableList<Stabilizer.Sample> ?: return;val latest=list.lastOrNull() ?: return;if(rawStroke.isEmpty()||rawStroke.last().timeMs!=latest.timeMs||rawStroke.last().point.x!=latest.point.x||rawStroke.last().point.y!=latest.point.y)rawStroke+=latest.copy(point=latest.point.copy());val processed=StrokeProcessingEngine.process(rawStroke,smoothEnabled,getPrivate("stabilizer") as? Float ?: 20f,getPrivate("realTimeStabilizer") as? Boolean ?: true);list.clear();list.addAll(processed);invalidate()}
    private fun captureAndCommitProcessedStroke(){val doc=getPrivate("document") as? AnimationDocument ?: return;val layerId=getPrivate("selectedLayerId") as? String ?: return;val layer=doc.layers.firstOrNull{it.id==layerId} ?: return;val frame=layer.frameAt(doc.currentFrame) ?: return;val stroke=frame.strokes.lastOrNull() ?: return;if(rawStroke.isEmpty())return;stroke.samples.clear();stroke.samples.addAll(StrokeProcessingEngine.process(rawStroke,smoothEnabled,getPrivate("stabilizer") as? Float ?: 20f,getPrivate("realTimeStabilizer") as? Boolean ?: true).map{StrokeSample(it.point.x,it.point.y,it.pressure,it.timeMs,it.tilt,it.orientation)});rawStroke.clear();invalidate()}

    private fun routeUiTouch(x:Float,y:Float,down:Boolean):Boolean{
        if(x<=66f&&y>=54f&&y<height-112f){val i=((y-57f)/41f).toInt();if(i in 0..9){setPrivate("tool",enumValue("com.animame.editor.AnimationEditorViewV5\$Tool",i));invalidate();return true}}
        if(y in 5f..47f&&x>=186f&&x<675f){val i=((x-190f)/78f).toInt();if(i in 0..5){setPrivate("panel",enumValue("com.animame.editor.AnimationEditorViewV5\$Panel",i));invalidate();return true}}
        if(getPrivate("panel")?.toString()?.endsWith("BRUSHES")==true&&x>=width-300f&&y>=96f&&y<height-112f){val chosen=brushAtRow(y);if(chosen!=null){setPrivate("selectedBrush",chosen);setPrivate("brushSettings",chosen.defaults.copy());setPrivate("brushSize",chosen.defaults.size);setPrivate("opacity",chosen.defaults.opacity);invalidate();return true}}
        if(getPrivate("panel")?.toString()?.endsWith("OPTIONS")==true&&x>=width-300f){if(y in 132f..158f){adjusting=down;adjustKind="size";updateAdjustment(x,y);return true};if(y in 158f..184f){adjusting=down;adjustKind="opacity";updateAdjustment(x,y);return true};if(y in 316f..344f){if(down)smoothEnabled=!smoothEnabled;invalidate();return true};if(y in 344f..370f){adjusting=down;adjustKind="stabilizer";updateAdjustment(x,y);return true};if(y in 370f..398f){if(down){setPrivate("antiAlias",!(getPrivate("antiAlias") as? Boolean ?: true));invalidate()};return true}}
        if(getPrivate("panel")?.toString()?.endsWith("TIMELINE")==true&&x>=width-300f&&y in 120f..190f){adjusting=down;adjustKind="onion";updateAdjustment(x,y);return true};return false
    }

    private fun updateAdjustment(x:Float,y:Float){val left=width-285f;val right=width-30f;val t=((x-left)/(right-left)).coerceIn(0f,1f);when(adjustKind){"size"->setPrivate("brushSize",1f+t*255f);"opacity"->setPrivate("opacity",t);"stabilizer"->setPrivate("stabilizer",t*100f);"onion"->{val doc=getPrivate("document") as? AnimationDocument ?: return;doc.onion.opacity=(t*100f).roundToInt()}};invalidate()}
    private fun brushAtRow(y:Float):BrushPreset?{var rowY=108f;for(family in BrushCatalog.families){rowY+=18f;for(p in BrushCatalog.presets.filter{it.family==family}){if(y>=rowY-13f&&y<rowY+7f)return p;rowY+=16f};rowY+=4f};return null}
    private fun enumValue(className:String,index:Int):Any{val c=Class.forName(className);return (c.enumConstants?:error("No enum constants for $className"))[index]}
    private fun getPrivate(name:String):Any?{val f=AnimationEditorViewV5::class.java.getDeclaredField(name);f.isAccessible=true;return f.get(this)}
    private fun setPrivate(name:String,value:Any?){val f=AnimationEditorViewV5::class.java.getDeclaredField(name);f.isAccessible=true;f.set(this,value)}
    private fun invokePrivate(name:String,vararg args:Any?):Any?{val methods=AnimationEditorViewV5::class.java.declaredMethods.filter{it.name==name&&it.parameterTypes.size==args.size};val method=methods.firstOrNull{m->m.parameterTypes.indices.all{i->args[i]==null||m.parameterTypes[i].isAssignableFrom(args[i]!!::class.java)}}?:return null;method.isAccessible=true;return method.invoke(this,*args)}
    private fun strokeLength(samples:List<StrokeSample>):Float{var d=0f;for(i in 1 until samples.size)d+=kotlin.math.hypot(samples[i].x-samples[i-1].x,samples[i].y-samples[i-1].y);return d.coerceAtLeast(.001f)}
}