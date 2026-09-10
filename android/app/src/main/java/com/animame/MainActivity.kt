package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.BrushDefaults
import com.animame.editor.BrushEngine
import com.animame.editor.DrawingFrame
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample

class MainActivity : Activity() {
    private lateinit var editor: EditorSurface
    private lateinit var timelineLabel: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi() }
    override fun onResume() { super.onResume(); if (::editor.isInitialized) { editor.refreshTheme(); editor.invalidate() } }
    private fun buildUi() {
        val root = FrameLayout(this); editor = EditorSurface(); root.addView(editor, FrameLayout.LayoutParams(-1, -1))
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,6,8,6); setBackgroundColor(Color.rgb(30,34,39)) }
        top.addView(button("⚙",56){startActivity(Intent(this@MainActivity,SettingsActivity::class.java))})
        top.addView(button("Pincel",96){editor.tool=Tool.BRUSH;editor.invalidate()})
        top.addView(button("Borracha",105){editor.tool=Tool.ERASER;editor.invalidate()})
        top.addView(button("Tamanho",100){editor.adjustSize()}); top.addView(button("Opacidade",105){editor.adjustOpacity()}); top.addView(button("Camada +",105){editor.addLayer()})
        root.addView(top,FrameLayout.LayoutParams(-1,66))
        timelineLabel=TextView(this).apply{setTextColor(Color.WHITE);textSize=15f;setPadding(12,4,12,4)}
        val timeline=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.rgb(24,27,31));addView(timelineLabel,LinearLayout.LayoutParams(180,54));addView(button("◀",54){editor.previousFrame()});addView(button("＋",54){editor.insertFrame()});addView(button("⧉",54){editor.duplicateFrame()});addView(button("−",54){editor.deleteFrame()});addView(button("▶",54){editor.nextFrame()});addView(button("Onion",78){editor.toggleOnion()});addView(button("Limpar",80){editor.clearCurrentFrame()})}
        val p=FrameLayout.LayoutParams(-1,58);p.gravity=android.view.Gravity.BOTTOM;root.addView(timeline,p);setContentView(root);editor.timelineLabel=timelineLabel;editor.refreshTimeline()
    }
    private fun button(label:String,width:Int,action:()->Unit)=Button(this).apply{text=label;textSize=12f;setOnClickListener{action()};layoutParams=LinearLayout.LayoutParams(width,54)}
    private enum class Tool{BRUSH,ERASER}
    private inner class EditorSurface:View(this@MainActivity){
        private val document=AnimationDocument(duration=1); private val currentSamples=mutableListOf<StrokeSample>(); private var previewStamps=emptyList<BrushEngine.Stamp>(); private var accent=ThemeColorStore.DEFAULT; private var brushSize=12f; private var brushOpacity=1f; private var drawing=false; private var onionEnabled=true; private var activeLayerId:String=document.activeLayer.id; var tool=Tool.BRUSH; var timelineLabel:TextView?=null
        private val bg=Paint(Paint.ANTI_ALIAS_FLAG);private val panel=Paint(Paint.ANTI_ALIAS_FLAG);private val paper=Paint(Paint.ANTI_ALIAS_FLAG);private val stampPaint=Paint(Paint.ANTI_ALIAS_FLAG);private val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=25f};private val sub=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.LTGRAY;textSize=15f}
        init{refreshTheme();isFocusable=true}
        fun refreshTheme(){accent=ThemeColorStore.get(this@MainActivity);bg.color=Color.rgb(18,20,23);panel.color=blend(accent,Color.rgb(30,34,39),.82f);paper.color=Color.rgb(245,245,245)}
        override fun onDraw(c:Canvas){c.drawColor(bg.color);val top=66f;val bottom=height-58f;val left=width*.07f;val right=width*.93f;c.drawRect(0f,0f,width.toFloat(),top,panel);c.drawRect(left,top+14f,right,bottom-14f,paper);drawOnionSkin(c);drawDocument(c);if(previewStamps.isNotEmpty())drawStamps(c,previewStamps,accent,1f);c.drawText("ANIMA-ME",78f,38f,text);c.drawText("Frame ${document.currentFrame+1}/${document.duration}  •  ${document.fps} FPS  •  ${document.layers.size} camada(s)",78f,58f,sub);c.drawText("${document.activeLayer.name}  •  ${if(onionEnabled)"Onion ON" else "Onion OFF"}",width-270f,38f,sub)}
        private fun drawDocument(c:Canvas){document.layers.asReversed().forEach{layer->if(!layer.visible||layer.opacity<=0f)return@forEach;val frame=layer.frameAt(document.currentFrame)?:return@forEach;frame.strokes.forEach{stroke->drawStamps(c,BrushEngine.stamps(stroke.samples,stroke.resolvedBrushSettings(),stroke.id.hashCode().toLong()),stroke.color,layer.opacity)}}}
        private fun drawOnionSkin(c:Canvas){if(!onionEnabled)return;val layer=document.layers.firstOrNull{it.id==activeLayerId}?:return;layer.previousFrames(document.currentFrame,document.onion.previousCount).forEach{(_,f)->drawFrameGhost(c,f,Color.rgb(70,145,255),.22f)};layer.nextFrames(document.currentFrame,document.onion.nextCount).forEach{(_,f)->drawFrameGhost(c,f,Color.rgb(255,90,100),.18f)}}
        private fun drawFrameGhost(c:Canvas,frame:DrawingFrame,color:Int,alpha:Float){frame.strokes.forEach{stroke->drawStamps(c,BrushEngine.stamps(stroke.samples,stroke.resolvedBrushSettings(),stroke.id.hashCode().toLong()),color,alpha)}}
        private fun drawStamps(c:Canvas,stamps:List<BrushEngine.Stamp>,color:Int,layerOpacity:Float){stamps.forEach{stamp->stampPaint.color=Color.argb((stamp.alpha*layerOpacity*255f).toInt().coerceIn(1,255),Color.red(color),Color.green(color),Color.blue(color));c.drawCircle(stamp.x,stamp.y,stamp.size*.5f,stampPaint)}}
        override fun onTouchEvent(event:MotionEvent):Boolean{when(event.actionMasked){MotionEvent.ACTION_DOWN->{if(event.y<70f||event.y>height-64f)return false;currentSamples.clear();drawing=true;addSample(event);renderPreview();invalidate();return true};MotionEvent.ACTION_MOVE->{if(!drawing)return true;addSample(event);renderPreview();invalidate();return true};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(drawing&&event.actionMasked==MotionEvent.ACTION_UP)commitStroke();drawing=false;currentSamples.clear();previewStamps=emptyList();invalidate();return true}};return true}
        private fun addSample(event:MotionEvent){val pressure=(if(event.pressure>1f)event.pressure/2f else event.pressure).coerceIn(.05f,1f);currentSamples+=StrokeSample(event.x,event.y,pressure,event.eventTime)}
        private fun renderPreview(){val base=if(tool==Tool.ERASER)BrushDefaults.forPreset("eraser")else BrushDefaults.forPreset("basic");previewStamps=BrushEngine.stamps(BrushEngine.smooth(currentSamples,.18f),base.copy(size=brushSize,opacity=brushOpacity),document.currentFrame.toLong())}
        private fun commitStroke(){if(currentSamples.isEmpty())return;val frame=document.activeLayer.ensureFrame(document.currentFrame);val color=if(tool==Tool.ERASER)Color.WHITE else accent;val settings=(if(tool==Tool.ERASER)BrushDefaults.forPreset("eraser")else BrushDefaults.forPreset("basic")).copy(size=brushSize,opacity=brushOpacity);frame.strokes+=StrokeData(brushId=settings.id,color=color,size=brushSize,opacity=brushOpacity,settings=settings,samples=currentSamples.map{it.copy()}.toMutableList())}
        fun insertFrame(){document.insertFrame(document.currentFrame);invalidate();refreshTimeline()};fun duplicateFrame(){document.duplicateFrame(document.currentFrame);document.currentFrame++;invalidate();refreshTimeline()};fun deleteFrame(){document.deleteFrame(document.currentFrame);invalidate();refreshTimeline()};fun previousFrame(){document.currentFrame=(document.currentFrame-1).coerceAtLeast(0);invalidate();refreshTimeline()};fun nextFrame(){document.currentFrame=(document.currentFrame+1).coerceAtMost(document.duration-1);invalidate();refreshTimeline()};fun toggleOnion(){onionEnabled=!onionEnabled;invalidate();refreshTimeline()};fun clearCurrentFrame(){document.activeLayer.frameAt(document.currentFrame)?.strokes?.clear();invalidate()};fun addLayer(){val layer=document.addLayer();activeLayerId=layer.id;invalidate();refreshTimeline()}
        fun adjustSize(){brushSize=when{brushSize<8f->12f;brushSize<24f->32f;brushSize<64f->72f;else->6f};Toast.makeText(this@MainActivity,"Tamanho: ${brushSize.toInt()} px",Toast.LENGTH_SHORT).show()};fun adjustOpacity(){brushOpacity=when{brushOpacity>.85f->.65f;brushOpacity>.55f->.35f;else->1f};Toast.makeText(this@MainActivity,"Opacidade: ${(brushOpacity*100).toInt()}%",Toast.LENGTH_SHORT).show()};fun refreshTimeline(){timelineLabel?.text="Frame ${document.currentFrame+1}/${document.duration}  •  ${document.activeLayer.name}\n${if(onionEnabled)"Onion Skin" else "Onion desligado"}"}
        private fun blend(a:Int,b:Int,amount:Float):Int{val t=amount.coerceIn(0f,1f);return Color.rgb((Color.red(a)*(1f-t)+Color.red(b)*t).toInt(),(Color.green(a)*(1f-t)+Color.green(b)*t).toInt(),(Color.blue(a)*(1f-t)+Color.blue(b)*t).toInt())}
    }
}
