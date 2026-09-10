package com.animame.editor

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** Converts pointer samples into deterministic brush dabs. Different kernels use different math. */
data class BrushDab(val x:Float,val y:Float,val radius:Float,val alpha:Float,val angle:Float,val texture:Float)

object BrushStrokeProcessorV2 {
    fun generate(spec:BrushSpec,samples:List<StrokeSample>,baseSize:Float,baseOpacity:Float):List<BrushDab>{
        if(samples.isEmpty()) return emptyList()
        val out=ArrayList<BrushDab>(samples.size*2)
        var distance=0f
        for(i in samples.indices){
            val s=samples[i]; val p=samples.getOrNull(i-1)
            val speed=if(p==null)0f else sqrt((s.x-p.x)*(s.x-p.x)+(s.y-p.y)*(s.y-p.y))/maxOf(1f,(s.timeMs-p.timeMs).toFloat())
            val pressure=s.pressure.coerceIn(0f,1f)
            val dynamic=when(spec.dynamics){
                BrushDynamics.CONSTANT->1f
                BrushDynamics.PRESSURE->pressure
                BrushDynamics.VELOCITY->(1f-speed*spec.velocityResponse).coerceIn(.08f,1f)
                BrushDynamics.PRESSURE_VELOCITY->(pressure*(1f-speed*spec.velocityResponse*.7f)).coerceIn(.05f,1f)
                BrushDynamics.TILT->1f
                BrushDynamics.PRESSURE_TILT->pressure
                BrushDynamics.VELOCITY_TILT->(1f-speed*spec.velocityResponse).coerceIn(.08f,1f)
                BrushDynamics.INK_FLOW->(spec.flow*(.35f+.65f*pressure)).coerceIn(.02f,1f)
            }
            val radius=baseSize*.5f*spec.sizeScale*(.35f+.65f*dynamic).pow(spec.pressureExponent)
            val alpha=baseOpacity*spec.opacityScale*(.25f+.75f*dynamic)
            val angle=when(spec.kernel){BrushKernel.RIBBON,BrushKernel.CALLIGRAPHY->if(p==null)0f else kotlin.math.atan2(s.y-p.y,s.x-p.x);BrushKernel.STAMP->spec.rotation;else->0f}
            val texture=when(spec.texture){BrushTexture.NONE->1f;BrushTexture.WET->.75f+spec.mix*.25f;BrushTexture.SPECKLE->(sin(s.x*.37f+s.y*.11f)*.5f+.5f);BrushTexture.DUST->(cos(s.x*.13f-s.y*.29f)*.5f+.5f);else->.55f+spec.grain*.45f}
            out += BrushDab(s.x,s.y,radius,alpha,angle,texture)
            if(spec.kernel==BrushKernel.SPRAY || spec.scatter>.45f){
                val count=(1+spec.scatter*4).toInt()
                repeat(count){j-> val a=(j*2.399963f+spec.rotation)*Math.PI.toFloat()/180f; val r=radius*(.5f+spec.scatter*2f)*(j+1)/(count+1f); out += BrushDab(s.x+cos(a)*r,s.y+sin(a)*r,radius*(.15f+.35f*spec.flow),alpha*.35f,angle,texture) }
            }
            distance += if(p==null)0f else sqrt((s.x-p.x)*(s.x-p.x)+(s.y-p.y)*(s.y-p.y))
        }
        return out
    }
    private fun Float.pow(p:Float)=exp(kotlin.math.ln(coerceAtLeast(.0001f))*p)
}
