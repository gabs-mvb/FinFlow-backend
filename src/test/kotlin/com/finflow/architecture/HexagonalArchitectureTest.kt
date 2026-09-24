package com.finflow.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class HexagonalArchitectureTest {
    @Test
    fun `domain and application dependencies point inward`() {
        val violations = mutableListOf<String>()
        Files.walk(Path.of("core/src/main/kotlin")).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }.forEach { file ->
                val text = Files.readString(file)
                val domain = file.toString().replace('\\', '/').contains("/domain/")
                Regex("(?m)^import (.+)").findAll(text).forEach {
                    val dependency = it.groupValues[1]
                    val allowed =
                        dependency.startsWith("java.") || dependency.startsWith("kotlin.") ||
                            (
                                dependency.startsWith("com.finflow.") &&
                                    !dependency.contains(".adapter.") && !dependency.contains(".composition.") &&
                                    (!domain || dependency.contains(".domain."))
                            )
                    if (!allowed) violations += "$file -> $dependency"
                }
            }
        }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun `http controllers depend on input ports and not persistence or service implementations`() {
        val violations = mutableListOf<String>()
        Files.walk(Path.of("src/main/kotlin")).use { paths ->
            paths.filter { it.fileName.toString().endsWith("Controller.kt") }.forEach { file ->
                Regex("(?m)^import (.+)").findAll(Files.readString(file)).forEach {
                    val dependency = it.groupValues[1]
                    if (dependency.contains(".outbound.") ||
                        Regex("\\.application\\.[A-Z]\\w*Service$").containsMatchIn(dependency)
                    ) {
                        violations += "$file -> $dependency"
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }
}
