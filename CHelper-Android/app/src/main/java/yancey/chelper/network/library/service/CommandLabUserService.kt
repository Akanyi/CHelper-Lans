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

package yancey.chelper.network.library.service

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import yancey.chelper.network.library.data.BaseResult

/**
 * CommandLab 用户系统 API
 * -by Akanyi
 * 包含注册、登录、用户资料管理等功能
 */
@Suppress("unused")
interface CommandLabUserService {
    
    // 注册相关
    
    /**
     * 发送邮箱验证码请求体
     */
    class SendMailRequest {
        @Suppress("PropertyName")
        var special_code: String? = null
        var type: Int? = null  // 0=注册, 1=更新密码, 2=找回密码
        var mail: String? = null
        var lang: String? = "zh-CN"
        
        companion object {
            const val TYPE_REGISTER = 0
            const val TYPE_UPDATE_PASSWORD = 1
            const val TYPE_RESET_PASSWORD = 2
        }
    }
    
    /**
     * 发送邮箱验证码
     * 
     * 需要先完成人机验证获取 special_code
     */
    @POST("register/sendMail")
    fun sendMail(@Body request: SendMailRequest): Call<BaseResult<Void?>>
    
    /**
     * 注册请求体
     */
    class RegisterRequest {
        @Suppress("PropertyName")
        var special_code: String? = null
        var mailCode: String? = null
        var mail: String? = null
        var nickname: String? = null
        var password: String? = null
        @Suppress("PropertyName")
        var android_id: String? = null
    }
    
    /**
     * 提交注册
     */
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<BaseResult<Void?>>
    
    // 登录相关
    
    /**
     * 登录请求体
     */
    class LoginRequest {
        @JvmField
        var mail: String? = null
        @JvmField
        var account: String? = null // From upstream
        @JvmField
        var password: String? = null
    }
    
    /**
     * 用户信息
     */
    class User {
        var id: Int? = null
        var email: String? = null
        var nickname: String? = null

        @Suppress("PropertyName")
        var is_guest: Boolean? = null
        @Suppress("PropertyName")
        var is_admin: Boolean? = null
        @Suppress("PropertyName")
        var is_moderator: Boolean? = null
        @Suppress("PropertyName")
        var gravatar_url: String? = null
    }
    
    /**
     * 登录响应
     */
    class LoginResponse {
        @Suppress("PropertyName")
        var user_id: Int? = null // Added from upstream
        var token: String? = null
        var user: User? = null
    }
    
    /**
     * 正式用户登录
     */
    @POST("register/login")
    fun login(@Body request: LoginRequest): Call<BaseResult<LoginResponse?>>
    
    // 访客相关
    
    /**
     * 访客注册请求体
     * 
     * auth_code 生成方式：
     * 1. 使用内置私钥对 fingerprint 进行 SHA256withECDSA 签名
     * 2. 将签名结果进行 Base64 编码
     * 
     * @see yancey.chelper.network.library.util.GuestAuthUtil
     */
    class GuestRegisterRequest {
        /** 设备指纹（如 Android ID 的 hash） */
        var fingerprint: String? = null
        /** 使用私钥对 fingerprint 签名后的结果（Base64 编码） */
        @Suppress("PropertyName")
        var auth_code: String? = null
    }
    
    /**
     * 访客注册
     * 
     * 使用设备指纹和客户端签名的授权码注册访客账号
     */
    @POST("guest/register")
    fun guestRegister(@Body request: GuestRegisterRequest): Call<BaseResult<LoginResponse?>>
    
    /**
     * 访客登录请求体
     */
    class GuestLoginRequest {
        var fingerprint: String? = null
        /** 使用私钥对 fingerprint 签名后的结果（Base64 编码） */
        @Suppress("PropertyName")
        var auth_code: String? = null
    }
    
    /**
     * 访客登录
     */
    @POST("guest/login")
    fun guestLogin(@Body request: GuestLoginRequest): Call<BaseResult<LoginResponse?>>
    
    // 用户资料
    
    /**
     * 获取当前用户资料
     */
    @GET("user/profile")
    fun getProfile(): Call<BaseResult<User?>>
    
    /**
     * 获取指定用户信息
     */
    @GET("user/{user_id}")
    fun getUser(@Path("user_id") userId: Int): Call<BaseResult<User?>>
    
    /**
     * 更新用户信息请求体
     */
    class UpdateUserRequest {
        var nickname: String? = null
    }
    
    /**
     * 更新用户信息
     */
    @PUT("users/{user_id}")
    fun updateUser(
        @Path("user_id") userId: Int,
        @Body request: UpdateUserRequest
    ): Call<BaseResult<UpdateUserRequest?>>
    
    // 密码管理
    
    /**
     * 更新密码请求体
     */
    class UpdatePasswordRequest {
        @Suppress("PropertyName")
        var special_code: String? = null
        var mailCode: String? = null
        var mail: String? = null
        @Suppress("PropertyName")
        var new_password: String? = null
    }
    
    /**
     * 更新密码
     * 
     * 需要先完成人机验证和邮箱验证
     */
    @POST("register/updatePassword")
    fun updatePassword(@Body request: UpdatePasswordRequest): Call<BaseResult<Void?>>
    
    // 账号注销
    
    /**
     * 账号注销请求体
     */
    class DeleteAccountRequest {
        @Suppress("PropertyName")
        var special_code: String? = null
        var mailCode: String? = null
        var mail: String? = null
    }
    
    /**
     * 申请注销账号
     * 
     * 账号将进入 30 天等待期，之后永久删除
     */
    @POST("register/deleteAccount")
    fun deleteAccount(@Body request: DeleteAccountRequest): Call<BaseResult<Void?>>
    
    // 管理员接口
    
    /**
     * 获取用户列表（管理员）
     */
    @GET("users")
    fun getUsers(@Query("keyword") keyword: String?): Call<BaseResult<List<User>?>>
    
    /**
     * 获取用户信息（管理员）
     */
    @GET("admin/user/{user_id}")
    fun adminGetUser(@Path("user_id") userId: Int): Call<BaseResult<User?>>
    
    /**
     * 更新用户角色请求体
     */
    class UpdateRolesRequest {
        @Suppress("PropertyName")
        var is_admin: Boolean? = null

        @Suppress("PropertyName")
        var is_moderator: Boolean? = null
    }
    
    /**
     * 更新用户角色（管理员）
     */
    @PUT("users/{user_id}/roles")
    fun updateRoles(
        @Path("user_id") userId: Int,
        @Body request: UpdateRolesRequest
    ): Call<BaseResult<Void?>>
    
    /**
     * 重置密码请求体
     */
    class ResetPasswordRequest {
        var email: String? = null
        var code: String? = null
        @Suppress("PropertyName")
        var new_password: String? = null
    }

    /**
     * 重置用户密码（管理员）
     */
    @PUT("users/{user_id}/reset-password")
    fun resetPassword(
        @Path("user_id") userId: Int,
        @Body request: ResetPasswordRequest
    ): Call<BaseResult<Void?>>

    class UserResetPasswordRequest {
        var email: String? = null
        var code: String? = null
        @Suppress("PropertyName")
        var new_password: String? = null
    }

    class ResetPasswordResponse {
        var message: String? = null
    }

    /**
     * 重置用户密码（用户自服务）
     */
    @POST("user/reset_password")
    fun userResetPassword(
        @Body request: UserResetPasswordRequest?
    ): Call<BaseResult<ResetPasswordResponse?>?>?

    class UpdateSettingRequest {
        var nickname: String? = null

        @Suppress("PropertyName")
        var old_password: String? = null

        @Suppress("PropertyName")
        var new_password: String? = null
    }

    class UpdateSettingResponse {
        var message: String? = null
    }

    @POST("web/update_settings")
    fun updateSetting(
        @Body request: UpdateSettingRequest?
    ): Call<BaseResult<UpdateSettingResponse?>?>?

    class LogoutResponse {
        var message: String? = null
    }

    @POST("user/logout")
    fun logout(): Call<BaseResult<LogoutResponse?>?>?

    class VerifySensitiveRequest {
        var operation: String? = null
        var code: String? = null
    }

    class VerifySensitiveResponse {
        var verified: Boolean? = null
    }

    @POST("user/verify_sensitive")
    fun verifySensitive(
        @Body request: VerifySensitiveRequest?
    ): Call<BaseResult<VerifySensitiveResponse?>?>?

    // Upstream Compatibility Layer (Renamed where necessary)

    class SendVerifyCodeRequest {
        @Suppress("PropertyName")
        var special_code: String? = null
        var type: Int? = null
        var email: String? = null
        var phone: String? = null
        var lang: String? = null
    }

    @POST("register/sendCode")
    fun sendVerifyCode(
        @Body request: SendVerifyCodeRequest?
    ): Call<BaseResult<Void?>?>?

    class WebRegisterRequest {
        var email: String? = null
    }

    class WebRegisterResponse {
        var message: String? = null
    }

    @POST("user/register")
    fun webRegister(
        @Body request: WebRegisterRequest?
    ): Call<BaseResult<WebRegisterResponse?>?>?

    class VerifyRegistrationRequest {
        var email: String? = null
        var code: String? = null
        var password: String? = null

        @Suppress("PropertyName")
        var android_id: String? = null
    }

    class VerifyRegistrationResponse {
        @Suppress("PropertyName")
        var user_id: Int? = null
        var token: String? = null
    }

    @POST("user/verify")
    fun verifyRegistration(
        @Body request: VerifyRegistrationRequest?
    ): Call<BaseResult<VerifyRegistrationResponse?>?>?

    @get:GET("web/user_info")
    val userInfo: Call<BaseResult<User?>?>?

    class CheckLoginResponse {
        @Suppress("PropertyName")
        var logged_in: Boolean? = null
        var user: User? = null
    }

    @GET("web/check_login")
    fun checkLogin(): Call<BaseResult<CheckLoginResponse?>?>?

    class SendResetCodeRequest {
        var email: String? = null
    }

    class SendResetCodeResponse {
        var message: String? = null
    }

    @POST("user/send_reset_code")
    fun sendResetCode(
        @Body request: SendResetCodeRequest?
    ): Call<BaseResult<SendResetCodeResponse?>?>?
}
