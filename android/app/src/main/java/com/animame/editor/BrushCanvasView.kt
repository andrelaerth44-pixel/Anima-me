package com.animame.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class BrushCanvasView(context:Context):View(context){
    var settings:BrushSettings=BrushCatalog.presets.first().settings
    var size:Float=settings.size
    var opacity:Float=settings.opacity
    private val strokes=mutableListOf<StrokeData>();private var active:StrokeData?=null
    private val bg=Paint().apply{color=Color.WHITE}
    override fun onDraw(c:Canvas){c.drawRect(0f,0f,width.toFloat(),height.toFloat(),bg);strokes.forEach{BrushEngine.drawStroke(c,it,it.color)};active?.let{BrushEngine.drawStroke(c,it,it.color)}}
    override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{active=StrokeData(brushId=settings.id,color=Color.BLACK,size=size,opacity=opacity,settings=settings.copy(size=size,opacity=opacity),samples=mutableListOf(StrokeSample(e.x,e.y,e.pressure,e.eventTime)));invalidate()};MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP->{active?.samples?.add(StrokeSample(e.x,e.y,e.pressure,e.eventTime));if(e.actionMasked==MotionEvent.ACTION_UP){active?.let{strokes+=it};active=null};invalidate()};MotionEvent.ACTION_CANCEL->{active=null;invalidate()}};return true}
    fun clear(){strokes.clear();active=null;invalidate()}
}
