package com.animame.editor

/** Procedural brush parameters. All presets use original Anima-me logic; no proprietary brush assets are bundled. */
data class BrushSettings(
    var size:Float=12f,var sizeMin:Float=1f,var sizeMax:Float=512f,var opacity:Float=1f,var opacityMin:Float=0f,var flow:Float=1f,var alpha:Float=1f,var hardness:Float=1f,var feather:Float=0f,var spacing:Float=.15f,
    var startThickness:Float=1f,var endThickness:Float=1f,var startOpacity:Float=1f,var endOpacity:Float=1f,var forceFadeOut:Boolean=false,var blurShape:Int=0,var blurDegree:Float=0f,var blurStart:Float=0f,var blurMiddle:Float=0f,var blurEnd:Float=0f,var jitterBlur:Float=0f,var fade:Float=0f,var fadeIn:Float=0f,var fadeOut:Float=0f,var fadeStartTimeMs:Float=0f,var fadeEndTimeMs:Float=0f,
    var brushPattern:String="round",var patternOpacity:Float=1f,var opacitySaturation:Boolean=true,var decreaseOpacity:Boolean=false,var initialAngle:Float=0f,var followingRotation:Boolean=false,var aspect:Float=1f,
    var jitter:Float=0f,var jitterPosition:Float=0f,var jitterThickness:Float=0f,var jitterOpacity:Float=0f,var jitterSpacing:Float=0f,var jitterAngle:Float=0f,var scatter:Boolean=false,var absoluteParticleSize:Boolean=false,var particleSize:Float=12f,var particleDensity:Float=0f,var particleDeviation:Float=0f,
    var brushType:String="opaque",var texturePattern:String="none",var textureStrength:Float=0f,var textureScale:Float=1f,var textureRotation:Float=0f,var textureOffset:Float=0f,
    var speedThickness:Float=0f,var speedOpacity:Float=0f,var speedBlurring:Float=0f,var pressureSize:Float=0f,var pressureOpacity:Float=0f,var pressureFlow:Float=0f,var pressureBlurring:Float=0f,var tiltSize:Float=0f,var tiltOpacity:Float=0f,var angle:Float=0f,var roundness:Float=1f,
    var blendMode:String="normal",var lockAlpha:Boolean=false,var eraser:Boolean=false,var antialias:Boolean=true,var disablePrediction:Boolean=false,var stabilizerOverride:Float?=null,var stabilizerConstant:Float=0f,var stabilizerFastStrokes:Float=0f,var locked:Boolean=false,var customName:String?=null
){
 fun normalized()=copy(size=size.coerceIn(sizeMin.coerceAtLeast(.1f),sizeMax.coerceAtLeast(sizeMin)),sizeMin=sizeMin.coerceAtLeast(.1f),sizeMax=sizeMax.coerceAtLeast(sizeMin.coerceAtLeast(.1f)),opacity=opacity.coerceIn(0f,1f),opacityMin=opacityMin.coerceIn(0f,1f),flow=flow.coerceIn(0f,1f),alpha=alpha.coerceIn(0f,1f),hardness=hardness.coerceIn(0f,1f),feather=feather.coerceIn(0f,1f),spacing=spacing.coerceIn(.01f,4f),startThickness=startThickness.coerceIn(0f,2f),endThickness=endThickness.coerceIn(0f,2f),startOpacity=startOpacity.coerceIn(0f,1f),endOpacity=endOpacity.coerceIn(0f,1f),blurDegree=blurDegree.coerceIn(0f,1f),blurStart=blurStart.coerceIn(0f,1f),blurMiddle=blurMiddle.coerceIn(0f,1f),blurEnd=blurEnd.coerceIn(0f,1f),jitterBlur=jitterBlur.coerceIn(0f,1f),fade=fade.coerceIn(0f,1f),fadeIn=fadeIn.coerceIn(0f,1f),fadeOut=fadeOut.coerceIn(0f,1f),patternOpacity=patternOpacity.coerceIn(0f,1f),aspect=aspect.coerceIn(.05f,4f),jitter=jitter.coerceIn(0f,1f),jitterPosition=jitterPosition.coerceIn(0f,1f),jitterThickness=jitterThickness.coerceIn(0f,1f),jitterOpacity=jitterOpacity.coerceIn(0f,1f),jitterSpacing=jitterSpacing.coerceIn(0f,1f),jitterAngle=jitterAngle.coerceIn(0f,1f),particleDensity=particleDensity.coerceIn(0f,1f),particleDeviation=particleDeviation.coerceIn(-1f,1f),textureStrength=textureStrength.coerceIn(0f,1f),textureScale=textureScale.coerceAtLeast(.01f),speedThickness=speedThickness.coerceIn(-1f,1f),speedOpacity=speedOpacity.coerceIn(-1f,1f),speedBlurring=speedBlurring.coerceIn(-1f,1f),pressureSize=pressureSize.coerceIn(-1f,1f),pressureOpacity=pressureOpacity.coerceIn(-1f,1f),pressureFlow=pressureFlow.coerceIn(-1f,1f),pressureBlurring=pressureBlurring.coerceIn(-1f,1f),tiltSize=tiltSize.coerceIn(-1f,1f),tiltOpacity=tiltOpacity.coerceIn(-1f,1f),roundness=roundness.coerceIn(.05f,1f),stabilizerConstant=stabilizerConstant.coerceIn(0f,1f),stabilizerFastStrokes=stabilizerFastStrokes.coerceIn(0f,1f))
 fun radiusFor(pressure:Float,tilt:Float=0f,speed:Float=0f):Float {
  val p=pressure.coerceIn(0f,1f);val t=(tilt.coerceIn(0f,1.5708f)/1.5708f);val v=speed.coerceIn(0f,1f)
  var f=(1f+pressureSize*(p-.5f)*2f).coerceIn(.05f,2f)*(1f+tiltSize*t).coerceIn(.05f,2f)*(1f+speedThickness*(v-.5f)*2f).coerceIn(.05f,2f)
  f*=when(brushType.lowercase()){"graphite"->.82f+.22f*p;"charcoal","dry","pastel"->.72f+.32f*p;"ink"->.78f+.45f*p;"marker"->.92f+.12f*p;"water"->1.08f+.12f*p;"paint"->.9f+.24f*p;"air"->1.15f;"particle","nature","effect","stamp"->.72f+.5f*p;"eraser"->.98f;else->1f}
  if(jitterThickness>0f)f*=1f+jitterThickness*(sin((p*37f+speed*113f+size)*6.283f)*.5f)
  val shape=(.72f+.28f*aspect.coerceIn(.05f,1f).sqrt())*(.8f+.2f*roundness)
  return(size*f*shape).coerceIn(sizeMin,sizeMax)
 }
 fun opacityFor(pressure:Float,tilt:Float=0f,distance:Float=0f,strokeLength:Float=0f,speed:Float=0f):Float {
  val p=pressure.coerceIn(0f,1f);val t=tilt.coerceIn(0f,1.5708f)/1.5708f;val v=speed.coerceIn(0f,1f);var x=opacity*alpha*flow
  x*=(1f+pressureOpacity*(p-.5f)*2f).coerceIn(0f,2f);x*=(1f+tiltOpacity*t).coerceIn(0f,2f);x*=(1f+speedOpacity*(v-.5f)*2f).coerceIn(0f,2f)
  x*=when(brushType.lowercase()){"graphite"->.7f+.3f*(.5f+.5f*sin(distance/max(1f,size)*5f));"charcoal","dry","pastel"->.65f+.35f*(.5f+.5f*sin(distance/max(1f,size)*9f));"ink"->.9f+.1f*p;"marker"->if(opacitySaturation)1f else .85f;"water"->.58f+.42f*(1f-feather*.35f);"air"->.35f+.65f*(1f-feather*.45f);"paint"->.82f+.18f*p;"particle","nature","effect","stamp"->.72f+.28f*particleDensity;"eraser"->1f;else->.8f+.2f*hardness}
  if(hardness<1f)x*=.75f+.25f*hardness
  if(textureStrength>0f)x*=.86f+.14f*(.5f+.5f*sin((distance/ max(.1f,size))*textureScale*13f+textureOffset))
  if(strokeLength>0f){val u=(distance/strokeLength).coerceIn(0f,1f);x*=startOpacity+(endOpacity-startOpacity)*u;x*=startThickness+(endThickness-startThickness)*u;if(fade>0f)x*=1f-u*fade;if(fadeIn>0f)x*=(u/(fadeIn.coerceAtLeast(.001f))).coerceIn(0f,1f);if(fadeOut>0f)x*=((1f-u)/(fadeOut.coerceAtLeast(.001f))).coerceIn(0f,1f);if(forceFadeOut)x*=1f-u*u}
  if(jitterOpacity>0f)x*=1f-jitterOpacity*(.5f+.5f*sin(distance*0.17f+size))
  return x.coerceIn(opacityMin,1f)
 }
}

object BrushDefaults {
 fun forPreset(id:String):BrushSettings {val x=id.lowercase();return when{
  x.contains("eraser")->BrushSettings(size=24f,hardness=if(x.contains("soft")) .18f else .9f,feather=if(x.contains("soft")) .72f else .06f,spacing=.08f,pressureSize=.55f,pressureOpacity=.1f,eraser=true,brushType="eraser")
  x.contains("pencil")||x.contains("graphite")||x.contains("sketch")->BrushSettings(size=6f,opacity=.68f,flow=.78f,hardness=.58f,feather=.24f,spacing=.07f,pressureSize=.72f,pressureOpacity=.28f,jitter=.08f,jitterPosition=.12f,jitterOpacity=.18f,textureStrength=.42f,textureScale=1.3f,brushType="graphite")
  x.contains("charcoal")->BrushSettings(size=14f,opacity=.6f,flow=.7f,hardness=.35f,feather=.35f,spacing=.11f,jitter=.2f,jitterPosition=.25f,jitterOpacity=.28f,textureStrength=.75f,brushType="charcoal")
  x.contains("comic")||x.contains("manga")||x.contains("gpen")||x.contains("mapping")||x.contains("technical")||x.contains("fountain")||x.startsWith("ink")->BrushSettings(size=8f,opacity=.95f,flow=.95f,hardness=.96f,feather=.015f,spacing=.055f,pressureSize=.82f,pressureOpacity=.3f,followingRotation=true,brushType="ink")
  x.contains("screentone")->BrushSettings(size=16f,opacity=.72f,flow=.8f,spacing=.5f,scatter=true,particleSize=2.2f,particleDensity=.65f,textureStrength=.7f,brushType="screentone")
  x.contains("speedline")->BrushSettings(size=5f,opacity=.9f,flow=.9f,spacing=.18f,pressureSize=.3f,brushType="speedline")
  x.contains("marker")||x.contains("felt")||x.contains("highlighter")->BrushSettings(size=20f,opacity=if(x.contains("highlighter")) .35f else .78f,flow=.9f,hardness=.7f,feather=.08f,spacing=.16f,aspect=.65f,followingRotation=true,pressureSize=.25f,brushType="marker")
  x.contains("pastel")||x.contains("crayon")->BrushSettings(size=18f,opacity=.66f,flow=.72f,hardness=.38f,feather=.3f,spacing=.1f,jitter=.15f,jitterPosition=.18f,jitterOpacity=.22f,textureStrength=.72f,brushType="pastel")
  x.contains("chalk")||x.contains("dry")->BrushSettings(size=16f,opacity=.62f,flow=.68f,hardness=.42f,feather=.28f,spacing=.09f,jitter=.24f,jitterPosition=.28f,jitterOpacity=.25f,textureStrength=.82f,brushType="dry")
  x.contains("watercolor")||x.contains("gouache")||x=="water"||x.contains("wash")->BrushSettings(size=26f,opacity=if(x.contains("gouache")) .72f else .5f,flow=.42f,hardness=if(x.contains("edge")) .42f else .2f,feather=if(x.contains("edge")) .35f else .62f,spacing=.14f,pressureSize=.35f,pressureOpacity=.38f,textureStrength=.4f,brushType="water",texturePattern="paper")
  x.contains("acrylic")||x.contains("oil")||x.contains("impasto")||x=="paint"->BrushSettings(size=20f,opacity=.9f,flow=.78f,hardness=.62f,feather=.12f,spacing=.1f,pressureSize=.5f,pressureOpacity=.25f,jitter=.06f,textureStrength=if(x.contains("impasto")) .78f else .38f,brushType="paint")
  x.contains("smudge")||x.contains("blend")||x.contains("mixing")->BrushSettings(size=28f,opacity=.6f,flow=.72f,hardness=if(x.contains("hard")) .72f else .2f,feather=.45f,spacing=.09f,pressureSize=.35f,brushType="smudge")
  x.contains("airbrush")->BrushSettings(size=48f,opacity=if(x.contains("hard")) .45f else .3f,flow=.34f,hardness=if(x.contains("hard")) .22f else .04f,feather=if(x.contains("hard")) .65f else .95f,spacing=.06f,brushType="air")
  x.contains("spray")||x.contains("speckle")||x.contains("dust")->BrushSettings(size=30f,opacity=.5f,flow=.55f,spacing=.35f,jitter=.65f,jitterPosition=.8f,jitterOpacity=.45f,scatter=true,particleSize=if(x.contains("dust")) 4f else 7f,particleDensity=.7f,brushType="particle")
  x.contains("particle")||x.contains("stars")||x.contains("spark")||x.contains("glitter")->BrushSettings(size=14f,opacity=.85f,flow=.8f,spacing=.55f,jitter=.55f,jitterPosition=.75f,scatter=true,particleSize=8f,particleDensity=.9f,brushType="particle")
  x.contains("grass")||x.contains("leaf")||x.contains("leaves")||x.contains("fur")||x.contains("hair")||x.contains("cloud")||x.contains("vine")||x.contains("flower")||x.contains("branch")||x.contains("bamboo")||x.contains("sand")||x.contains("waves")->BrushSettings(size=18f,opacity=.75f,flow=.7f,spacing=.22f,jitter=.42f,jitterPosition=.55f,jitterAngle=.5f,scatter=true,particleSize=8f,particleDensity=.5f,brushType="nature")
  x.contains("fire")||x.contains("smoke")->BrushSettings(size=22f,opacity=.7f,flow=.55f,hardness=.15f,feather=.7f,spacing=.25f,jitter=.65f,jitterPosition=.75f,jitterOpacity=.35f,scatter=true,particleSize=10f,particleDensity=.55f,brushType="effect")
  x.contains("light")||x.contains("bloom")||x.contains("neon")->BrushSettings(size=24f,opacity=.8f,flow=.8f,hardness=.08f,feather=.8f,spacing=.12f,brushType="glow")
  x.contains("chain")||x.contains("ribbon")->BrushSettings(size=14f,opacity=.9f,flow=.9f,spacing=.5f,followingRotation=true,brushType="stamp")
  x.contains("texture")||x.contains("paper")||x.contains("canvas")||x.contains("noise")->BrushSettings(size=24f,opacity=.5f,flow=.65f,spacing=.18f,jitter=.3f,jitterPosition=.35f,textureStrength=.9f,textureScale=1.4f,brushType="texture")
  else->BrushSettings()
 }}
}
