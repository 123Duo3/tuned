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

    private fun featureOf(packageName: String?): String? {
        val prefix = "ink.duo3.tuned.feature."
        if (packageName == null || !packageName.startsWith(prefix)) return null
        return packageName.removePrefix(prefix).substringBefore(".")
    }

    @Test
    fun `feature packages do not import each other`() {
        val violations =
            files.flatMap { file ->
                val feature = featureOf(file.packagee?.name) ?: return@flatMap emptyList()
                file.imports
                    .map { it.name }
                    .filter { it.startsWith("ink.duo3.tuned.feature.") }
                    .mapNotNull { featureOf(it) }
                    .filter { it != feature }
                    .map { "${file.path}: feature '$feature' imports feature '$it'" }
            }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    /**
     * Whitelist: a feature's *project-internal* imports may only reach domain,
     * core, navigation, or its own feature subpackage. Anything else internal
     * (data, di, player.media3, another feature) is a violation. External imports
     * (AndroidX, lifecycle, etc.) are unconstrained.
     */
    @Test
    fun `feature only imports domain core navigation internally`() {
        val allowedPrefixes =
            listOf(
                "ink.duo3.tuned.domain",
                "ink.duo3.tuned.core",
                "ink.duo3.tuned.navigation",
            )
        val violations =
            files.flatMap { file ->
                val feature = featureOf(file.packagee?.name) ?: return@flatMap emptyList()
                file.imports
                    .map { it.name }
                    .filter { it.startsWith("ink.duo3.tuned.") }
                    .filterNot { imp ->
                        allowedPrefixes.any { imp.startsWith(it) } || featureOf(imp) == feature
                    }.map { "${file.path}: feature '$feature' imports disallowed '$it'" }
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
                .scopeFromProject()
                .classes()
                .filter { it.resideInPackage("ink.duo3.tuned.data.repository..") }
                .filterNot { it.hasAbstractModifier }
                .filter { clazz -> clazz.parents().none { it.name in domainRepositoryInterfaces } }
                .map { "${it.path}: ${it.name} does not implement a domain.repository interface" }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }
}
