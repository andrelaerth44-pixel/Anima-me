package com.animame.editor

/** Capability registry used by the Android UI so tools are not scattered as ad-hoc buttons. */
enum class ToolGroup { DRAW, SELECT, TRANSFORM, ANIMATION, CAMERA, RULER, COLOR, FILTER, MATERIAL, MEDIA, PROJECT }

data class ToolCapability(val id:String,val label:String,val group:ToolGroup,val implemented:Boolean=true)

object ProfessionalFeatureCatalog {
    val tools=listOf(
        ToolCapability("brush","Brush",ToolGroup.DRAW),ToolCapability("eraser","Eraser",ToolGroup.DRAW),ToolCapability("fill","Fill",ToolGroup.DRAW),ToolCapability("lasso","Lasso",ToolGroup.SELECT),
        ToolCapability("eyedropper","Eyedropper",ToolGroup.COLOR),ToolCapability("transform","Transform",ToolGroup.TRANSFORM),ToolCapability("liquify","Liquify",ToolGroup.TRANSFORM),
        ToolCapability("camera","Camera",ToolGroup.CAMERA),ToolCapability("onion_skin","Onion Skin",ToolGroup.ANIMATION),ToolCapability("timeline","Timeline",ToolGroup.ANIMATION),ToolCapability("cycle","Drawing Cycle",ToolGroup.ANIMATION),
        ToolCapability("edit_multiple","Edit Multiple",ToolGroup.ANIMATION),ToolCapability("audio","Audio",ToolGroup.MEDIA),ToolCapability("video_import","Video Import",ToolGroup.MEDIA),ToolCapability("image_sequence","Image Sequence",ToolGroup.MEDIA),
        ToolCapability("symmetry","Symmetry Ruler",ToolGroup.RULER),ToolCapability("perspective","Perspective Ruler",ToolGroup.RULER),ToolCapability("radial","Radial Ruler",ToolGroup.RULER),ToolCapability("parallel","Parallel Ruler",ToolGroup.RULER),
        ToolCapability("gradient","Gradient",ToolGroup.COLOR),ToolCapability("blend","Blend Modes",ToolGroup.COLOR),ToolCapability("filters","Filters",ToolGroup.FILTER),ToolCapability("materials","Materials",ToolGroup.MATERIAL),
        ToolCapability("text","Text",ToolGroup.PROJECT),ToolCapability("qr_brush","Brush QR",ToolGroup.PROJECT),ToolCapability("export_video","Export Video",ToolGroup.PROJECT),ToolCapability("export_gif","Export GIF",ToolGroup.PROJECT),ToolCapability("export_png_sequence","PNG Sequence",ToolGroup.PROJECT)
    )
}
