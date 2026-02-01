/**
 * It is part of CHelper. CHelper is a command helper for Minecraft Bedrock Edition.
 * Copyright (C) 2025  Yancey
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

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import yancey.chelper.R
import yancey.chelper.network.library.data.LibraryFunction
import yancey.chelper.ui.common.CHelperTheme
import yancey.chelper.ui.common.layout.RootViewWithHeaderAndCopyright
import yancey.chelper.ui.common.widget.Divider
import yancey.chelper.ui.common.widget.Icon
import yancey.chelper.ui.common.widget.Text

@Composable
fun PublicLibraryShowScreen(
    id: Int,
    viewModel: PublicLibraryShowViewModel = viewModel()
) {
    val clipboard = LocalClipboard.current
    
    LaunchedEffect(id) {
        viewModel.loadFunction(id)
    }
    
    RootViewWithHeaderAndCopyright(title = viewModel.library.name ?: "加载中") {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "加载中...",
                            style = TextStyle(color = CHelperTheme.colors.textSecondary)
                        )
                    }
                }
                viewModel.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = viewModel.errorMessage ?: "加载失败",
                                style = TextStyle(color = CHelperTheme.colors.textSecondary)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "点击重试",
                                modifier = Modifier.clickable { viewModel.loadFunction(id) },
                                style = TextStyle(color = CHelperTheme.colors.mainColor)
                            )
                        }
                    }
                }
                else -> {
                    val contents: List<Pair<Boolean, String>> = remember(viewModel.library) {
                        viewModel.library.content
                            ?.split("\n")
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?.map {
                                if (it.startsWith("#")) {
                                    true to it.substring(1).trim()
                                } else {
                                    false to it
                                }
                            }
                            ?.filter { it.second.isNotEmpty() }
                            ?: listOf()
                    }
                    
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        // 元信息
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color = CHelperTheme.colors.backgroundComponent)
                                .padding(15.dp)
                        ) {
                            viewModel.library.author?.let { author ->
                                Text(
                                    text = "作者: $author",
                                    style = TextStyle(
                                        color = CHelperTheme.colors.textSecondary,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            Row {
                                viewModel.library.like_count?.let { likes ->
                                    Text(
                                        text = "♥ $likes",
                                        style = TextStyle(
                                            color = CHelperTheme.colors.mainColor,
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                            }
                            viewModel.library.note?.takeIf { it.isNotBlank() }?.let { note ->
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = note,
                                    style = TextStyle(
                                        color = CHelperTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(10.dp))
                        
                        // 内容列表
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 15.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color = CHelperTheme.colors.backgroundComponent)
                        ) {
                            items(contents) { content ->
                                Row(
                                    modifier = Modifier
                                        .padding(20.dp, 10.dp)
                                        .defaultMinSize(minHeight = 24.dp)
                                ) {
                                    Text(
                                        text = content.second,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.CenterVertically)
                                            .weight(1f),
                                        style = TextStyle(
                                            color = if (content.first) CHelperTheme.colors.mainColor else CHelperTheme.colors.textMain,
                                        )
                                    )
                                    if (content.first) {
                                        val scope = rememberCoroutineScope()
                                        Icon(
                                            id = R.drawable.copy,
                                            contentDescription = stringResource(R.string.common_icon_copy_content_description),
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .clickable {
                                                    scope.launch {
                                                        clipboard.setClipEntry(
                                                            ClipEntry(
                                                                ClipData.newPlainText(
                                                                    null,
                                                                    content.second
                                                                )
                                                            )
                                                        )
                                                    }
                                                }
                                                .padding(start = 5.dp)
                                                .size(24.dp)
                                        )
                                    }
                                }
                                Divider(padding = 0.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PublicLibraryShowScreenLightThemePreview() {
    val viewModel: PublicLibraryShowViewModel = viewModel()
    viewModel.library = LibraryFunction().apply {
        name = "Test Library"
        author = "Test Author"
        note = "This is a test description"
        like_count = 42
        content = buildString {
            for (i in 1..10) {
                append("# Command${i * 2 - 1}\n")
                append("Description${i * 2}\n")
            }
        }
    }
    CHelperTheme(theme = CHelperTheme.Theme.Light, backgroundBitmap = null) {
        PublicLibraryShowScreen(id = 1, viewModel = viewModel)
    }
}

@Preview
@Composable
fun PublicLibraryShowScreenDarkThemePreview() {
    val viewModel: PublicLibraryShowViewModel = viewModel()
    viewModel.library = LibraryFunction().apply {
        name = "Test Library"
        author = "Test Author"
        note = "This is a test description"
        like_count = 42
        content = buildString {
            for (i in 1..10) {
                append("# Command${i * 2 - 1}\n")
                append("Description${i * 2}\n")
            }
        }
    }
    CHelperTheme(theme = CHelperTheme.Theme.Dark, backgroundBitmap = null) {
        PublicLibraryShowScreen(id = 1, viewModel = viewModel)
    }
}
