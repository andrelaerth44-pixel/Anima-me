package com.animame.editor

data class VideoImportSettings(val preserveEveryFrame:Boolean=true,val preserveAlpha:Boolean=true,val targetFps:Int?=null)
data class Mp4ExportSettings(val width:Int,val height:Int,val fps:Int,val startFrame:Int,val endFrame:Int,val bitrate:Int=8_000_000)
data class GifExportSettings(val width:Int,val height:Int,val fps:Int,val startFrame:Int,val endFrame:Int,val loop:Boolean=true)
data class ImageSequenceExportSettings(val width:Int,val height:Int,val startFrame:Int,val endFrame:Int,val zip:Boolean=true)
// validation 6
