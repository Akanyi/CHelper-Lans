package yancey.chelper.android.common.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yancey.chelper.network.ServiceManager
import yancey.chelper.network.library.data.CaptchaStatusResponse
import yancey.chelper.network.library.data.CaptchaTokenRequest
import yancey.chelper.ui.common.CHelperTheme
import java.util.UUID

/**
 * 人机验证结果
 */
sealed class CaptchaResult {
    data class Success(val specialCode: String) : CaptchaResult()
    data class Failure(val message: String) : CaptchaResult()
    object Cancelled : CaptchaResult()
}

/**
 * 人机验证 BottomSheet
 * 
 * 模拟底部弹窗效果，包含 WebView 用于显示验证页面。
 * 
 * @param onDismissRequest 关闭回调
 * @param action 验证动作（如“注册账号”）
 * @param onResult 结果回调
 */
    var isVisible by remember { mutableStateOf(false) }
    var pendingResult by remember { mutableStateOf<CaptchaResult?>(null) }
    
    // 启动入场动画
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // 监听退出动画，动画结束后触发回调
    LaunchedEffect(isVisible) {
        if (!isVisible && pendingResult != null) {
            delay(300) // 等待动画结束
            // 触发外部回调，这通常会导致 CaptchaBottomSheet 从 Composition 中移除
            if (pendingResult is CaptchaResult.Cancelled) {
                onDismissRequest()
            } else {
                onResult(pendingResult!!)
            }
        }
    }

    // 处理关闭逻辑（带离场动画）
    fun dismiss(result: CaptchaResult) {
        pendingResult = result
        isVisible = false
    }

    Dialog(
        onDismissRequest = { dismiss(CaptchaResult.Cancelled) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismiss(CaptchaResult.Cancelled) }, // 点击背景关闭
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = TweenSpec(durationMillis = 300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = TweenSpec(durationMillis = 300)
                )
            ) {
                CaptchaContent(
                    action = action,
                    onResult = { result ->
                        dismiss(result)
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CaptchaContent(
    action: String,
    onResult: (CaptchaResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webView: WebView? by remember { mutableStateOf(null) }
    
    // 状态管理
    val specialCode = remember { UUID.randomUUID().toString() }
    var pollingJob: Job? by remember { mutableStateOf(null) }
    
    // JS 接口定义
    val jsInterface = remember {
        object {
            @JavascriptInterface
            fun onSuccess(code: String?) {
                pollingJob?.cancel()
                scope.launch(Dispatchers.Main) {
                    if (!code.isNullOrEmpty()) {
                        onResult(CaptchaResult.Success(specialCode))
                    } else {
                        onResult(CaptchaResult.Failure("验证返回为空"))
                    }
                }
            }

            @JavascriptInterface
            fun onFail() {
                pollingJob?.cancel()
                scope.launch(Dispatchers.Main) {
                    onResult(CaptchaResult.Failure("验证失败"))
                }
            }

            @JavascriptInterface
            fun onCancel() {
                pollingJob?.cancel()
                scope.launch(Dispatchers.Main) {
                    onResult(CaptchaResult.Cancelled)
                }
            }
        }
    }

    // 逻辑处理
    LaunchedEffect(Unit) {
        try {
            // 1. 请求 token
            val request = CaptchaTokenRequest().apply {
                this.special_code = specialCode
                this.action = action
            }
            
            val response = withContext(Dispatchers.IO) {
                ServiceManager.CAPTCHA_SERVICE?.requestToken(request)
            }
            
            if (response?.isSuccess() == true && response.data?.verification_token != null) {
                val token = response.data!!.verification_token!!
                
                // 2. 加载页面
                withContext(Dispatchers.Main) {
                    webView?.loadUrl("https://abyssous.site/captcha/verifing?token=$token")
                }
                
                // 3. 开始轮询
                pollingJob = launch(Dispatchers.IO) {
                    val startTime = System.currentTimeMillis()
                    while (isActive) {
                        if (System.currentTimeMillis() - startTime > 300_000L) { // 5分钟超时
                            withContext(Dispatchers.Main) {
                                onResult(CaptchaResult.Failure("验证超时"))
                            }
                            break
                        }
                        
                        delay(1500)
                        
                        try {
                            val statusResponse = ServiceManager.CAPTCHA_SERVICE?.getStatus(specialCode)
                            if (statusResponse?.isSuccess() == true && statusResponse.data != null) {
                                when (statusResponse.data!!.status) {
                                    CaptchaStatusResponse.STATUS_VERIFIED -> {
                                        withContext(Dispatchers.Main) {
                                            onResult(CaptchaResult.Success(specialCode))
                                        }
                                        break
                                    }
                                    CaptchaStatusResponse.STATUS_FAILED -> {
                                        withContext(Dispatchers.Main) {
                                            onResult(CaptchaResult.Failure("验证失败"))
                                        }
                                        break
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } else {
                onResult(CaptchaResult.Failure(response?.message ?: "获取验证凭证失败"))
            }
        } catch (e: Exception) {
            onResult(CaptchaResult.Failure("网络错误: ${e.message}"))
        }
    }
    
    // UI 布局
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp) // 给定高度
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(CHelperTheme.colors.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                // 拦截点击事件，防止穿透到背景关闭 Dialog
            }
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.domStorageEnabled = true
                    setBackgroundColor(Color.WHITE)
                    addJavascriptInterface(jsInterface, "android")
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript("""
                                window.androidCallback = function(result) {
                                    android.onSuccess(result);
                                };
                            """.trimIndent(), null)
                        }
                    }
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
    
    // 拦截物理返回键
    BackHandler {
        onResult(CaptchaResult.Cancelled)
    }
}
