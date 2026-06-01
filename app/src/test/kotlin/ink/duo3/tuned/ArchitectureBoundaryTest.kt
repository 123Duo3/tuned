package ink.duo3.tuned

import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Encodes the four boundary rules from CLAUDE.md. Rules pass vacuously until the
 * relevant packages exist, then enforce on every build.
 */
class ArchitectureBoundaryTest {
    private val files = Konsist.scopeFromProject().files

    /**
     * A "page" is the presentation pair for one screen: its `feature.<name>`
     * (ViewModel + UiState) and its `ui.<name>` (Compose screen). The shared UI
     * packages `ui.designsystem` and `ui.theme` are not pages — they are reusable
     * across pages — so they return null here.
     */
    private fun pageOf(packageName: String?): String? {
        val leaf =
            when {
                packageName == null -> null
                packageName.startsWith(FEATURE_PREFIX) ->
                    packageName.removePrefix(FEATURE_PREFIX).substringBefore(".")

                packageName.startsWith(UI_PREFIX) ->
                    packageName.removePrefix(UI_PREFIX).substringBefore(".")

                else -> null
            }
        return if (leaf == null || leaf in SHARED_UI) null else leaf
    }

    @Test
    fun `pages do not import each other`() {
        val violations =
            files.flatMap { file ->
                val page = pageOf(file.packagee?.name) ?: return@flatMap emptyList()
                file.imports
                    .map { it.name }
                    .mapNotNull { pageOf(it) }
                    .filter { it != page }
                    .map { "${file.path}: page '$page' imports page '$it'" }
            }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    /**
     * Whitelist: a page's *project-internal* imports may only reach domain, core,
     * navigation, the shared UI packages (ui.designsystem / ui.theme), or its own
     * page (its matching feature/ui subpackage). Anything else internal (data, di,
     * player.media3, another page) is a violation. External imports (AndroidX,
     * lifecycle, Coil, etc.) are unconstrained.
     *
     * The generated `R` class lives in the root package and is how any UI reaches
     * string/drawable resources, so it is allowed everywhere.
     */
    @Test
    fun `pages only import domain core navigation and shared ui internally`() {
        val allowedPrefixes =
            listOf(
                "ink.duo3.tuned.domain",
                "ink.duo3.tuned.core",
                "ink.duo3.tuned.navigation",
                "ink.duo3.tuned.ui.designsystem",
                "ink.duo3.tuned.ui.theme",
            )
        val allowedExact = setOf("ink.duo3.tuned.R")
        val violations =
            files.flatMap { file ->
                val page = pageOf(file.packagee?.name) ?: return@flatMap emptyList()
                file.imports
                    .map { it.name }
                    .filter { it.startsWith("ink.duo3.tuned.") }
                    .filterNot { imp ->
                        imp in allowedExact ||
                            allowedPrefixes.any { imp.startsWith(it) } ||
                            pageOf(imp) == page
                    }.map { "${file.path}: page '$page' imports disallowed '$it'" }
            }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `media3 is only imported in player media3`() {
        val violations =
            files
                .filter { file -> file.imports.any { it.name.startsWith("androidx.media3") } }
                .filterNot { file ->
                    file.packagee?.name?.startsWith("ink.duo3.tuned.player.media3") == true
                }.map { "${it.path}: imports androidx.media3 outside player/media3" }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    /**
     * Rule 4: concrete classes in data/repository must implement an interface
     * declared in domain/repository — UI depends on the interface, never the impl.
     */
    @Test
    fun `data repository impls implement a domain repository interface`() {
        val domainRepositoryInterfaces =
            Konsist
                .scopeFromProject()
                .interfaces()
                .filter { it.resideInPackage("ink.duo3.tuned.domain.repository..") }
                .map { it.name }
                .toSet()

        val violations =
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("ink.duo3.tuned.data.repository..") }
                .filterNot { it.hasAbstractModifier }
                .filter { clazz -> clazz.parents().none { it.name in domainRepositoryInterfaces } }
                .map { "${it.path}: ${it.name} does not implement a domain.repository interface" }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    private companion object {
        const val FEATURE_PREFIX = "ink.duo3.tuned.feature."
        const val UI_PREFIX = "ink.duo3.tuned.ui."
        val SHARED_UI = setOf("designsystem", "theme")
    }
}
