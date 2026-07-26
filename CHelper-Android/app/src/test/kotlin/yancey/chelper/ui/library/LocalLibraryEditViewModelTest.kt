package yancey.chelper.ui.library

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import yancey.chelper.data.LocalLibraryEditDraft
import yancey.chelper.network.library.data.LibraryFunction

class LocalLibraryEditViewModelTest {
    @Test
    fun `新建模式初始干净且恢复草稿后变脏`() {
        val viewModel = LocalLibraryEditViewModel()
        viewModel.ensureEditingTarget(null, null, null)

        assertFalse(viewModel.isDirty)
        assertEquals("local-library:add", viewModel.draftKey)

        viewModel.restoreDraft(LocalLibraryEditDraft(name = "草稿", commands = "say hi"))

        assertTrue(viewModel.isDirty)
        assertEquals("草稿", viewModel.name.text.toString())
    }

    @Test
    fun `编辑模式使用稳定本地 ID 作为草稿键`() {
        val viewModel = LocalLibraryEditViewModel()
        val library = LibraryFunction(
            localEntryId = "stable",
            name = "库",
            content = "say hello",
            localIsV2 = false
        )

        viewModel.ensureEditingTarget("stable", null, library)

        assertEquals("local-library:update:stable", viewModel.draftKey)
        assertFalse(viewModel.isDirty)
    }

    @Test
    fun `模板追加而不覆盖已有内容`() {
        val viewModel = LocalLibraryEditViewModel()
        viewModel.ensureEditingTarget(null, null, null)
        viewModel.commands.setTextAndPlaceCursorAtEnd("say first")

        viewModel.applyTemplate(LocalLibraryTemplate("模板", "say second"))

        assertEquals("say first\n\nsay second", viewModel.commands.text.toString())
    }

    @Test
    fun `校验拦截完整套壳和非法状态行`() {
        val wrapped = LocalLibraryEditViewModel().apply {
            ensureEditingTarget(null, null, null)
            name.setTextAndPlaceCursorAtEnd("库")
            commands.setTextAndPlaceCursorAtEnd("###Function###\nsay hi\n###End###")
        }
        assertTrue(wrapped.validationError()!!.contains("Function"))

        val invalidState = LocalLibraryEditViewModel().apply {
            ensureEditingTarget(null, null, null)
            name.setTextAndPlaceCursorAtEnd("库")
            commands.setTextAndPlaceCursorAtEnd("> BAD\nsay hi")
        }
        assertTrue(invalidState.validationError()!!.contains("V2 状态"))
    }

    @Test
    fun `合法 V2 和聊天文本通过校验`() {
        val viewModel = LocalLibraryEditViewModel().apply {
            ensureEditingTarget(null, null, null)
            name.setTextAndPlaceCursorAtEnd("库")
            commands.setTextAndPlaceCursorAtEnd("> H\n中文聊天\n> C?!t5\nsay hi")
        }

        assertNull(viewModel.validationError())
    }
}
