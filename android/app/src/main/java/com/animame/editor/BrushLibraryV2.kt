package com.animame.editor

/**
 * Procedural brush library: 64 families x 16 physically different variants = 1024 presets.
 * A preset changes kernel, spacing, texture, pressure curve, scatter, rotation, fade and mixing;
 * the catalog is data-driven so the APK does not need 1024 bitmap files.
 */
enum class BrushKernel { ROUND, HARD, SOFT, CHALK, CHARCOAL, PENCIL, INK, WATERCOLOR, OIL, GOUACHE, AIRBRUSH, DRY, SPRAY, RIBBON, STAMP, CALLIGRAPHY }
enum class BrushTexture { NONE, PAPER, GRAIN, FIBER, CANVAS, TOOTH, SAND, WET, ROUGH, SPECKLE, DUST, FABRIC, METAL, LEAF, CLOUD, DOT }
enum class BrushDynamics { CONSTANT, PRESSURE, VELOCITY, PRESSURE_VELOCITY, TILT, PRESSURE_TILT, VELOCITY_TILT, INK_FLOW }

data class BrushSpec(
    val id:String,val name:String,val session:String,val category:String,val subcategory:String,
    val kernel:BrushKernel,val texture:BrushTexture,val dynamics:BrushDynamics,
    val sizeScale:Float,val opacityScale:Float,val spacing:Float,val scatter:Float,
    val rotation:Float,val grain:Float,val flow:Float,val fadeStart:Float,val fadeEnd:Float,
    val pressureExponent:Float,val velocityResponse:Float,val jitter:Float,val mix:Float
)

object BrushLibraryV2 {
    private val sessions=listOf("Ink","Pencil","Charcoal","Paint","Watercolor","Gouache","Oil","Airbrush","Marker","Pastel","Chalk","Texture","Pattern","Calligraphy","Effects","Animation")
    private val kernels=BrushKernel.values()
    private val textures=BrushTexture.values()
    private val dynamics=BrushDynamics.values()
    private val categoryNames=listOf("Line","Sketch","Fill","Dry Media","Wet Media","Texture","Stamp","Effect")
    private val names=listOf("Basic","Soft","Hard","Edge","Mix","Opaque","Fade","Grain","Rough","Smooth","Fine","Heavy","Dry","Wet","Dense","Light")

    val all:List<BrushSpec> by lazy { buildList(1024) {
        repeat(1024){i->
            val family=i/16; val variant=i%16
            val session=sessions[family % sessions.size]
            val kernel=kernels[(family*3+variant)%kernels.size]
            val texture=textures[(family+variant*5)%textures.size]
            val dynamics=dynamics[(family*2+variant)%dynamics.size]
            val category=categoryNames[(family+variant)%categoryNames.size]
            val sub=names[variant]
            val seed=(i*1103515245L+12345L) and 0x7fffffff
            fun f(n:Int)=((seed ushr (n%24))%1000)/1000f
            add(BrushSpec(
                id="anima_${i+1}",name="$session $category $sub ${variant+1}",session=session,category=category,subcategory=sub,
                kernel=kernel,texture=texture,dynamics=dynamics,
                sizeScale=.55f+f(1)*1.35f,opacityScale=.45f+f(3)*.55f,
                spacing=.045f+f(5)*.28f,scatter=f(7)*.85f,rotation=f(9)*360f,grain=.1f+f(11)*.9f,
                flow=.25f+f(13)*.75f,fadeStart=f(15)*.45f,fadeEnd=.55f+f(17)*.45f,
                pressureExponent=.55f+f(19)*1.6f,velocityResponse=f(21),jitter=f(23)*.32f,mix=f(4)
            ))
        }
    }}

    fun search(query:String="",session:String?=null,category:String?=null):List<BrushSpec>{
        val q=query.trim().lowercase()
        return all.filter{(q.isEmpty()||it.name.lowercase().contains(q)||it.subcategory.lowercase().contains(q))&&(session==null||it.session==session)&&(category==null||it.category==category)}
    }
    fun sessions():List<String> = sessions
    fun categories(session:String?=null):List<String> = all.filter{session==null||it.session==session}.map{it.category}.distinct()
    fun subcategories(session:String?=null,category:String?=null):List<String> = all.filter{(session==null||it.session==session)&&(category==null||it.category==category)}.map{it.subcategory}.distinct()
    fun byId(id:String)=all.firstOrNull{it.id==id}
}
