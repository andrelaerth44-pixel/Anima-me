package com.animame

/**
 * Compatibility bootstrap for optional professional-tool extensions.
 *
 * The core editor owns line, shape, lasso, transform and brush routing directly.
 * Optional overlays can be added here later without coupling the build to them.
 */
object ProfessionalToolsBootstrap {
    fun install(context: android.content.Context) = Unit
}
