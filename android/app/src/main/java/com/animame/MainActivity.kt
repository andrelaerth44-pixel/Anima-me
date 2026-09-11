package com.animame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.BrushDefaults
import com.animame.editor.BrushEngine
import com.animame.editor.BrushPresetRepository
import com.animame.editor.DrawingFrame
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample

class MainActivity : Activity() {
    private lateinit var editor: EditorSurface
    private lateinit var timelineLabel: TextView
    private lateinit var frameStrip: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; buildUi() }
    override fun onResume() { super.onResume(); if(::editor.isInitialized){editor.refreshTheme();editor.invalidate();editor.refreshTimeline()} }
    override fun onPause(){if(::editor.isInitialized)editor.stopPlayback();super.onPause()}

    private fun buildUi(){
        val root=FrameLayout(this); editor=EditorSurface(); root.addView(editor,FrameLayout.LayoutParams(-1,-1))
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(8,6,8,6);setBackgroundColor(Color.rgb(30,34,39))}
        top.addView(button("⚙",50){startActivity(Intent(this@MainActivity,SettingsActivity::class.java))})
        top.addView(button("Pincel",76){editor.tool=Tool.BRUSH;editor.invalidate()});top.addView(button("Borracha",88){editor.tool=Tool.ERASER;editor.invalidate()})
        top.addView(button("Pincéis…",82){editor.showBrushPicker()});top.addView(button("Suavizar",82){editor.toggleAntialias()})
        top.addView(button("Tamanho",82){editor.adjustSize()});top.addView(button("Opacidade",90){editor.adjustOpacity()})
        top.addView(button("− Zoom",72){editor.adjustZoom(-.1f)});top.addView(button("100%",62){editor.resetViewport()});top.addView(button("+ Zoom",72){editor.adjustZoom(.1f)})
        top.addView(button("Camada +",86){editor.addLayer()});top.addView(button("▶ Play",76){editor.togglePlayback()});top.addView(button("FPS −",68){editor.adjustFps(-1)});top.addView(button("FPS +",68){editor.adjustFps(1)})
        root.addView(top,FrameLayout.LayoutParams(-1,66))
        timelineLabel=TextView(this).apply{setTextColor(Color.WHITE);textSize=13f;setPadding(10,4,10,4)}
        frameStrip=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(4,3,4,3)}
        val frameScroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;setBackgroundColor(Color.rgb(20,23,27));addView(frameStrip,FrameLayout.LayoutParams(-2,58))}
        val controls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.rgb(24,27,31));addView(timelineLabel,LinearLayout.LayoutParams(190,58));addView(button("◀",48){editor.previousFrame()});addView(button("＋",48){editor.insertFrame()});addView(button("⧉",48){editor.duplicateFrame()});addView(button("−",48){editor.deleteFrame()});addView(button("▶",48){editor.nextFrame()});addView(button("Onion",68){editor.toggleOnion()});addView(button("Limpar",70){editor.clearCurrentFrame()});addView(frameScroll,LinearLayout.LayoutParams(0,58,1f))}
        root.addView(controls,FrameLayout.LayoutParams(-1,58).apply{gravity=android.view.Gravity.BOTTOM});setContentView(root);editor.timelineLabel=timelineLabel;editor.frameStrip=frameStrip;editor.refreshTimeline()
    }
    private fun button(label:String,width:Int,action:()->Unit)=Button(this).apply{text=label;textSize=10f;setOnClickListener{action()};layoutParams=LinearLayout.LayoutParams(width,54)}
    private enum class Tool{BRUSH,ERASER}

    private inner class EditorSurface:View(this@MainActivity),Choreographer.FrameCallback{
        private val document=AnimationDocument(duration=1);private val currentSamples=mutableListOf<StrokeSample>();private var previewStamps=emptyList<BrushEngine.Stamp>()
        private var accent=ThemeColorStore.DEFAULT;private var brushSize=12f;private var brushOpacity=1f;private var drawing=false;private var onionEnabled=true;private var playing=false
        private var lastPlaybackNanos=0L;private var playbackAccumulator=0L;private var activeLayerId=document.activeLayer.id;private var brushId="canvas_1";private var brushName="Dip Pen (Soft)";private var brushCategory="Simple"
        private var smoothEdges=true;private var zoom=1f;private var panX=0f;private var panY=0f;private var pinchStartDistance=0f;private var pinchStartZoom=1f;private var lastTouchX=0f;private var lastTouchY=0f;private var twoFingerGesture=false
        var tool=Tool.BRUSH;var timelineLabel:TextView?=null;var frameStrip:LinearLayout?=null
        private val bg=Paint(Paint.ANTI_ALIAS_FLAG);private val panel=Paint(Paint.ANTI_ALIAS_FLAG);private val paper=Paint(Paint.ANTI_ALIAS_FLAG);private val stampPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{isDither=true};private val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=25f};private val sub=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.LTGRAY;textSize=15f}
        init{refreshTheme();isFocusable=true}
        fun refreshTheme(){accent=ThemeColorStore.get(this@MainActivity);bg.color=Color.rgb(18,20,23);panel.color=blend(accent,Color.rgb(30,34,39),.82f);paper.color=Color.rgb(245,245,245)}
        override fun onDraw(c:Canvas){c.drawColor(bg.color);val top=66f;val bottom=height-58f;val left=width*.07f;val right=width*.93f;c.drawRect(0f,0f,width.toFloat(),top,panel);val cx=(left+right)*.5f;val cy=(top+bottom)*.5f;c.save();c.clipRect(left,top+14f,right,bottom-14f);c.translate(cx+panX,cy+panY);c.scale(zoom,zoom);c.translate(-cx,-cy);c.drawRect(left,top+14f,right,bottom-14f,paper);drawOnionSkin(c);drawDocument(c);if(previewStamps.isNotEmpty())drawStamps(c,previewStamps,accent,1f);c.restore();c.drawText("ANIMA-ME",78f,38f,text);c.drawText("Frame ${document.currentFrame+1}/${document.duration}  •  ${document.fps} FPS  •  ${document.layers.size} camada(s)",78f,58f,sub);c.drawText("$brushName  •  ${if(playing)"PLAY" else "PAUSE"}  •  ${(zoom*100).toInt()}%",width-330f,38f,sub)}
        private fun drawDocument(c:Canvas){document.layers.asReversed().forEach{layer->if(!layer.visible||layer.opacity<=0f)return@forEach;val frame=layer.frameAt(document.currentFrame)?:return@forEach;frame.strokes.forEach{stroke->drawStamps(c,BrushEngine.stamps(stroke.samples,stroke.resolvedBrushSettings(),stroke.id.hashCode().toLong()),stroke.color,layer.opacity)}}}
        private fun drawOnionSkin(c:Canvas){if(!onionEnabled||playing)return;val layer=document.layers.firstOrNull{it.id==activeLayerId}?:return;layer.previousFrames(document.currentFrame,document.onion.previousCount).forEach{(_,f)->drawFrameGhost(c,f,Color.rgb(70,145,255),.22f)};layer.nextFrames(document.currentFrame,document.onion.nextCount).forEach{(_,f)->drawFrameGhost(c,f,Color.rgb(255,90,100),.18f)}}
        private fun drawFrameGhost(c:Canvas,frame:DrawingFrame,color:Int,alpha:Float){frame.strokes.forEach{stroke->drawStamps(c,BrushEngine.stamps(stroke.samples,stroke.resolvedBrushSettings(),stroke.id.hashCode().toLong()),color,alpha)}}
        private fun drawStamps(c:Canvas,stamps:List<BrushEngine.Stamp>,color:Int,layerOpacity:Float){stamps.forEach{stamp->stampPaint.isAntiAlias=stamp.antialias;stampPaint.isDither=stamp.antialias;val a=(stamp.alpha*layerOpacity*255f).toInt().coerceIn(1,255);stampPaint.color=Color.argb(a,Color.red(color),Color.green(color),Color.blue(color));if(stamp.antialias&&stamp.edgeQuality==com.animame.editor.EdgeSmoothing.Quality.MAX){val fringe=(a*.16f).toInt().coerceIn(1,255);stampPaint.color=Color.argb(fringe,Color.red(color),Color.green(color),Color.blue(color));c.drawCircle(stamp.x,stamp.y,stamp.size*.5f+.45f,stampPaint);stampPaint.color=Color.argb(a,Color.red(color),Color.green(color),Color.blue(color))};c.drawCircle(stamp.x,stamp.y,stamp.size*.5f,stampPaint)}}
        override fun onTouchEvent(e:MotionEvent):Boolean{if(playing)return true;when(e.actionMasked){MotionEvent.ACTION_DOWN->{lastTouchX=e.x;lastTouchY=e.y;twoFingerGesture=false;if(e.y<70f||e.y>height-64f)return false;currentSamples.clear();drawing=true;addSample(e);renderPreview();invalidate();return true};MotionEvent.ACTION_POINTER_DOWN->{if(e.pointerCount>=2){drawing=false;currentSamples.clear();previewStamps=emptyList();twoFingerGesture=true;pinchStartDistance=pointerDistance(e).coerceAtLeast(1f);pinchStartZoom=zoom;lastTouchX=(e.getX(0)+e.getX(1))*.5f;lastTouchY=(e.getY(0)+e.getY(1))*.5f};return true};MotionEvent.ACTION_MOVE->{if(e.pointerCount>=2){twoFingerGesture=true;val d=pointerDistance(e).coerceAtLeast(1f);zoom=(pinchStartZoom*d/pinchStartDistance).coerceIn(.25f,4f);val mx=(e.getX(0)+e.getX(1))*.5f;val my=(e.getY(0)+e.getY(1))*.5f;panX+=mx-lastTouchX;panY+=my-lastTouchY;lastTouchX=mx;lastTouchY=my;invalidate();return true};if(twoFingerGesture||!drawing)return true;addSample(e);renderPreview();invalidate();return true};MotionEvent.ACTION_POINTER_UP->{twoFingerGesture=true;drawing=false;currentSamples.clear();previewStamps=emptyList();return true};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(drawing&&!twoFingerGesture&&e.actionMasked==MotionEvent.ACTION_UP)commitStroke();drawing=false;currentSamples.clear();previewStamps=emptyList();invalidate();return true}};return true}
        private fun pointerDistance(e:MotionEvent):Float{if(e.pointerCount<2)return 0f;val dx=e.getX(0)-e.getX(1);val dy=e.getY(0)-e.getY(1);return kotlin.math.sqrt(dx*dx+dy*dy)}
        private fun addSample(e:MotionEvent){val pressure=(if(e.pressure>1f)e.pressure/2f else e.pressure).coerceIn(.05f,1f);val p=toCanvasPoint(e.x,e.y);currentSamples+=StrokeSample(p.first,p.second,pressure,e.eventTime)}
        private fun toCanvasPoint(x:Float,y:Float):Pair<Float,Float>{val left=width*.07f;val right=width*.93f;val top=66f;val bottom=height-58f;val cx=(left+right)*.5f;val cy=(top+bottom)*.5f;return Pair((x-cx-panX)/zoom+cx,(y-cy-panY)/zoom+cy)}
        private fun renderPreview(){val base=if(tool==Tool.ERASER)BrushDefaults.forPreset("eraser")else BrushPresetRepository.find(brushId);previewStamps=BrushEngine.stamps(currentSamples,base.copy(size=brushSize,opacity=brushOpacity,antialias=smoothEdges),document.currentFrame.toLong())}
        private fun commitStroke(){if(currentSamples.isEmpty())return;val frame=document.activeLayer.ensureFrame(document.currentFrame);val color=if(tool==Tool.ERASER)Color.WHITE else accent;val base=if(tool==Tool.ERASER)BrushDefaults.forPreset("eraser")else BrushPresetRepository.find(brushId);val settings=base.copy(size=brushSize,opacity=brushOpacity,antialias=smoothEdges);frame.strokes+=StrokeData(brushId=settings.id,color=color,size=brushSize,opacity=brushOpacity,settings=settings,samples=currentSamples.map{it.copy()}.toMutableList())}
        fun toggleAntialias(){smoothEdges=!smoothEdges;Toast.makeText(this@MainActivity,if(smoothEdges)"Suavizar: ligado (bordas menos pixeladas)" else "Suavizar: desligado (bordas nítidas)",Toast.LENGTH_SHORT).show();invalidate()}
        fun showBrushPicker(){val outer=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(18,8,18,4)};val search=EditText(this@MainActivity).apply{hint="Pesquisar pincel ou categoria"};outer.addView(search);val list=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL};val scroll=ScrollView(this@MainActivity).apply{addView(list)};outer.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));lateinit var dialog:AlertDialog;fun populate(q:String){list.removeAllViews();BrushPresetRepository.search(q).take(80).forEach{p->list.addView(Button(this@MainActivity).apply{text="${p.name}  •  ${p.category}";textSize=12f;setOnClickListener{brushId=p.id;brushName=p.name;brushCategory=p.category;tool=Tool.BRUSH;Toast.makeText(this@MainActivity,"Pincel: ${p.name}",Toast.LENGTH_SHORT).show();invalidate();dialog.dismiss()}})}};dialog=AlertDialog.Builder(this@MainActivity).setTitle("Pincéis — 389 presets").setView(outer).setNegativeButton("Fechar",null).create();search.addTextChangedListener(SimpleTextWatcher{populate(search.text.toString())});populate("");dialog.show()}
        fun adjustZoom(d:Float){zoom=(zoom+d).coerceIn(.25f,4f);invalidate()};fun resetViewport(){zoom=1f;panX=0f;panY=0f;invalidate()}
        fun insertFrame(){document.insertFrame(document.currentFrame);document.currentFrame=(document.currentFrame+1).coerceAtMost(document.duration-1);invalidate();refreshTimeline()};fun duplicateFrame(){document.duplicateFrame(document.currentFrame);document.currentFrame=(document.currentFrame+1).coerceAtMost(document.duration-1);invalidate();refreshTimeline()};fun deleteFrame(){if(document.duration<=1){clearCurrentFrame();return};document.deleteFrame(document.currentFrame);document.currentFrame=document.currentFrame.coerceIn(0,document.duration-1);invalidate();refreshTimeline()}
        fun previousFrame(){stopPlayback();document.currentFrame=(document.currentFrame-1).coerceAtLeast(0);invalidate();refreshTimeline()};fun nextFrame(){stopPlayback();document.currentFrame=(document.currentFrame+1).coerceAtMost(document.duration-1);invalidate();refreshTimeline()};fun toggleOnion(){onionEnabled=!onionEnabled;invalidate();refreshTimeline()};fun clearCurrentFrame(){document.activeLayer.frameAt(document.currentFrame)?.strokes?.clear();invalidate();refreshTimeline()}
        fun addLayer(){val layer=document.addLayer();activeLayerId=layer.id;invalidate();refreshTimeline()}
        fun togglePlayback(){if(playing)stopPlayback()else startPlayback()};fun startPlayback(){if(document.duration<=1){Toast.makeText(this@MainActivity,"Adicione pelo menos 2 frames para reproduzir",Toast.LENGTH_SHORT).show();return};playing=true;lastPlaybackNanos=System.nanoTime();playbackAccumulator=0L;Choreographer.getInstance().postFrameCallback(this);refreshTimeline();invalidate()};fun stopPlayback(){if(!playing)return;playing=false;Choreographer.getInstance().removeFrameCallback(this);lastPlaybackNanos=0L;playbackAccumulator=0L;refreshTimeline();invalidate()}
        override fun doFrame(n:Long){if(!playing)return;if(lastPlaybackNanos==0L)lastPlaybackNanos=n;val elapsed=(n-lastPlaybackNanos).coerceAtLeast(0L);lastPlaybackNanos=n;playbackAccumulator+=elapsed;val fd=1_000_000_000L/document.fps.coerceIn(1,120);while(playbackAccumulator>=fd){playbackAccumulator-=fd;document.currentFrame++;if(document.currentFrame>=document.duration)document.currentFrame=0};invalidate();refreshTimeline();Choreographer.getInstance().postFrameCallback(this)}
        fun adjustFps(d:Int){val next=(document.fps+d).coerceIn(1,60);document.fps=next;Toast.makeText(this@MainActivity,"FPS: $next",Toast.LENGTH_SHORT).show();refreshTimeline();invalidate()}
        fun refreshTimeline(){timelineLabel?.text="Frame ${document.currentFrame+1}/${document.duration}  •  ${document.activeLayer.name}  •  ${document.fps} FPS  •  ${if(playing)"Reproduzindo" else "Parado"}";frameStrip?.let{s->s.removeAllViews();for(i in 0 until document.duration){s.addView(TextView(this@MainActivity).apply{text="${i+1}";textSize=12f;gravity=android.view.Gravity.CENTER;setTextColor(if(i==document.currentFrame)Color.WHITE else Color.LTGRAY);setBackgroundColor(if(i==document.currentFrame)accent else if(hasContent(i))Color.rgb(65,72,82)else Color.rgb(38,42,48));setOnClickListener{stopPlayback();document.currentFrame=i;invalidate();refreshTimeline()}},LinearLayout.LayoutParams(52,50).apply{setMargins(3,4,3,4)})}}}
        private fun hasContent(i:Int)=document.layers.any{layer->!layer.frameAt(i)?.strokes.isNullOrEmpty()}
        fun adjustSize(){brushSize=when{brushSize<8f->12f;brushSize<24f->32f;brushSize<64f->72f;else->6f};Toast.makeText(this@MainActivity,"Tamanho: ${brushSize.toInt()} px",Toast.LENGTH_SHORT).show()};fun adjustOpacity(){brushOpacity=when{brushOpacity>.85f->.65f;brushOpacity>.55f->.35f;else->1f};Toast.makeText(this@MainActivity,"Opacidade: ${(brushOpacity*100).toInt()}%",Toast.LENGTH_SHORT).show()}
        private fun blend(a:Int,b:Int,t:Float):Int{val u=t.coerceIn(0f,1f);return Color.rgb((Color.red(a)*(1-u)+Color.red(b)*u).toInt(),(Color.green(a)*(1-u)+Color.green(b)*u).toInt(),(Color.blue(a)*(1-u)+Color.blue(b)*u).toInt())}
    }
    private class SimpleTextWatcher(private val changed:()->Unit):android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=changed();override fun afterTextChanged(s:android.text.Editable?)=Unit}
}
