package com.budgetty.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class SheetScrollNotCappedTest {

    private fun findings(code: String) =
        SheetScrollNotCapped(TestConfig("active" to "true")).lint(code)

    @Test
    fun `flags an uncapped LazyColumn inside a sheet`() {
        val code = """
            fun s() {
                AdaptiveSheet(onDismiss = {}) {
                    LazyColumn(contentPadding = PaddingValues(0)) {
                        items(3) {}
                    }
                }
            }
        """.trimIndent()
        assertEquals(1, findings(code).size)
    }

    @Test
    fun `flags a LazyColumn with no modifier at all inside a sheet`() {
        val code = """
            fun s() {
                ModalBottomSheet(onDismissRequest = {}) {
                    LazyColumn { items(3) {} }
                }
            }
        """.trimIndent()
        assertEquals(1, findings(code).size)
    }

    @Test
    fun `flags an uncapped verticalScroll Column inside a sheet`() {
        val code = """
            fun s() {
                ModalBottomSheet(onDismissRequest = {}) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("a")
                    }
                }
            }
        """.trimIndent()
        assertEquals(1, findings(code).size)
    }

    @Test
    fun `passes a LazyColumn capped with weight fill = false`() {
        val code = """
            fun s() {
                AdaptiveSheet(onDismiss = {}) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(3) {}
                    }
                }
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }

    @Test
    fun `passes a verticalScroll Column capped with weight fill = false`() {
        val code = """
            fun s() {
                AdaptiveSheet(onDismiss = {}) {
                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        Text("a")
                    }
                }
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }

    @Test
    fun `ignores a LazyColumn that is not inside a sheet`() {
        val code = """
            fun s() {
                LazyColumn { items(3) {} }
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }

    @Test
    fun `ignores a LazyColumn inside a plain Dialog (not a bottom sheet)`() {
        val code = """
            fun s() {
                Dialog(onDismissRequest = {}) {
                    LazyColumn { items(3) {} }
                }
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }

    @Test
    fun `ignores a horizontally-scrolling LazyRow inside a sheet`() {
        val code = """
            fun s() {
                AdaptiveSheet(onDismiss = {}) {
                    LazyRow { items(3) {} }
                }
            }
        """.trimIndent()
        assertEquals(0, findings(code).size)
    }
}
