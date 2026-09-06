package com.animame.editor

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Project browser matching the supplied RoughAnimator reference flow. */
class ProjectHomeView(
    context: Context,
    private val onOpen: (AnimationDocument, AnimationProject) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sortByName = false

    init { setBackgroundColor(Color.rgb(52, 54, 57)) }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(52,54,57)
        c.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
        text(c,"ANIMA-ME",22f,34f,Color.WHITE,18f,true)
        round(c,width-160f,7f,width-88f,45f,Color.rgb(74,77,81),7f)
        text(c,"▣+",width-141f,31f,Color.WHITE,18f,false)
        round(c,width-88f,7f,width-8f,45f,if(sortByName) Color.rgb(255,246,218) else Color.rgb(74,77,81),6f)
        text(c,if(sortByName)"Name" else "Date",width-76f,30f,if(sortByName)Color.DKGRAY else Color.WHITE,10f,false)

        val projects = if(sortByName) ProjectStore.projects.sortedBy { it.name.lowercase() } else ProjectStore.projects.toList()
        var y=58f
        if(projects.isEmpty()) text(c,"No projects",40f,100f,Color.LTGRAY,14f,false)
        else projects.forEach { p ->
            round(c,8f,y,width/2f-8f,y+64f,Color.rgb(69,72,77),2f)
            paint.color=Color.WHITE; c.drawRect(16f,y+17f,62f,y+47f,paint)
            text(c,p.name,72f,y+32f,Color.WHITE,14f,false)
            text(c,"${p.fps} fps • ${p.cameraWidth}×${p.cameraHeight}",72f,y+51f,Color.LTGRAY,9f,false)
            y += 70f
        }
        round(c,width/2f+8f,height-56f,width-8f,height-8f,Color.rgb(92,92,92),5f)
        text(c,"New project",width*0.75f,height-27f,Color.WHITE,13f,false)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(e.action != MotionEvent.ACTION_UP) return true
        val x=e.x; val y=e.y
        if(y < 52f && x > width-90f) { sortByName=!sortByName; invalidate(); return true }
        if(y > height-70f && x > width/2f) { showCreateDialog(); return true }
        val projects = if(sortByName) ProjectStore.projects.sortedBy { it.name.lowercase() } else ProjectStore.projects.toList()
        val index=((y-58f)/70f).toInt()
        if(index in projects.indices && x < width/2f) {
            val p=projects[index]
            val d=ProjectDocumentStore.load(context,p.id) ?: AnimationDocument(p.name,p.canvasWidth,p.canvasHeight,p.fps,1)
            ProjectStore.touch(p)
            onOpen(d,p)
        }
        return true
    }

    private fun showCreateDialog() {
        val box=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(24,0,24,0) }
        fun field(label:String,value:String): EditText {
            box.addView(TextView(context).apply { text=label; setTextColor(Color.LTGRAY); textSize=13f; setPadding(0,10,0,0) })
            return EditText(context).apply { setText(value); inputType=2 }.also(box::addView)
        }
        val name=EditText(context).apply { hint="New project name"; setText("Untitled") }
        box.addView(TextView(context).apply { text="New project name"; setTextColor(Color.LTGRAY); textSize=13f; setPadding(0,10,0,0) })
        box.addView(name)
        val fps=field("Frames per second","24")
        val w=field("Camera width","1280")
        val h=field("Camera height","720")
        val mw=field("+ Margins width","0")
        val mh=field("+ Margins height","0")

        AlertDialog.Builder(context).setTitle("New project").setView(box)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Okay") { _,_ ->
                val n=name.text.toString().trim().ifBlank { "Untitled" }
                val f=fps.text.toString().toIntOrNull()?.coerceIn(1,240) ?: 24
                val ww=w.text.toString().toIntOrNull()?.coerceIn(1,16384) ?: 1280
                val hh=h.text.toString().toIntOrNull()?.coerceIn(1,16384) ?: 720
                val marginX=mw.text.toString().toIntOrNull()?.coerceIn(0,8192) ?: 0
                val marginY=mh.text.toString().toIntOrNull()?.coerceIn(0,8192) ?: 0
                val canvasW=(ww+marginX*2).coerceIn(1,16384)
                val canvasH=(hh+marginY*2).coerceIn(1,16384)
                val p=ProjectStore.create(n,f,ww,hh,maxOf(marginX,marginY))
                p.canvasWidth=canvasW; p.canvasHeight=canvasH; ProjectStore.update(p)
                val d=ProjectDocumentStore.load(context,p.id) ?: AnimationDocument(n,canvasW,canvasH,f,1)
                d.name=n; d.width=canvasW; d.height=canvasH; d.fps=f; d.duration=1; d.currentFrame=0; d.normalize()
                ProjectDocumentStore.save(context,p.id,d)
                ProjectStore.touch(p)
                onOpen(d,p)
            }.show()
    }

    private fun round(c:Canvas,l:Float,t:Float,r:Float,b:Float,color:Int,rad:Float){paint.color=color;paint.style=Paint.Style.FILL;c.drawRoundRect(l,t,r,b,rad,rad,paint)}
    private fun text(c:Canvas,s:String,x:Float,y:Float,color:Int,size:Float,bold:Boolean=false){paint.color=color;paint.textSize=size;paint.typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;paint.style=Paint.Style.FILL;c.drawText(s,x,y,paint)}
}
