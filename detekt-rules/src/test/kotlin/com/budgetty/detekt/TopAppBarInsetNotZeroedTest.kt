package com.budgetty.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class TopAppBarInsetNotZeroedTest {

    private fun findings(code: String) =
        TopAppBarInsetNotZeroed(TestConfig("active" to "true")).lint(code)

    @Test
    fun `flags a TopAppBar with no windowInsets`() {
        val code = """
            fun s() {
                Scaffold(topBar = {
                    TopAppBar(title = { Text("Hi") })
                }) {}
            }
        """.trimIndent()
        assertEquals(1, findings(code).size)
    }

    @Test
    fun `flags a TopAppBar that keeps the default (non-zero) insets`() {
        val code = """
            fun s() {
                TopAppBar(
                    title = { Text("Hi") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                )
            }
        """.trimIndent()
        assertEquals(1, findings(code).size)
    }

    @Test
    fun `flags a CenterAlignedTopAppBar with no windowInsets`() {
        val code = """
            fun s() {
                CenterAlignedTopAppBar(title = { Text("Hi") })
            }
        """.trimIndent()
        assertEquals(1, findings(code).size)
    }

    @Test
    fun `passes a TopAppBar with zeroed window insets`() {
        val code = """
            fun s() {
                TopAppBar(
                    title = { Text("Hi") },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }

    @Test
    fun `passes a zeroed TopAppBar written without spaces`() {
        val code = """
            fun s() {
                TopAppBar(title = { Text("Hi") }, windowInsets = WindowInsets(0,0,0,0))
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }

    @Test
    fun `ignores a TopAppBar hosted inside a full-bleed Dialog`() {
        val code = """
            fun s() {
                Dialog(onDismissRequest = {}) {
                    Scaffold(topBar = {
                        TopAppBar(title = { Text("Hi") })
                    }) {}
                }
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }

    @Test
    fun `ignores a non app bar composable`() {
        val code = """
            fun s() {
                Surface { Text("Hi") }
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }
}
