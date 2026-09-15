package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationDocumentSnapshot
import com.animame.editor.AnimationLayer
import com.animame.editor.BrushCatalog
import com.animame.editor.BrushDefaults
import com.animame.editor.BrushEngine
import com.animame.editor.DrawingFrame
import com.animame.editor.EditorViewportState
import com.animame.editor.HistoryRegistry
import com.animame.editor.SelectionTransformController
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class MainActivity : Activity() {
    private lateinit var editor: EditorSurface
    private lateinit var timeline: TimelinePanel
    private lateinit var optionsPanel: LinearLayout
    private val topBarDp = 54
    private val timelineHeightDp = 238
    private val toolBarWidthDp = 118
    private val optionsWidthDp = 258

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        BrushToolState.load(this)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::editor.isInitialized) {
            BrushToolState.load(this)
            editor.refreshTheme()
            editor.invalidate()
            refreshWorkspace()
        }
    }

    override fun onPause() {
        if (::editor.isInitialized) editor.stopPlayback()
        super.onPause()
    }

    private fun buildUi() {
        val root = FrameLayout(this)
        editor = EditorSurface()
        root.addView(editor, FrameLayout.LayoutParams(-1, -1))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setBackgroundColor(Color.rgb(31, 35, 40))
        }
        header.addView(button("Menu", 60) { Toast.makeText(this@MainActivity, "Menu do projeto", Toast.LENGTH_SHORT).show() })
        header.addView(button("Definições", 84) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) })
        header.addView(TextView(this).apply {
            text = "Desenho: ${editor.document.duration} frames"
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(0, -1, 1f))
        header.addView(button("Frame -", 68) { editor.previousFrame() })
        header.addView(button("Frame +", 68) { editor.nextFrame() })
        header.addView(button("Reproduzir", 88) { editor.togglePlayback() })
        header.addView(button("FPS -", 58) { editor.adjustFps(-1) })
        header.addView(button("FPS +", 58) { editor.adjustFps(1) })
        header.addView(button("Exportar", 74) { Toast.makeText(this@MainActivity, "Hub de exportação disponível nas Definições", Toast.LENGTH_SHORT).show() })
        root.addView(header, FrameLayout.LayoutParams(-1, dp(topBarDp)))

        timeline = TimelinePanel(this, editor.document,
            onFrameSelected = { layerId, frame ->
                editor.stopPlayback(); editor.selectLayer(layerId)
                editor.document.currentFrame = frame.coerceIn(0, editor.document.duration - 1)
                editor.invalidate(); refreshWorkspace()
            },
            onLayerSelected = { layerId -> editor.selectLayer(layerId); editor.invalidate(); refreshWorkspace() },
            onLayerChanged = { editor.document.normalize(); editor.invalidate(); refreshWorkspace() }
        )
        root.addView(timeline, FrameLayout.LayoutParams(-1, dp(timelineHeightDp)).apply { topMargin = dp(topBarDp) })

        root.addView(buildToolBar(), FrameLayout.LayoutParams(dp(toolBarWidthDp), -1).apply { topMargin = dp(topBarDp + timelineHeightDp) })
        optionsPanel = buildOptionsPanel()
        root.addView(optionsPanel, FrameLayout.LayoutParams(dp(optionsWidthDp), -1).apply {
            leftMargin = dp(toolBarWidthDp); topMargin = dp(topBarDp + timelineHeightDp)
        })
        setContentView(root)
        refreshWorkspace()
    }

    private fun buildToolBar(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(5), dp(5), dp(5), dp(5))
        setBackgroundColor(Color.rgb(26, 29, 34))
        addView(button("Modificar camadas", 108) { refreshWorkspace() })
        addView(button("Adicionar desenho", 108) { editor.insertFrame() })
        addView(button("Timeline", 108) { timeline.visibility = View.VISIBLE })
        addView(button("Opções", 108) { optionsPanel.visibility = if (optionsPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE })
        addView(button("Pincel", 108) { editor.tool = Tool.BRUSH; editor.clearSelection(); refreshWorkspace(); editor.invalidate() })
        addView(button("Borracha", 108) { editor.tool = Tool.ERASER; editor.clearSelection(); refreshWorkspace(); editor.invalidate() })
        addView(button("Pincéis", 108) { openBrushPicker() })
        addView(button("Laço", 108) { editor.beginLassoMode() })
        addView(button("Mover/Transformar", 108) { editor.beginTransformMode() })
        addView(button("Aplicar transformação", 108) { editor.applyTransform() })
        addView(button("Cancelar transformação", 108) { editor.cancelTransform() })
        addView(button("Rodar -15", 108) { editor.rotateSelection(-15f) })
        addView(button("Rodar +15", 108) { editor.rotateSelection(15f) })
        addView(button("Espelhar H", 108) { editor.flipSelection(true) })
        addView(button("Espelhar V", 108) { editor.flipSelection(false) })
        addView(button("Inverter seleção", 108) { editor.invertSelection() })
        addView(button("Nova camada", 108) { editor.addLayer() })
        addView(button("Desfazer", 108) { editor.undo() })
        addView(button("Refazer", 108) { editor.redo() })
        addView(button("Reproduzir", 108) { editor.togglePlayback() })
        addView(button("Onion skin", 108) { editor.toggleOnion() })
        addView(button("Zoom +", 108) { editor.zoom(1.2f) })
        addView(button("Zoom -", 108) { editor.zoom(.8333333f) })
        addView(button("Ajustar", 108) { editor.resetViewport() })
    }

    private fun buildOptionsPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(10), dp(10))
            setBackgroundColor(Color.rgb(12, 30, 47))
        }
        panel.addView(TextView(this).apply { text = "Opções da ferramenta"; textSize = 18f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(-1, dp(38)))
        val brushName = TextView(this).apply { setTextColor(Color.WHITE); textSize = 14f }; panel.addView(brushName)
        panel.addView(button("Abrir biblioteca de pincéis", 220) { openBrushPicker() })
        val sizeLabel = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f }; panel.addView(sizeLabel)
        panel.addView(seekBar(1, 4096, BrushToolState.size.toInt()) { BrushToolState.size = it.toFloat(); BrushToolState.save(this@MainActivity); updateOptionLabels(brushName, sizeLabel, null); editor.invalidate() })
        val opacityLabel = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f }; panel.addView(opacityLabel)
        panel.addView(seekBar(0, 100, (BrushToolState.opacity * 100f).toInt()) { BrushToolState.opacity = it / 100f; BrushToolState.save(this@MainActivity); updateOptionLabels(brushName, sizeLabel, opacityLabel); editor.invalidate() })
        val spacingLabel = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f }; panel.addView(spacingLabel)
        panel.addView(seekBar(1, 400, (BrushToolState.spacing * 100f).toInt()) { BrushToolState.spacing = it / 100f; BrushToolState.save(this@MainActivity); spacingLabel.text = "Espaçamento: ${"%.2f".format(BrushToolState.spacing)}"; editor.invalidate() })
        val smoothingLabel = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f }; panel.addView(smoothingLabel)
        panel.addView(seekBar(0, 100, (BrushToolState.smoothing * 100f).toInt()) { BrushToolState.smoothing = it / 100f; BrushToolState.save(this@MainActivity); smoothingLabel.text = "Suavização: ${(BrushToolState.smoothing * 100).toInt()}%" })
        panel.addView(check("Sensibilidade à pressão", BrushToolState.pressure) { BrushToolState.pressure = it; BrushToolState.save(this@MainActivity) })
        panel.addView(check("Randomizar rotação", BrushToolState.randomRotation) { BrushToolState.randomRotation = it; BrushToolState.save(this@MainActivity) })
        panel.addView(TextView(this).apply {
            text = "Fluxo horizontal: ferramentas à esquerda, opções junto ao canvas e timeline no topo. A seleção é vetorial por traço e a transformação é aplicada aos pontos do frame."
            textSize = 12f; setTextColor(Color.LTGRAY); setPadding(0, dp(14), 0, 0)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        panel.post { updateOptionLabels(brushName, sizeLabel, opacityLabel); spacingLabel.text = "Espaçamento: ${"%.2f".format(BrushToolState.spacing)}"; smoothingLabel.text = "Suavização: ${(BrushToolState.smoothing * 100).toInt()}%" }
        return panel
    }

    private fun updateOptionLabels(brushName: TextView, sizeLabel: TextView, opacityLabel: TextView?) {
        val preset = BrushCatalog.all().firstOrNull { it.id == BrushToolState.brushId }
        brushName.text = "${preset?.name ?: "Pincel"}  |  ${preset?.category ?: ""}"
        sizeLabel.text = "Tamanho: ${BrushToolState.size.toInt()} px"
        opacityLabel?.text = "Opacidade: ${(BrushToolState.opacity * 100).toInt()}%"
    }

    private fun seekBar(min: Int, max: Int, initial: Int, onChange: (Int) -> Unit) = SeekBar(this).apply {
        this.max = max - min; progress = (initial - min).coerceIn(0, max - min)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) onChange(progress + min) }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun check(label: String, checked: Boolean, action: (Boolean) -> Unit) = CheckBox(this).apply {
        text = label; textSize = 12f; setTextColor(Color.WHITE); isChecked = checked; setOnCheckedChangeListener { _, value -> action(value) }
    }

    private fun openBrushPicker() { startActivityForResult(Intent(this, BrushPickerActivity::class.java), 42) }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) { BrushToolState.load(this); editor.invalidate(); refreshWorkspace() }
    }
    private fun refreshWorkspace() { if (::timeline.isInitialized) timeline.refresh(ThemeColorStore.get(this)); if (::optionsPanel.isInitialized) optionsPanel.invalidate() }
    private fun button(label: String, widthDp: Int, action: () -> Unit) = Button(this).apply { text = label; textSize = 9f; setOnClickListener { action() }; layoutParams = LinearLayout.LayoutParams(dp(widthDp), dp(46)).apply { setMargins(2,2,2,2) } }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private enum class Tool { BRUSH, ERASER }

    private inner class EditorSurface : View(this@MainActivity), Choreographer.FrameCallback {
        val document = AnimationDocument(name = intent.getStringExtra("project_name") ?: "Minha animação", width = intent.getIntExtra("project_width",1280), height = intent.getIntExtra("project_height",720), fps = intent.getIntExtra("project_fps",24), duration = intent.getIntExtra("project_duration",1).coerceAtLeast(1), transparentBackground = intent.getBooleanExtra("project_transparent",false))
        private val history get() = HistoryRegistry.forDocument(document)
        private val currentSamples = mutableListOf<StrokeSample>()
        private var previewStamps = emptyList<BrushEngine.Stamp>()
        private var accent = ThemeColorStore.DEFAULT
        private var drawing = false
        private var onionEnabled = true
        private var playing = false
        private var lastPlaybackNanos = 0L
        private var playbackAccumulator = 0L
        private val viewport = EditorViewportState()
        private val selection = SelectionTransformController()
        private val selectedStrokeIds = linkedSetOf<String>()
        private var lassoDrawing = false
        private var transformGesture = false
        private var transformPointerId = MotionEvent.INVALID_POINTER_ID
        private var lastTransformX = 0f
        private var lastTransformY = 0f
        private var lastTransformDistance = 0f
        private var lastTransformAngle = 0f
        private var transformPivotX = 0f
        private var transformPivotY = 0f
        private var transformHistoryBefore: AnimationDocumentSnapshot? = null
        private var panPointerId = MotionEvent.INVALID_POINTER_ID
        private var lastPanX = 0f
        private var lastPanY = 0f
        private var panActive = false
        var tool = Tool.BRUSH

        private val scaleDetector = ScaleGestureDetector(this@MainActivity, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean { viewport.zoomAt(detector.scaleFactor, detector.focusX, detector.focusY); invalidate(); return true }
        })
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val panel = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paper = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.rgb(30,180,255); pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f,8f),0f) }
        private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 25f }
        private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 15f }

        init { document.normalize(); refreshTheme(); isFocusable = true }
        fun refreshTheme() { accent = ThemeColorStore.get(this@MainActivity); bg.color = Color.rgb(18,20,23); panel.color = blend(accent,Color.rgb(30,34,39),.82f); paper.color = document.backgroundColor }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val top = dp(topBarDp + timelineHeightDp).toFloat()
            val leftPanel = dp(toolBarWidthDp + if (optionsPanel.visibility == View.VISIBLE) optionsWidthDp else 0).toFloat()
            c.save(); c.clipRect(leftPanel,top,width.toFloat(),height.toFloat()); c.translate(viewport.offsetX,viewport.offsetY); c.scale(viewport.scale,viewport.scale)
            val left=width*.07f; val right=width*.93f
            if(!document.transparentBackground)c.drawRect(left,top+dp(14).toFloat(),right,height-dp(14).toFloat(),paper)
            drawOnionSkin(c); drawDocument(c); if(previewStamps.isNotEmpty())drawStamps(c,previewStamps,accent,1f)
            if(selection.mode != SelectionTransformController.Mode.NONE) drawSelection(c)
            c.restore()
            c.drawText("Frame ${document.currentFrame+1}/${document.duration}",(width-dp(150)).toFloat(),top-dp(12).toFloat(),sub)
            c.drawText("Zoom ${(viewport.scale*100).toInt()}%  |  ${selection.mode}",(width-dp(210)).toFloat(),height-dp(8).toFloat(),sub)
        }

        private fun drawingForFrame(layer: AnimationLayer, frame: Int): DrawingFrame? { layer.frameAt(frame)?.let{return it}; var best=-1; layer.frames.keys.forEach{candidate->if(candidate<=frame&&candidate>best)best=candidate}; if(best<0)return null; val d=layer.frameAt(best)?:return null; return if(frame<best+d.exposure)d else null }
        private fun drawDocument(c:Canvas){ document.layers.asReversed().forEach{layer->if(!layer.visible||layer.opacity<=0f||layer.isBackground)return@forEach; val frame=drawingForFrame(layer,document.currentFrame)?:return@forEach; frame.strokes.forEach{stroke->drawStamps(c,BrushEngine.stamps(stroke.samples,stroke.resolvedBrushSettings(),stroke.id.hashCode().toLong()),stroke.color,layer.opacity)}} }
        private fun drawOnionSkin(c:Canvas){if(!onionEnabled||playing)return; val layer=document.activeLayer; layer.previousFrames(document.currentFrame,document.onion.previousCount).forEach{(_,f)->drawFrameGhost(c,f,Color.rgb(70,145,255),.22f)}; layer.nextFrames(document.currentFrame,document.onion.nextCount).forEach{(_,f)->drawFrameGhost(c,f,Color.rgb(255,90,100),.18f)}}
        private fun drawFrameGhost(c:Canvas,frame:DrawingFrame,color:Int,alpha:Float){frame.strokes.forEach{stroke->drawStamps(c,BrushEngine.stamps(stroke.samples,stroke.resolvedBrushSettings(),stroke.id.hashCode().toLong()),color,alpha)}}
        private fun drawStamps(c:Canvas,stamps:List<BrushEngine.Stamp>,color:Int,layerOpacity:Float){stamps.forEach{stamp->stampPaint.color=Color.argb((stamp.alpha*layerOpacity*255f).toInt().coerceIn(1,255),Color.red(color),Color.green(color),Color.blue(color));c.drawCircle(stamp.x,stamp.y,stamp.size*.5f,stampPaint)}}

        private fun drawSelection(c: Canvas) {
            if (!selection.path.isEmpty) c.drawPath(selection.path, selectionPaint)
            if (!selection.bounds.isEmpty) { val b=selection.bounds; c.drawRect(b,selectionPaint); val r=8f; c.drawCircle(b.left,b.top,r,handlePaint); c.drawCircle(b.right,b.top,r,handlePaint); c.drawCircle(b.left,b.bottom,r,handlePaint); c.drawCircle(b.right,b.bottom,r,handlePaint) }
        }

        fun beginLassoMode(){ stopPlayback(); clearSelection(); selection.activateLasso(); lassoDrawing=false; tool=Tool.BRUSH; invalidate(); Toast.makeText(this@MainActivity,"Laço: contorne o desenho no canvas",Toast.LENGTH_SHORT).show() }
        fun beginTransformMode(){ if(selectedStrokeIds.isEmpty()){Toast.makeText(this@MainActivity,"Faça uma seleção primeiro",Toast.LENGTH_SHORT).show();return}; selection.beginTransform(); transformGesture=false; invalidate(); Toast.makeText(this@MainActivity,"Transformação ativa",Toast.LENGTH_SHORT).show() }
        fun clearSelection(){selection.clear();selectedStrokeIds.clear();lassoDrawing=false;transformGesture=false;transformHistoryBefore=null;invalidate()}

        override fun onTouchEvent(event:MotionEvent):Boolean{
            if(selection.mode==SelectionTransformController.Mode.LASSO||selection.mode==SelectionTransformController.Mode.MOVE||selection.mode==SelectionTransformController.Mode.TRANSFORM)return handleSelectionTouch(event)
            scaleDetector.onTouchEvent(event); if(playing||document.activeLayer.locked)return true
            if(event.actionMasked==MotionEvent.ACTION_CANCEL||(Build.VERSION.SDK_INT>=33&&(event.flags and MotionEvent.FLAG_CANCELED)!=0)){cancelStroke();return true}
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN->{val canvasTop=dp(topBarDp+timelineHeightDp);val canvasLeft=dp(toolBarWidthDp+if(optionsPanel.visibility==View.VISIBLE)optionsWidthDp else 0);if(event.y<canvasTop||event.x<canvasLeft)return false;panActive=false;panPointerId=MotionEvent.INVALID_POINTER_ID;currentSamples.clear();drawing=true;if(event.getToolType(0)==MotionEvent.TOOL_TYPE_ERASER)tool=Tool.ERASER;addSample(event);renderPreview();invalidate();return true}
                MotionEvent.ACTION_POINTER_DOWN->{if(event.pointerCount>=2){drawing=false;currentSamples.clear();previewStamps=emptyList();panActive=true;val i=event.actionIndex;panPointerId=event.getPointerId(i);lastPanX=event.getX(i);lastPanY=event.getY(i)};return true}
                MotionEvent.ACTION_MOVE->{if(panActive&&event.pointerCount>=2){val i=event.findPointerIndex(panPointerId).takeIf{it>=0}?:0;val x=event.getX(i);val y=event.getY(i);viewport.panBy(x-lastPanX,y-lastPanY);lastPanX=x;lastPanY=y;invalidate();return true};if(!drawing||event.pointerCount!=1)return true;addSample(event);renderPreview();invalidate();return true}
                MotionEvent.ACTION_POINTER_UP->{if(event.pointerCount<=2){panActive=false;panPointerId=MotionEvent.INVALID_POINTER_ID};return true}
                MotionEvent.ACTION_UP->{if(drawing)commitStroke();cancelStrokeState();refreshWorkspace();return true}
            };return true
        }

        private fun handleSelectionTouch(event:MotionEvent):Boolean{
            if(event.actionMasked==MotionEvent.ACTION_CANCEL){restoreTransform();clearSelection();return true}
            if(selection.mode==SelectionTransformController.Mode.LASSO){
                when(event.actionMasked){
                    MotionEvent.ACTION_DOWN->{selection.beginLasso(modelX(event.x),modelY(event.y));lassoDrawing=true;invalidate();return true}
                    MotionEvent.ACTION_MOVE->{if(lassoDrawing){selection.addLassoPoint(modelX(event.x),modelY(event.y));invalidate()};return true}
                    MotionEvent.ACTION_UP->{if(lassoDrawing){selection.addLassoPoint(modelX(event.x),modelY(event.y));if(selection.finishLasso())selectStrokesInsideLasso();lassoDrawing=false;invalidate()};return true}
                };return true
            }
            if(event.actionMasked==MotionEvent.ACTION_DOWN){val mx=modelX(event.x);val my=modelY(event.y);if(selection.bounds.contains(mx,my)){beginTransformHistory();transformGesture=true;transformPointerId=event.getPointerId(0);lastTransformX=mx;lastTransformY=my;return true};clearSelection();return true}
            if(event.actionMasked==MotionEvent.ACTION_POINTER_DOWN&&event.pointerCount>=2){beginTransformHistory();transformGesture=true;lastTransformDistance=distance(event);lastTransformAngle=angle(event);transformPivotX=modelX((event.getX(0)+event.getX(1))/2f);transformPivotY=modelY((event.getY(0)+event.getY(1))/2f);return true}
            if(event.actionMasked==MotionEvent.ACTION_MOVE&&transformGesture){if(event.pointerCount>=2){val d=distance(event);val a=angle(event);if(lastTransformDistance>0f)applyScale(d/lastTransformDistance,transformPivotX,transformPivotY);applyRotation(a-lastTransformAngle,transformPivotX,transformPivotY);lastTransformDistance=d;lastTransformAngle=a;invalidate();return true};val i=event.findPointerIndex(transformPointerId).takeIf{it>=0}?:0;val mx=modelX(event.getX(i));val my=modelY(event.getY(i));applyTranslation(mx-lastTransformX,my-lastTransformY);lastTransformX=mx;lastTransformY=my;invalidate();return true}
            if(event.actionMasked==MotionEvent.ACTION_UP){finishTransformHistory();transformGesture=false;transformPointerId=MotionEvent.INVALID_POINTER_ID;return true}
            return true
        }

        private fun beginTransformHistory(){if(transformHistoryBefore==null)transformHistoryBefore=document.snapshot()}
        private fun finishTransformHistory(){val before=transformHistoryBefore?:return;history.record(before,document.snapshot(),"Transformar seleção");transformHistoryBefore=null;refreshWorkspace()}
        private fun restoreTransform(){val before=transformHistoryBefore?:return;document.restore(before);transformHistoryBefore=null;selection.clear();invalidate();refreshWorkspace()}

        private fun selectStrokesInsideLasso(){selectedStrokeIds.clear();val frame=document.activeLayer.frameAt(document.currentFrame)?:return;val region=android.graphics.Region();val b=selection.bounds;val rect=android.graphics.Rect(b.left.toInt(),b.top.toInt(),b.right.toInt()+1,b.bottom.toInt()+1);region.setPath(selection.path,android.graphics.Region(rect));frame.strokes.forEach{stroke->if(stroke.samples.any{s->region.contains(s.x.toInt(),s.y.toInt())})selectedStrokeIds+=stroke.id};if(selectedStrokeIds.isEmpty())Toast.makeText(this@MainActivity,"Nenhum traço encontrado na seleção",Toast.LENGTH_SHORT).show()}
        private fun selectedStrokes():List<StrokeData>{val frame=document.activeLayer.frameAt(document.currentFrame)?:return emptyList();return frame.strokes.filter{selectedStrokeIds.contains(it.id)}}
        private fun applyTranslation(dx:Float,dy:Float){selectedStrokes().forEach{stroke->for(i in stroke.samples.indices){val s=stroke.samples[i];stroke.samples[i]=s.copy(x=s.x+dx,y=s.y+dy)}};selection.moveBy(dx,dy)}
        private fun applyScale(f:Float,pivotX:Float,pivotY:Float){val factor=f.coerceIn(.2f,5f);selectedStrokes().forEach{stroke->for(i in stroke.samples.indices){val s=stroke.samples[i];stroke.samples[i]=s.copy(x=pivotX+(s.x-pivotX)*factor,y=pivotY+(s.y-pivotY)*factor)}};selection.scaleBy(factor,pivotX,pivotY)}
        private fun applyRotation(degrees:Float,pivotX:Float,pivotY:Float){if(degrees==0f)return;val r=Math.toRadians(degrees.toDouble());val co=cos(r).toFloat();val si=sin(r).toFloat();selectedStrokes().forEach{stroke->for(i in stroke.samples.indices){val s=stroke.samples[i];val dx=s.x-pivotX;val dy=s.y-pivotY;stroke.samples[i]=s.copy(x=pivotX+dx*co-dy*si,y=pivotY+dx*si+dy*co)}};selection.rotateBy(degrees)}

        fun applyTransform(){if(selectedStrokeIds.isEmpty())return;finishTransformHistory();selection.doneTransform();transformGesture=false;invalidate();refreshWorkspace();Toast.makeText(this@MainActivity,"Transformação aplicada",Toast.LENGTH_SHORT).show()}
        fun cancelTransform(){restoreTransform();Toast.makeText(this@MainActivity,"Transformação cancelada",Toast.LENGTH_SHORT).show()}
        fun rotateSelection(degrees:Float){if(selectedStrokeIds.isEmpty())return;beginTransformHistory();val b=selection.bounds;val px=b.centerX();val py=b.centerY();applyRotation(degrees,px,py);finishTransformHistory();invalidate();refreshWorkspace()}
        fun flipSelection(horizontal:Boolean){if(selectedStrokeIds.isEmpty())return;beginTransformHistory();val b=selection.bounds;val px=b.centerX();val py=b.centerY();selectedStrokes().forEach{stroke->for(i in stroke.samples.indices){val s=stroke.samples[i];stroke.samples[i]=if(horizontal)s.copy(x=px-(s.x-px))else s.copy(y=py-(s.y-py))}};if(horizontal)selection.flipHorizontal()else selection.flipVertical();finishTransformHistory();invalidate();refreshWorkspace()}
        fun invertSelection(){val frame=document.activeLayer.frameAt(document.currentFrame)?:return;val previous=selectedStrokeIds.toSet();selectedStrokeIds.clear();frame.strokes.forEach{if(!previous.contains(it.id))selectedStrokeIds+=it.id};if(selectedStrokeIds.isEmpty()){selection.clear();invalidate();return};selection.invertSelection();rebuildSelectionBounds();invalidate();refreshWorkspace()}
        private fun rebuildSelectionBounds(){val strokes=selectedStrokes();if(strokes.isEmpty())return;var l=Float.POSITIVE_INFINITY;var t=Float.POSITIVE_INFINITY;var r=Float.NEGATIVE_INFINITY;var b=Float.NEGATIVE_INFINITY;strokes.forEach{s->s.samples.forEach{p->l=minOf(l,p.x);t=minOf(t,p.y);r=maxOf(r,p.x);b=maxOf(b,p.y)}};selection.setBounds(RectF(l,t,r,b))}

        private fun distance(e:MotionEvent)=hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0));private fun angle(e:MotionEvent)=Math.toDegrees(atan2((e.getY(1)-e.getY(0)).toDouble(),(e.getX(1)-e.getX(0)).toDouble())).toFloat();private fun modelX(x:Float)=(x-viewport.offsetX)/viewport.scale;private fun modelY(y:Float)=(y-viewport.offsetY)/viewport.scale
        private fun addSample(event:MotionEvent){val pressure=if(BrushToolState.pressure)event.pressure.coerceIn(.05f,1f)else 1f;val stylus=event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS;val tilt=if(stylus)event.getAxisValue(MotionEvent.AXIS_TILT).coerceIn(0f,Math.PI.toFloat()/2f)else 0f;val orientation=if(stylus)event.orientation else 0f;currentSamples+=StrokeSample(modelX(event.x),modelY(event.y),pressure,event.eventTime,tilt,orientation)}
        private fun renderPreview(){val id=if(tool==Tool.ERASER)"eraser"else BrushToolState.brushId;val base=if(tool==Tool.ERASER)BrushDefaults.forPreset("eraser")else BrushCatalog.settings(id);val settings=base.copy(size=BrushToolState.size,opacity=BrushToolState.opacity,spacing=BrushToolState.spacing);previewStamps=BrushEngine.stamps(BrushEngine.smooth(currentSamples,BrushToolState.smoothing),settings,document.currentFrame.toLong())}
        private fun commitStroke(){if(currentSamples.isEmpty())return;val before=document.snapshot();val frame=document.activeLayer.ensureFrame(document.currentFrame);val settings=if(tool==Tool.ERASER)BrushDefaults.forPreset("eraser").copy(size=BrushToolState.size,opacity=BrushToolState.opacity)else BrushCatalog.settings(BrushToolState.brushId).copy(size=BrushToolState.size,opacity=BrushToolState.opacity,spacing=BrushToolState.spacing);val color=if(tool==Tool.ERASER)Color.WHITE else accent;frame.strokes+=StrokeData(brushId=settings.id,color=color,size=BrushToolState.size,opacity=BrushToolState.opacity,settings=settings,samples=currentSamples.map{it.copy()}.toMutableList());history.record(before,document.snapshot(),"Desenhar traço");BrushToolState.save(this@MainActivity)}
        private fun cancelStroke(){currentSamples.clear();cancelStrokeState();invalidate()};private fun cancelStrokeState(){drawing=false;panActive=false;panPointerId=MotionEvent.INVALID_POINTER_ID;previewStamps=emptyList();invalidate()}
        fun selectLayer(id:String){document.selectLayer(id);invalidate()};fun zoom(factor:Float){viewport.zoomAt(factor,width*.5f,height*.5f);invalidate()};fun resetViewport(){viewport.reset();invalidate()}
        fun insertFrame(){stopPlayback();val before=document.snapshot();document.insertFrame(document.currentFrame);history.record(before,document.snapshot(),"Inserir frame");invalidate();refreshWorkspace()}
        fun duplicateFrame(){stopPlayback();val before=document.snapshot();document.duplicateFrame(document.currentFrame);document.currentFrame=(document.currentFrame+1).coerceAtMost(document.duration-1);history.record(before,document.snapshot(),"Duplicar frame");invalidate();refreshWorkspace()}
        fun deleteFrame(){stopPlayback();if(document.duration<=1){clearCurrentFrame()}else{val before=document.snapshot();document.deleteFrame(document.currentFrame);history.record(before,document.snapshot(),"Excluir frame");invalidate();refreshWorkspace()}}
        fun previousFrame(){stopPlayback();document.currentFrame=(document.currentFrame-1).coerceAtLeast(0);invalidate();refreshWorkspace()};fun nextFrame(){stopPlayback();document.currentFrame=(document.currentFrame+1).coerceAtMost(document.duration-1);invalidate();refreshWorkspace()}
        fun toggleOnion(){onionEnabled=!onionEnabled;invalidate();refreshWorkspace()}
        fun clearCurrentFrame(){val before=document.snapshot();document.activeLayer.frameAt(document.currentFrame)?.strokes?.clear();clearSelection();history.record(before,document.snapshot(),"Limpar frame");invalidate();refreshWorkspace()}
        fun addLayer(){val before=document.snapshot();document.addLayer();history.record(before,document.snapshot(),"Adicionar camada");invalidate();refreshWorkspace()}
        fun undo(){stopPlayback();clearSelection();if(history.undo(document)){invalidate();refreshWorkspace();Toast.makeText(this@MainActivity,"Desfazer",Toast.LENGTH_SHORT).show()}else Toast.makeText(this@MainActivity,"Nada para desfazer",Toast.LENGTH_SHORT).show()}
        fun redo(){stopPlayback();clearSelection();if(history.redo(document)){invalidate();refreshWorkspace();Toast.makeText(this@MainActivity,"Refazer",Toast.LENGTH_SHORT).show()}else Toast.makeText(this@MainActivity,"Nada para refazer",Toast.LENGTH_SHORT).show()}
        fun togglePlayback(){if(playing)stopPlayback()else startPlayback()};fun startPlayback(){if(document.duration<=1){Toast.makeText(this@MainActivity,"Adicione pelo menos 2 frames para reproduzir",Toast.LENGTH_SHORT).show();return};playing=true;lastPlaybackNanos=System.nanoTime();playbackAccumulator=0L;clearSelection();Choreographer.getInstance().postFrameCallback(this);refreshWorkspace();invalidate()};fun stopPlayback(){if(!playing)return;playing=false;Choreographer.getInstance().removeFrameCallback(this);lastPlaybackNanos=0L;playbackAccumulator=0L;refreshWorkspace();invalidate()}
        override fun doFrame(frameTimeNanos:Long){if(!playing)return;if(lastPlaybackNanos==0L)lastPlaybackNanos=frameTimeNanos;playbackAccumulator+=(frameTimeNanos-lastPlaybackNanos).coerceAtLeast(0L);lastPlaybackNanos=frameTimeNanos;val frameDuration=1_000_000_000L/document.fps.coerceIn(1,120);while(playbackAccumulator>=frameDuration){playbackAccumulator-=frameDuration;document.advancePlaybackFrame()};invalidate();refreshWorkspace();Choreographer.getInstance().postFrameCallback(this)}
        fun adjustFps(delta:Int){val before=document.snapshot();document.fps=(document.fps+delta).coerceIn(1,60);history.record(before,document.snapshot(),"Alterar FPS");Toast.makeText(this@MainActivity,"FPS: ${document.fps}",Toast.LENGTH_SHORT).show();invalidate();refreshWorkspace()};fun adjustSize(){BrushToolState.size=when{BrushToolState.size<8f->12f;BrushToolState.size<24f->32f;BrushToolState.size<64f->72f;else->6f};BrushToolState.save(this@MainActivity);Toast.makeText(this@MainActivity,"Tamanho: ${BrushToolState.size.toInt()} px",Toast.LENGTH_SHORT).show();refreshWorkspace()};fun adjustOpacity(){BrushToolState.opacity=when{BrushToolState.opacity>.85f->.65f;BrushToolState.opacity>.55f->.35f;else->1f};BrushToolState.save(this@MainActivity);Toast.makeText(this@MainActivity,"Opacidade: ${(BrushToolState.opacity*100).toInt()}%",Toast.LENGTH_SHORT).show();refreshWorkspace()}
        private fun blend(a:Int,b:Int,amount:Float):Int{val t=amount.coerceIn(0f,1f);return Color.rgb((Color.red(a)*(1f-t)+Color.red(b)*t).toInt(),(Color.green(a)*(1f-t)+Color.green(b)*t).toInt(),(Color.blue(a)*(1f-t)+Color.blue(b)*t).toInt())}
    }
}