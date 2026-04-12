package yancey.chelper.ui.library.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import yancey.chelper.network.library.util.LoginUtil
import yancey.chelper.network.ServiceManager
import yancey.chelper.network.library.data.UpdateProfileRequest
import yancey.chelper.network.library.data.UserProfileData
import yancey.chelper.network.library.data.LibraryFunction

class UserProfileViewModel : ViewModel() {
    var userProfile by mutableStateOf<UserProfileData?>(null)
        private set

    var currentUserId by mutableStateOf<Int?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var privateLibraries by mutableStateOf<List<LibraryFunction>>(emptyList())
        private set

    var isLoadingPrivate by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var viewUserId by mutableStateOf<Int>(-1)
        private set

    var isUpdating by mutableStateOf(false)
        private set

    var updateErrorMessage by mutableStateOf<String?>(null)
        private set
        
    var updateSuccessMessage by mutableStateOf<String?>(null)
        private set

    init {
        currentUserId = LoginUtil.currentUser?.id
    }

    fun loadProfile(id: Int) {
        viewUserId = id
        if (isLoading) return
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val res = ServiceManager.COMMAND_LAB_PUBLIC_SERVICE?.getUserProfile(id)
                if (res?.status == 0 && res.data != null) {
                    userProfile = res.data
                } else {
                    errorMessage = res?.message ?: "拉取主页失败"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "网络错误"
            } finally {
                isLoading = false
            }
            if (currentUserId == id) {
                loadPrivateLibraries()
            }
        }
    }

    fun refresh() {
        if (viewUserId != -1) {
            loadProfile(viewUserId)
            if (currentUserId == viewUserId) {
                loadPrivateLibraries()
            }
        }
    }

    fun loadPrivateLibraries() {
        if (isLoadingPrivate) return
        isLoadingPrivate = true
        viewModelScope.launch {
            try {
                val res = ServiceManager.COMMAND_LAB_USER_SERVICE?.getMyLibraries(type = 1, pageNum = 1, pageSize = 300)
                val responseData = res?.data
                if (res?.status == 0 && responseData != null) {
                    privateLibraries = responseData.functions?.filterNotNull() ?: emptyList()
                } else {
                    updateErrorMessage = res?.message ?: "拉取私有云库失败"
                }
            } catch (e: Exception) {
                updateErrorMessage = e.message ?: "网络错误"
            } finally {
                isLoadingPrivate = false
            }
        }
    }

    fun deleteOrUnpublishLibrary(libraryId: Int, isPublic: Boolean) {
        viewModelScope.launch {
            try {
                val res = ServiceManager.COMMAND_LAB_USER_SERVICE?.deleteLibrary(libraryId)
                if (res?.status == 0) {
                    updateSuccessMessage = if(isPublic) "下架成功" else "删除成功"
                    if (isPublic) {
                        val updatedList = userProfile?.recentFunctions?.filter { it.id != libraryId }
                        userProfile = userProfile?.copy(recentFunctions = updatedList)
                    } else {
                        privateLibraries = privateLibraries.filter { it.id != libraryId }
                    }
                } else {
                    updateErrorMessage = res?.message ?: "操作失败"
                }
            } catch (e: Exception) {
                updateErrorMessage = e.message ?: "网络错误"
            }
        }
    }

    fun updateProfile(nickname: String, avatarUrl: String?, homepage: String?, signature: String?, onComplete: () -> Unit) {
        if (isUpdating) return
        isUpdating = true
        updateErrorMessage = null
        updateSuccessMessage = null
        viewModelScope.launch {
            try {
                val req = UpdateProfileRequest(
                    nickname = nickname.ifBlank { null },
                    avatarUrl = avatarUrl?.ifBlank { null },
                    homepage = homepage?.ifBlank { null },
                    signature = signature?.ifBlank { null }
                )
                val res = ServiceManager.COMMAND_LAB_USER_SERVICE?.updateProfile(viewUserId, req)
                if (res?.status == 0) {
                    updateSuccessMessage = "资料已更新"
                    loadProfile(viewUserId) // reload
                    onComplete()
                } else {
                    updateErrorMessage = res?.message ?: "更新失败"
                }
            } catch (e: Exception) {
                updateErrorMessage = e.message ?: "网络错误"
            } finally {
                isUpdating = false
            }
        }
    }

    fun clearUpdateMessages() {
        updateErrorMessage = null
        updateSuccessMessage = null
    }
}
