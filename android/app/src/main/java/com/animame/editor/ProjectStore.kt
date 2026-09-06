package com.animame.editor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AnimationProject(
    val id:String=UUID.randomUUID().toString(),var name:String="Untitled",var fps:Int=24,
    var cameraWidth:Int=1280,var cameraHeight:Int=720,var margin:Int=0,
    var canvasWidth:Int=1280,var canvasHeight:Int=720,var lastFrame:Int=0
)

/** Persistent project index. The editor intentionally starts in this browser every launch. */
object ProjectStore {
    private const val PREFS="anima_me_projects"
    private const val KEY="index"
    private var prefs:android.content.SharedPreferences?=null
    private var loaded=false
    val projects=mutableListOf<AnimationProject>()

    fun initialize(context:Context){
        if(loaded)return
        prefs=context.applicationContext.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        projects.clear()
        runCatching{
            val a=JSONArray(prefs?.getString(KEY,"[]")?:"[]")
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                projects += AnimationProject(
                    id=o.optString("id",UUID.randomUUID().toString()),name=o.optString("name","Untitled"),
                    fps=o.optInt("fps",24),cameraWidth=o.optInt("cameraWidth",1280),cameraHeight=o.optInt("cameraHeight",720),
                    margin=o.optInt("margin",0),canvasWidth=o.optInt("canvasWidth",1280),canvasHeight=o.optInt("canvasHeight",720),
                    lastFrame=o.optInt("lastFrame",0)
                )
            }
        }
        loaded=true
    }

    fun create(name:String,fps:Int,w:Int,h:Int,margin:Int=0):AnimationProject{
        val m=margin.coerceAtLeast(0)
        val p=AnimationProject(name=name.ifBlank{"Untitled"},fps=fps.coerceIn(1,240),cameraWidth=w.coerceIn(1,16384),cameraHeight=h.coerceIn(1,16384),margin=m,canvasWidth=w.coerceAtLeast(1)+m*2,canvasHeight=h.coerceAtLeast(1)+m*2)
        projects.add(0,p);save();return p
    }
    fun touch(p:AnimationProject){val i=projects.indexOfFirst{it.id==p.id};if(i>=0){projects.removeAt(i);projects.add(0,p);save()}}
    fun delete(id:String){projects.removeAll{it.id==id};save()}
    fun update(p:AnimationProject){val i=projects.indexOfFirst{it.id==p.id};if(i>=0){projects[i]=p;save()}}
    private fun save(){
        val a=JSONArray();projects.forEach{p->a.put(JSONObject().apply{put("id",p.id);put("name",p.name);put("fps",p.fps);put("cameraWidth",p.cameraWidth);put("cameraHeight",p.cameraHeight);put("margin",p.margin);put("canvasWidth",p.canvasWidth);put("canvasHeight",p.canvasHeight);put("lastFrame",p.lastFrame)})}
        prefs?.edit()?.putString(KEY,a.toString())?.apply()
    }
}
