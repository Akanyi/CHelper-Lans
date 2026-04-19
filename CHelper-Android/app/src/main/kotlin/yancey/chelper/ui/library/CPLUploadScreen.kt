package yancey.chelper.ui.library

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import yancey.chelper.R
import yancey.chelper.data.LocalCommandLabDataStore
import yancey.chelper.network.library.data.LibraryFunction
import yancey.chelper.ui.common.CHelperTheme
import yancey.chelper.ui.common.dialog.CaptchaDialog
import yancey.chelper.ui.common.dialog.CustomDialog
import yancey.chelper.ui.common.dialog.CustomDialogProperties
import yancey.chelper.ui.common.dialog.DialogContainer
import yancey.chelper.ui.common.layout.RootViewWithHeaderAndCopyright
import yancey.chelper.ui.common.widget.Button
import yancey.chelper.ui.common.widget.Icon
import yancey.chelper.ui.common.widget.Switch
import yancey.chelper.ui.common.widget.Text
import yancey.chelper.ui.common.widget.TextField
import yancey.chelper.ui.library.mcd.LineType
import yancey.chelper.ui.library.mcd.MCDContentView
import yancey.chelper.ui.library.mcd.MCDValidationResult
import yancey.chelper.ui.library.mcd.validateMCDContent

@SuppressLint("UseKtx")
@Composable
fun CPLUploadScreen(
    viewModel: CPLUploadViewModel = viewModel(),
    navController: NavHostController,
    editLibraryId: Int = -1,
    editLibraryJson: String? = null
) {
    LaunchedEffect(editLibraryId, editLibraryJson) {
        if (editLibraryId > 0 && viewModel.editId == -1) {
            viewModel.loadFromCloudJson(editLibraryJson, editLibraryId)
        }
    }
    val context = LocalContext.current
    val localCommandLabDataStore = remember(context) { LocalCommandLabDataStore(context) }
    val libraries by localCommandLabDataStore.localLibraryFunctions()
        .collectAsState(initial = emptyList())

    var showImportDialog by remember { mutableStateOf(false) }
    var showCaptchaDialog by remember { mutableStateOf(false) }
    var showPreviewScreen by remember { mutableStateOf(false) }
    var captchaCallback by remember { mutableStateOf<(String) -> Unit>({}) }
    var validationResult by remember { mutableStateOf<MCDValidationResult?>(null) }

    if (showCaptchaDialog) {
        CaptchaDialog(
            action = "publish",
            onDismissRequest = { showCaptchaDialog = false },
            onSuccess = { code -> captchaCallback(code) }
        )
    }

    // ━━━ 预览界面（覆盖式，全屏） ━━━
    if (showPreviewScreen && validationResult != null) {
        MCDPreviewScreen(
            fullMCDContent = viewModel.buildFullMCD(),
            validationResult = validationResult!!,
            onBack = { showPreviewScreen = false },
            onFixAmbiguous = {
                val originalLines = viewModel.commands.text.toString().split(Regex("\\r?\\n"))
                val ambiguousLineNumbers = validationResult!!.lines
                    .filter { it.type == LineType.AMBIGUOUS }
                    .map { it.lineNumber }
                    .toSet()

                val fixedLines = originalLines.mapIndexed { idx, line ->
                    if ((idx + 1) in ambiguousLineNumbers && line.trimStart().let { t ->
                            t.isNotEmpty() && !t.startsWith("#")
                        }) {
                        "#$line"
                    } else {
                        line
                    }
                }
                val fixedContent = fixedLines.joinToString("\n")
                viewModel.commands.setTextAndPlaceCursorAtEnd(fixedContent)
                validationResult = validateMCDContent(fixedContent)
            },
            onConfirmUpload = {
                showPreviewScreen = false
                viewModel.upload(null) {
                    navController.popBackStack()
                }
            }
        )
        return
    }

    // ━━━ 编辑界面 ━━━
    RootViewWithHeaderAndCopyright(title = stringResource(R.string.upload_title)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ================= 基础信息卡片 =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CHelperTheme.colors.backgroundComponent)
                    .padding(16.dp)
            ) {
                Text(
                    text = "基础信息",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CHelperTheme.colors.textMain
                    )
                )
                Spacer(Modifier.height(14.dp))
                TextField(
                    state = viewModel.name,
                    hint = stringResource(R.string.upload_field_name),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    state = viewModel.description,
                    hint = stringResource(R.string.upload_field_description),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        state = viewModel.version,
                        hint = stringResource(R.string.upload_field_version),
                        modifier = Modifier.weight(1f)
                    )
                    TextField(
                        state = viewModel.tags,
                        hint = stringResource(R.string.upload_field_tags),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= 脚本内容卡片 =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CHelperTheme.colors.backgroundComponent)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "执行脚本",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CHelperTheme.colors.textMain
                        )
                    )
                    
                    // 右侧操作区：可选的 V2 辅助补全 + 本地导入
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (viewModel.useV2) {
                            var showLowCodeHelper by remember { mutableStateOf(false) }

                            if (showLowCodeHelper) {
                                LowCodeV2HelperDialog(
                                    rawContent = viewModel.commands.text.toString(),
                                    onDismiss = { showLowCodeHelper = false },
                                    onApply = { newContent ->
                                        viewModel.commands.setTextAndPlaceCursorAtEnd(newContent)
                                        showLowCodeHelper = false
                                        com.hjq.toast.Toaster.show("已应用标记！")
                                    }
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE65100).copy(alpha = 0.1f))
                                    .clickable { showLowCodeHelper = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    id = R.drawable.pencil,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "低代码补全 V2",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE65100)
                                    )
                                )
                            }
                        }

                        // 将原本突兀的大按钮变成了轻量级的文字行动点
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CHelperTheme.colors.mainColor.copy(alpha = 0.1f))
                                .clickable { showImportDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                id = R.drawable.folder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.upload_import_local),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CHelperTheme.colors.mainColor
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                TextField(
                    state = viewModel.commands,
                    hint = stringResource(if (viewModel.useV2) R.string.upload_field_commands_v2 else R.string.upload_field_commands_v1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.TopStart
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 底部辅助选项区：语法说明 & V2 开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    "https://abyssous.site/wiki".toUri()
                                )
                                context.startActivity(intent)
                            }
                            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            id = R.drawable.book,
                            contentDescription = stringResource(R.string.upload_wiki_link),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.upload_wiki_link),
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = CHelperTheme.colors.mainColor,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "启用 V2 语法",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = CHelperTheme.colors.textSecondary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = viewModel.useV2,
                            onCheckedChange = { viewModel.useV2 = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val emptyErrorText = stringResource(R.string.upload_empty_error)
            Button(
                text = stringResource(if (viewModel.isLoading) R.string.upload_btn_uploading else R.string.upload_btn_preview),
                onClick = {
                    if (!viewModel.isLoading) {
                        val content = viewModel.commands.text.toString()
                        if (viewModel.name.text.isBlank() || content.isBlank()) {
                            com.hjq.toast.Toaster.show(emptyErrorText)
                            return@Button
                        }
                        validationResult = validateMCDContent(content)
                        showPreviewScreen = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (showImportDialog) {
        ImportLocalLibraryDialog(
            libraries = libraries,
            onDismiss = { showImportDialog = false },
            onSelect = { id ->
                viewModel.loadFromLocal(libraries[id])
                showImportDialog = false
            }
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 预览界面
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun MCDPreviewScreen(
    fullMCDContent: String,
    validationResult: MCDValidationResult,
    onBack: () -> Unit,
    onFixAmbiguous: () -> Unit,
    onConfirmUpload: () -> Unit
) {
    var showQuickHelp by remember { mutableStateOf(false) }

    // 系统返回键 → 返回编辑
    BackHandler { onBack() }

    RootViewWithHeaderAndCopyright(title = stringResource(R.string.upload_preview_title)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ━━━ 状态栏 ━━━
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (validationResult.hasErrors) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            id = R.drawable.alert_triangle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.upload_validation_error,
                                validationResult.errorCount
                            ),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD32F2F)
                            )
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            id = R.drawable.check_circle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.upload_validation_ok,
                                validationResult.lines.size
                            ),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showQuickHelp = !showQuickHelp }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        id = R.drawable.help_circle,
                        contentDescription = stringResource(R.string.upload_syntax_help),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.upload_syntax_help),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = CHelperTheme.colors.mainColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        id = if (showQuickHelp) R.drawable.chevron_up else R.drawable.chevron_down,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // ━━━ 快速帮助（折叠） ━━━
            if (showQuickHelp) {
                QuickSyntaxHelp(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp)
                )
            }

            // ━━━ 错误行汇总 + 一键修复 ━━━
            if (validationResult.hasErrors) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x15D32F2F))
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.upload_error_summary),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F)
                        )
                    )
                    Spacer(Modifier.height(6.dp))

                    validationResult.lines.filter { it.type == LineType.AMBIGUOUS }
                        .forEach { line ->
                            Row(
                                modifier = Modifier.padding(vertical = 1.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "L${line.lineNumber}",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFD32F2F)
                                    ),
                                    modifier = Modifier.width(32.dp)
                                )
                                Text(
                                    text = line.rawText,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFD32F2F)
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.upload_fix_ambiguous),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CHelperTheme.colors.mainColor)
                            .clickable { onFixAmbiguous() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ━━━ 可视化渲染预览（含元数据） ━━━
            MCDContentView(
                content = fullMCDContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ━━━ 底部操作栏 ━━━
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CHelperTheme.colors.backgroundComponent)
                        .clickable { onBack() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            id = R.drawable.chevron_left,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.upload_btn_back),
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = CHelperTheme.colors.textMain
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (validationResult.hasErrors) Color(0xFFBDBDBD)
                            else CHelperTheme.colors.mainColor
                        )
                        .then(
                            if (!validationResult.hasErrors) {
                                Modifier.clickable { onConfirmUpload() }
                            } else Modifier
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                if (validationResult.hasErrors) R.string.upload_btn_fix_required
                                else R.string.upload_btn_confirm
                            ),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                        if (!validationResult.hasErrors) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                id = R.drawable.chevron_right,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 快速语法帮助（仅函数体内的语法）
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun QuickSyntaxHelp(modifier: Modifier = Modifier) {
    val helpContext = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CHelperTheme.colors.backgroundComponent)
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.upload_quick_help_title),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CHelperTheme.colors.textMain
            )
        )
        Spacer(Modifier.height(6.dp))

        val items = listOf(
            "execute ..." to "指令行（英文字母或 / 开头）",
            "#注释文字" to "注释行（不执行，仅说明）",
            "---链名---" to "命令链分隔符（v2）",
            "> C" to "连锁方块状态（v2）",
            "> I" to "脉冲方块状态（v2）",
            "> R" to "循环方块状态（v2）",
            "> C?" to "加 ? = 条件方块",
            "> C!" to "加 ! = 红石驱动",
            "> Ct5" to "加 t+数字 = 延迟 Tick"
        )

        items.forEach { (syntax, desc) ->
            Row(
                modifier = Modifier.padding(vertical = 1.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = syntax,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CHelperTheme.colors.mainColor,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.width(100.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = desc,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = CHelperTheme.colors.textSecondary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 完整 Wiki 链接
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        "https://abyssous.site/wiki".toUri()
                    )
                    helpContext.startActivity(intent)
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                id = R.drawable.external_link,
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.upload_wiki_full_link),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = CHelperTheme.colors.mainColor,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 本地导入选择对话框
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ImportLocalLibraryDialog(
    libraries: List<LibraryFunction>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    CustomDialog(
        onDismissRequest = onDismiss,
        properties = CustomDialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogContainer(backgroundNoTranslate = true) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.upload_import_dialog_title),
                    style = TextStyle(
                        fontSize = 20.sp,
                        color = CHelperTheme.colors.textMain,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    libraries.forEachIndexed { index, lib ->
                        Text(
                            text = lib.name ?: stringResource(R.string.upload_import_unnamed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(index) }
                                .padding(vertical = 12.dp),
                            style = TextStyle(color = CHelperTheme.colors.textMain)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CHelperTheme.colors.line)
                        )
                    }
                    if (libraries.isEmpty()) {
                        Text(
                            text = stringResource(R.string.upload_import_empty),
                            style = TextStyle(color = CHelperTheme.colors.textHint)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(8.dp),
                        style = TextStyle(color = CHelperTheme.colors.mainColor)
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 低代码 V2 状态标记辅助工具对话框
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun LowCodeV2HelperDialog(
    rawContent: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    // 提取出所有需要标记的行
    val lines = rawContent.split(Regex("\\r?\\n"))
    val results = yancey.chelper.ui.library.mcd.validateMCDContent(rawContent).lines.associateBy { it.lineNumber }

    // 状态记录: map of lineNumber -> stateString (例如 "> C", "> I!")
    val lineStates = remember { mutableStateOf(mutableMapOf<Int, String>()) }

    var lastEffectiveType: LineType? = null
    val targetLines = mutableListOf<Pair<Int, String>>() // <LineNumber, RawText>

    for ((idx, line) in lines.withIndex()) {
        val lineNum = idx + 1
        val result = results[lineNum]
        if (result == null) continue

        if (result.type == LineType.COMMAND) {
            if (lastEffectiveType != LineType.STATE_LINE) {
                targetLines.add(lineNum to line)
                if (!lineStates.value.containsKey(lineNum)) {
                    lineStates.value[lineNum] = "> C" // 默认连锁
                }
            }
            lastEffectiveType = LineType.COMMAND
        } else {
            if (result.type != LineType.COMMENT && result.type != LineType.META) {
                lastEffectiveType = result.type
            }
        }
    }

    CustomDialog(
        onDismissRequest = onDismiss,
        properties = CustomDialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        DialogContainer(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(600.dp),
            backgroundNoTranslate = true
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "V2 标记助手",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CHelperTheme.colors.textMain
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "以下指令缺少方块状态，请为它们分配：",
                    style = TextStyle(fontSize = 13.sp, color = CHelperTheme.colors.textSecondary)
                )
                Spacer(Modifier.height(12.dp))

                if (targetLines.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("当前脚本中所有指令都已有正确的前置方块推断状态，无需再标记。", style = TextStyle(color = CHelperTheme.colors.textHint, textAlign = TextAlign.Center))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        for ((lineNum, text) in targetLines) {
                            val currentState = lineStates.value[lineNum] ?: "> C"
                            LowCodeLineItem(
                                lineNumber = lineNum,
                                code = text,
                                currentState = currentState,
                                onStateChange = { newState ->
                                    val newMap = lineStates.value.toMutableMap()
                                    newMap[lineNum] = newState
                                    lineStates.value = newMap
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "取消",
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = TextStyle(color = CHelperTheme.colors.textSecondary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (targetLines.isEmpty()) Color(0xFFBDBDBD) else Color(0xFFE65100))
                            .then(
                                if (targetLines.isNotEmpty()) Modifier.clickable {
                                    val output = mutableListOf<String>()
                                    var lastType: LineType? = null
                                    for ((i, l) in lines.withIndex()) {
                                        val ln = i + 1
                                        val res = results[ln]
                                        if (res?.type == LineType.COMMAND) {
                                            if (lastType != LineType.STATE_LINE) {
                                                output.add(lineStates.value[ln] ?: "> C")
                                            }
                                            lastType = LineType.COMMAND
                                        } else if (res != null) {
                                            if (res.type != LineType.COMMENT && res.type != LineType.META) {
                                                lastType = res.type
                                            }
                                        }
                                        output.add(l)
                                    }
                                    onApply(output.joinToString("\n"))
                                } else Modifier
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "一键应用",
                            style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LowCodeLineItem(
    lineNumber: Int,
    code: String,
    currentState: String,
    onStateChange: (String) -> Unit
) {
    // 解析现有状态（忽略所有 _ 占位符）
    val cleanState = currentState.replace("_", "").trim()
    val isC = Regex("^>\\s*c", RegexOption.IGNORE_CASE).containsMatchIn(cleanState)
    val isI = Regex("^>\\s*i", RegexOption.IGNORE_CASE).containsMatchIn(cleanState)
    val isR = Regex("^>\\s*r", RegexOption.IGNORE_CASE).containsMatchIn(cleanState)
    val isH = Regex("^>\\s*h\\s*$", RegexOption.IGNORE_CASE).matches(cleanState)
    val isCond = cleanState.contains("?")
    val isRed = cleanState.contains("!")
    val delayMatch = Regex("t(\\d+)").find(cleanState)
    val delay = delayMatch?.groupValues?.get(1) ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CHelperTheme.colors.backgroundComponent)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "L$lineNumber",
                style = TextStyle(fontSize = 11.sp, color = Color(0xFFE65100), fontFamily = FontFamily.Monospace),
                modifier = Modifier.width(32.dp)
            )
            Text(
                code,
                style = TextStyle(fontSize = 12.sp, color = CHelperTheme.colors.textMain, fontFamily = FontFamily.Monospace),
                maxLines = 1
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LowCodeChip("连锁(C)", isC) { onStateChange(buildState(">C", isCond, isRed, delay)) }
            LowCodeChip("脉冲(I)", isI) { onStateChange(buildState(">I", isCond, isRed, delay)) }
            LowCodeChip("循环(R)", isR) { onStateChange(buildState(">R", isCond, isRed, delay)) }
            LowCodeChip("手动(H)", isH) { onStateChange(">H") }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (isH) 0.5f else 1f)) {
                Switch(
                    checked = isCond,
                    onCheckedChange = { if (!isH) onStateChange(buildState(if (isC) "> C" else if (isI) "> I" else "> R", it, isRed, delay)) }
                )
                Spacer(Modifier.width(6.dp))
                Text("条件?", style = TextStyle(fontSize = 11.sp, color = CHelperTheme.colors.textSecondary))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (isH) 0.5f else 1f)) {
                Switch(
                    checked = isRed,
                    onCheckedChange = { if (!isH) onStateChange(buildState(if (isC) "> C" else if (isI) "> I" else "> R", isCond, it, delay)) }
                )
                Spacer(Modifier.width(6.dp))
                Text("红石!", style = TextStyle(fontSize = 11.sp, color = CHelperTheme.colors.textSecondary))
            }
        }
    }
}

private fun buildState(base: String, cond: Boolean, red: Boolean, delay: String): String {
    var s = base
    if (cond) s += "?"
    if (red) s += "!"
    if (delay.isNotEmpty()) s += "t$delay"
    return s
}

@Composable
private fun LowCodeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Color(0xFFE65100) else CHelperTheme.colors.backgroundComponentNoTranslate)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 11.sp,
                color = if (selected) Color.White else CHelperTheme.colors.textMain,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}
