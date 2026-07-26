/**
 * It is part of CHelper. CHelper is a command helper for Minecraft Bedrock Edition.
 * Copyright (C) 2026  Yancey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package yancey.chelper.ui.library

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import yancey.chelper.data.LocalLibraryEditDraft
import yancey.chelper.network.library.data.LibraryFunction
import yancey.chelper.ui.library.mcd.LineType
import yancey.chelper.ui.library.mcd.validateMCDContent

enum class EditMode {
    ADD,
    UPDATE
}

data class LocalLibraryEditorSnapshot(
    val name: String,
    val version: String,
    val description: String,
    val tags: String,
    val commands: String,
    val autoSync: Boolean,
    val useV2: Boolean
)

data class LocalLibraryTemplate(
    val label: String,
    val content: String,
    val requiresV2: Boolean = false
)

class LocalLibraryEditViewModel : ViewModel() {
    var id by mutableStateOf<Int?>(null)
    var localEntryId by mutableStateOf<String?>(null)
    val mode get() = if (localEntryId == null && id == null) EditMode.ADD else EditMode.UPDATE
    var name by mutableStateOf(TextFieldState())
    var version by mutableStateOf(TextFieldState())
    var description by mutableStateOf(TextFieldState())
    var tags by mutableStateOf(TextFieldState())
    var commands by mutableStateOf(TextFieldState())

    /**
     * 保存时是否顺手把本条本地库同步到云端。
     * 以前叫"自动同步"——但当时要求"本地库必须先有 uuid"，
     * 用户根本不知道 uuid 哪里来，相当于这个开关只对已经手动上传过的库生效。
     *
     * 现在改成"自动生成 UUID 并同步"：
     * - 本地库还没绑定云端 id：保存时调 upload，让后端分配 uuid，
     *   响应里的 uuid + id 回写到本地，下次起就是普通的"已绑定"状态。
     * - 已经绑过云端：保存时调 update。
     * 用户不再需要关心 uuid 从哪来。
     */
    var autoSync by mutableStateOf(false)

    /**
     * 是否使用 MCD V2 语法。
     * - 新建本地库（ADD）默认开启：V2 才支持命令链 / 方块状态等完整可视化，新内容没必要再绑老语法。
     * - 编辑已有库（UPDATE）根据 content 里是否带 `@mcd_version=2` 头自动推断，
     *   避免误把存量 V1 库标记成 V2 反而渲染异常。
     * - 本地通过 localIsV2 单独持久化，云端同步时再生成 `@mcd_version=2` 头。
     */
    var useV2 by mutableStateOf(true)
    var isShowDeleteDialog by mutableStateOf(false)
    var isShowLowCodeHelper by mutableStateOf(false)

    // V2 → V1 降级二次确认：切回去会丢命令链/方块状态可视化，得让用户先确认
    var isShowV2DowngradeConfirm by mutableStateOf(false)
    var isSyncing by mutableStateOf(false)
    var isShowExitConfirm by mutableStateOf(false)
    var isShowTemplateDialog by mutableStateOf(false)
    var isInitialized by mutableStateOf(false)
        private set
    var draftRestored by mutableStateOf(false)
        private set
    var draftWritesEnabled by mutableStateOf(true)
        private set
    var exitApproved by mutableStateOf(false)
        private set
    private var initialSnapshot: LocalLibraryEditorSnapshot? = null
    private var initializedEditKey: String? = null
    private var initializedAddMode = false

    fun ensureEditingTarget(localEntryId: String?, targetId: Int?, library: LibraryFunction?) {
        this.localEntryId = localEntryId ?: library?.localEntryId
        id = targetId
        if (localEntryId == null && targetId == null) {
            if (!initializedAddMode) {
                if (initializedEditKey != null) {
                    name.setTextAndPlaceCursorAtEnd("")
                    version.setTextAndPlaceCursorAtEnd("")
                    description.setTextAndPlaceCursorAtEnd("")
                    tags.setTextAndPlaceCursorAtEnd("")
                    commands.setTextAndPlaceCursorAtEnd("")
                    autoSync = false
                }
                // 新建模式：V2 是默认偏好
                useV2 = true
                initializedEditKey = null
                initializedAddMode = true
                initialSnapshot = snapshot()
                isInitialized = true
            }
            return
        }
        initializedAddMode = false
        val targetKey = localEntryId ?: "index:$targetId"
        if (initializedEditKey == targetKey || library == null) return

        name.setTextAndPlaceCursorAtEnd(library.name ?: "")
        version.setTextAndPlaceCursorAtEnd(library.version ?: "")
        description.setTextAndPlaceCursorAtEnd(library.note ?: "")
        tags.setTextAndPlaceCursorAtEnd(library.tags?.joinToString(separator = ",") ?: "")

        val cleanLines = library.localBody().lines().filter { line ->
            val t = line.trim()
            !(t.startsWith("@name=") || t.startsWith("@version=") ||
                    t.startsWith("@tags=") || t.startsWith("@note=") ||
                    t.startsWith("@mcd_version=") || t.startsWith("@uuid="))
        }
        commands.setTextAndPlaceCursorAtEnd(cleanLines.joinToString("\n").trim())
        autoSync = library.autoSync ?: false
        // 编辑存量库：从原始 content 推断 V2 标记。容忍 `@mcd_version= 2` 这种带空格的写法
        useV2 = library.usesLocalMcdV2()
        initializedEditKey = targetKey
        initialSnapshot = snapshot()
        isInitialized = true
    }

    val draftKey: String
        get() = localEntryId?.let { "local-library:update:$it" }
            ?: id?.let { "local-library:update:index:$it" }
            ?: "local-library:add"

    val isDirty: Boolean
        get() = initialSnapshot?.let { snapshot() != it } == true

    fun snapshot(): LocalLibraryEditorSnapshot = LocalLibraryEditorSnapshot(
        name = name.text.toString(),
        version = version.text.toString(),
        description = description.text.toString(),
        tags = tags.text.toString(),
        commands = commands.text.toString().replace("\r\n", "\n"),
        autoSync = autoSync,
        useV2 = useV2
    )

    fun restoreDraft(draft: LocalLibraryEditDraft?) {
        if (draftRestored) return
        draft?.let {
            name.setTextAndPlaceCursorAtEnd(it.name)
            version.setTextAndPlaceCursorAtEnd(it.version)
            description.setTextAndPlaceCursorAtEnd(it.description)
            tags.setTextAndPlaceCursorAtEnd(it.tags)
            commands.setTextAndPlaceCursorAtEnd(it.commands)
            autoSync = it.autoSync
            useV2 = it.useV2
        }
        draftRestored = true
    }

    fun toDraft(snapshot: LocalLibraryEditorSnapshot = snapshot()): LocalLibraryEditDraft =
        LocalLibraryEditDraft(
            name = snapshot.name,
            version = snapshot.version,
            description = snapshot.description,
            tags = snapshot.tags,
            commands = snapshot.commands,
            autoSync = snapshot.autoSync,
            useV2 = snapshot.useV2,
            updatedAt = System.currentTimeMillis()
        )

    fun markSaved() {
        draftWritesEnabled = false
        initialSnapshot = snapshot()
    }

    fun approveExit() {
        exitApproved = true
    }

    fun applyTemplate(template: LocalLibraryTemplate) {
        val current = commands.text.toString()
        val next = if (current.isBlank()) template.content else "$current\n\n${template.content}"
        commands.setTextAndPlaceCursorAtEnd(next)
    }

    fun availableTemplates(): List<LocalLibraryTemplate> = buildList {
        add(LocalLibraryTemplate("单条命令", "say hello"))
        add(LocalLibraryTemplate("注释 + 命令", "# 说明这条命令的用途\nsay hello"))
        if (useV2) {
            add(LocalLibraryTemplate("脉冲命令方块", "> I\nsay hello", requiresV2 = true))
            add(LocalLibraryTemplate("连锁命令链", "> I\nsay start\n> C!\nsay next", requiresV2 = true))
            add(LocalLibraryTemplate("循环执行", "> R!\nsay loop", requiresV2 = true))
            add(LocalLibraryTemplate("聊天文本", "> H\n这是一段聊天文本", requiresV2 = true))
        }
    }

    fun validationError(): String? {
        if (name.text.isBlank()) return "名称不能为空"
        if (commands.text.isBlank()) return "执行脚本不能为空"
        if (sequenceOf(name.text, version.text, description.text, tags.text).any { '\n' in it || '\r' in it }) {
            return "名称、版本、描述和标签不能包含换行"
        }
        val body = commands.text.toString()
        val forbidden = listOf("###Function###", "###End###", "@name=", "@version=", "@uuid=", "@mcd_version=")
        if (forbidden.any { body.contains(it, ignoreCase = true) }) {
            return "脚本区只填写命令正文，不要粘贴 MCD 头部或 Function 标记"
        }
        val validation = validateMCDContent(body)
        val firstError = validation.lines.firstOrNull { it.type == LineType.AMBIGUOUS }
        if (firstError != null) {
            return "第 ${firstError.lineNumber} 行无法识别，共 ${validation.errorCount} 行需要修正"
        }
        if (useV2) {
            val stateRegex = Regex(
                """^>\s*([ICRH_])?([?_])?([!_])?(?:t(\d+|_))?\s*$""",
                RegexOption.IGNORE_CASE
            )
            val invalidState = body.lines().withIndex().firstOrNull {
                it.value.trim().startsWith(">") && !stateRegex.matches(it.value.trim())
            }
            if (invalidState != null) return "第 ${invalidState.index + 1} 行不是合法的 V2 状态标记"
        }
        return null
    }

    /**
     * 把用户输入的 content（不带 MCD 头）拼成完整 MCD 文本：
     * 头部（@name/@version/@tags/@note/@mcd_version/@uuid）+ Function 段。
     * 给"自动同步"接口拼上传载荷用。
     *
     * 提供 fallbackUuid 是为了"自动生成 UUID 并同步"场景——本地库还没 uuid 时，
     * 先在 ViewModel 外层 `UUID.randomUUID()` 生成一个塞进来，确保头部一定有 uuid，
     * 避免后端按"新建"再分配一个，导致下次再保存找不到对应记录。
     */
    fun buildFullMCD(
        existingLibrary: LibraryFunction?,
        fallbackUuid: String?
    ): String {
        val tagList = tags.text.toString()
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val effectiveUuid = existingLibrary?.uuid?.takeIf { it.isNotEmpty() } ?: fallbackUuid
        val sb = StringBuilder()
        sb.append("@name=${name.text}\n")
        sb.append("@version=${version.text.toString().ifEmpty { "1.0.0" }}\n")
        if (tagList.isNotEmpty()) {
            sb.append("@tags=${tagList.joinToString(",")}\n")
        }
        sb.append("@note=${description.text}\n")
        if (useV2) {
            sb.append("@mcd_version=2\n")
        }
        if (!effectiveUuid.isNullOrEmpty()) {
            sb.append("@uuid=$effectiveUuid\n")
        }
        sb.append("\n")
        sb.append("###Function###\n")
        sb.append(commands.text.toString())
        sb.append("\n###End###")
        return sb.toString()
    }
}
