package com.animame

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationLayer

class TimelinePanel(
    context: Context,
    private val document: AnimationDocument,
    private val onFrameSelected: (layerId: String, frame: Int) -> Unit,
    private val onLayerSelected: (layerId: String) -> Unit,
    private val onLayerChanged: () -> Unit
) : LinearLayout(context) {
    private val ruler = LinearLayout(context)
    private val rows = LinearLayout(context)
    private val horizontal = HorizontalScrollView(context)
    private val vertical = ScrollView(context)
    private var zoom = 1f
    private val baseFrameWidth = 42f
    private val layerHeaderWidth = 230

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(22, 25, 29))
        buildRuler()
        addView(ruler, LinearLayout.LayoutParams(-1, 38))
        rows.orientation = VERTICAL
        horizontal.isHorizontalScrollBarEnabled = true
        horizontal.addView(rows, HorizontalScrollView.LayoutParams(-2, -2))
        vertical.isFillViewport = true
        vertical.addView(horizontal, LinearLayout.LayoutParams(-1, -1))
        addView(vertical, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun frameWidth(): Int = (baseFrameWidth * zoom).toInt().coerceAtLeast(14)
    private fun trackWidth(): Int = (document.duration * frameWidth() + 12).coerceAtLeast(frameWidth())

    private fun buildRuler() {
        ruler.removeAllViews(); ruler.orientation = HORIZONTAL; ruler.setBackgroundColor(Color.rgb(31,35,40))
        val tools = LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(4,0,4,0)
            addView(label("TIMELINE",66), LinearLayout.LayoutParams(66,-1))
            addView(smallButton("-",28) { setZoom(zoom/1.25f) })
            addView(smallButton("+",28) { setZoom(zoom*1.25f) })
            addView(label("${(zoom*100).toInt()}%",42), LinearLayout.LayoutParams(42,-1))
            addView(smallButton("In",30) { setPlaybackStart() })
            addView(smallButton("Out",30) { setPlaybackEnd() })
        }
        ruler.addView(tools, LinearLayout.LayoutParams(layerHeaderWidth,-1))
        for (frame in 0 until document.duration) {
            val width = frameWidth(); val fps = document.fps.coerceAtLeast(1); val isSecond = frame % fps == 0; val isMajor = frame % (fps/2).coerceAtLeast(1) == 0
            ruler.addView(TextView(context).apply {
                text = if (isSecond) "${frame/fps}s" else ""; textSize = if (isSecond) 8f else 6f; gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; setPadding(0,0,0,2)
                setTextColor(if(frame==document.currentFrame) Color.WHITE else Color.LTGRAY)
                setBackgroundColor(when { frame==document.currentFrame -> Color.rgb(58,64,72); frame in document.playbackStart..document.playbackEnd && isSecond -> Color.rgb(43,50,58); isMajor -> Color.rgb(37,41,47); else -> Color.rgb(31,35,40) })
            }, LinearLayout.LayoutParams(width,38))
        }
    }

    fun refresh(accent: Int) {
        buildRuler(); rows.removeAllViews()
        document.layers.forEach { layer -> rows.addView(buildLayerRow(layer,accent), LinearLayout.LayoutParams(trackWidth()+layerHeaderWidth,74)) }
    }

    private fun buildLayerRow(layer: AnimationLayer, accent: Int): View {
        val row = LinearLayout(context).apply { orientation=HORIZONTAL; setBackgroundColor(if(layer.id==document.selectedLayerId) Color.rgb(39,44,51) else Color.rgb(27,30,35)) }
        val header = LinearLayout(context).apply { orientation=VERTICAL; setPadding(10,5,6,4); gravity=Gravity.CENTER_VERTICAL }
        val name = TextView(context).apply { text=if(layer.isBackground) "Fundo" else layer.name; textSize=12f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); setOnClickListener { if(!layer.isBackground) onLayerSelected(layer.id) } }
        header.addView(name, LinearLayout.LayoutParams(-1,32))
        val controls = LinearLayout(context).apply { orientation=HORIZONTAL }
        controls.addView(smallButton(if(layer.visible) "V" else "-",28) { layer.visible=!layer.visible; onLayerChanged() })
        controls.addView(smallButton(if(layer.locked) "L" else "U",28) { if(!layer.isBackground){layer.locked=!layer.locked; onLayerChanged()} })
        header.addView(controls,LinearLayout.LayoutParams(-1,30)); row.addView(header,LinearLayout.LayoutParams(layerHeaderWidth,-1))
        val track = LinearLayout(context).apply { orientation=HORIZONTAL; setBackgroundColor(Color.rgb(20,23,27)) }
        for(frame in 0 until document.duration){
            val cell=TextView(context).apply{
                text=when{frame==document.currentFrame->"●";layer.frameAt(frame)!=null->if(layer.frameAt(frame)?.strokes.isNullOrEmpty())"○" else "●";else->""}; gravity=Gravity.CENTER; textSize=14f
                setTextColor(if(frame==document.currentFrame)accent else Color.LTGRAY)
                setBackgroundColor(when{frame==document.currentFrame->Color.rgb(61,68,77);layer.frameAt(frame)!=null->Color.rgb(49,56,64);else->Color.rgb(30,34,39)})
                setOnClickListener{onFrameSelected(layer.id,frame)}
            }
            track.addView(cell,LinearLayout.LayoutParams(frameWidth(),68).apply{setMargins(2,3,2,3)})
        }
        row.addView(track,LinearLayout.LayoutParams(trackWidth(),-1)); return row
    }

    private fun setZoom(value:Float){zoom=value.coerceIn(.5f,4f);refresh(ThemeColorStore.get(context))}
    private fun setPlaybackStart(){document.playbackStart=document.currentFrame.coerceIn(0,document.duration-1);if(document.playbackEnd<document.playbackStart)document.playbackEnd=document.playbackStart;refresh(ThemeColorStore.get(context))}
    private fun setPlaybackEnd(){document.playbackEnd=document.currentFrame.coerceIn(document.playbackStart,document.duration-1);refresh(ThemeColorStore.get(context))}
    private fun label(text:String,width:Int)=TextView(context).apply{this.text=text;textSize=9f;gravity=Gravity.CENTER;setTextColor(Color.LTGRAY);setPadding(2,0,2,0)}
    private fun smallButton(text:String,width:Int,action:()->Unit)=Button(context).apply{this.text=text;textSize=9f;setPadding(0,0,0,0);setOnClickListener{action()};layoutParams=LinearLayout.LayoutParams(width,32)}
}
