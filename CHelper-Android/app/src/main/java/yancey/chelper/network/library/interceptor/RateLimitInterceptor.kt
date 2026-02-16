package yancey.chelper.network.library.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import yancey.chelper.android.common.util.Settings
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 请求速率限制拦截器
 * 
 * 限制对 abyssous.site 的请求频率，防止触发 WAF 或过载
 * 采用令牌桶算法
 */
class RateLimitInterceptor : Interceptor {

    private val lastRequestTime = AtomicLong(0)
    
    // 简单的漏桶/令牌桶实现
    // 这里简化为：确保两次请求间隔不小于 1000 / limit 毫秒
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        if (request.url.host == "abyssous.site") {
            val limit = Settings.INSTANCE.requestRateLimit ?: 5
            if (limit > 0) {
                val minInterval = 1000L / limit
                synchronized(this) {
                    val now = System.currentTimeMillis()
                    val last = lastRequestTime.get()
                    val nextAllowed = last + minInterval
                    
                    if (now < nextAllowed) {
                        val waitTime = nextAllowed - now
                        if (waitTime > 0) {
                            try {
                                Log.d("RateLimit", "Throttling request by ${waitTime}ms")
                                Thread.sleep(waitTime)
                            } catch (e: InterruptedException) {
                                Thread.currentThread().interrupt()
                                throw IOException("Request interrupted during rate limiting", e)
                            }
                        }
                    }
                    lastRequestTime.set(System.currentTimeMillis())
                }
            }
        }
        
        return chain.proceed(request)
    }
}
