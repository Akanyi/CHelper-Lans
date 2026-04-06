/**
 * It is part of CHelper. CHelper is a command helper for Minecraft Bedrock Edition.
 * Copyright (C) 2026  Akanyi
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

package yancey.chelper.network.library.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * author 字段在 API 中有两种格式:
 *  - 旧式: 纯字符串 "作者名"
 *  - 新式: { "id": 123, "name": "作者名", "tier": 1 }
 * 这个结构体统一承接两种，后向兼容旧 MCD 数据
 */
@Serializable
data class AuthorInfo(
    var id: Int? = null,
    var name: String? = null,
    var tier: Int? = null
)

@Serializable
@Suppress("unused")
class LibraryFunction(
    var id: Int? = null,
    var uuid: String? = null,
    var name: String? = null,
    var content: String? = null,
    @Serializable(with = AuthorSerializer::class) var author: AuthorInfo? = null,
    var note: String? = null,
    var tags: List<String>? = null,
    var version: String? = null,
    @Serializable(with = LenientStringSerializer::class) @SerialName("createTime") var createdAt: String? = null,
    var preview: String? = null,
    var likeCount: Int? = null,
    var isLiked: Boolean? = null,
    var hasPublicVersion: Boolean? = null,
    var isPublish: Boolean? = null
) {
    /** 向后兼容：取 author 的展示名 */
    val authorName: String?
        get() = author?.name
}

/**
 * 兼容新旧两种 author JSON 格式的序列化器。
 * - JsonObject { id, name, tier } → AuthorInfo(id, name, tier)
 * - 纯字符串 "xxx" → AuthorInfo(name = "xxx")
 * - null → null
 */
object AuthorSerializer : KSerializer<AuthorInfo?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AuthorInfo") {
        element<Int?>("id")
        element<String?>("name")
        element<Int?>("tier")
    }

    override fun deserialize(decoder: Decoder): AuthorInfo? {
        require(decoder is JsonDecoder) { "This serializer can only be used with JSON" }
        val jsonElement = decoder.decodeJsonElement()
        return when {
            jsonElement is JsonNull -> null
            jsonElement is JsonObject -> {
                AuthorInfo(
                    id = jsonElement["id"]?.jsonPrimitive?.intOrNull,
                    name = jsonElement["name"]?.jsonPrimitive?.content,
                    tier = jsonElement["tier"]?.jsonPrimitive?.intOrNull
                )
            }
            // 旧版 API 直接返回字符串作者名
            jsonElement.jsonPrimitive.isString -> AuthorInfo(name = jsonElement.jsonPrimitive.content)
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: AuthorInfo?) {
        if (value != null) {
            encoder.encodeString(value.name ?: "")
        }
    }
}

/**
 * 宽松的字符串序列化器，允许接收数字或布尔并转换为字符串
 */
object LenientStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LenientString")

    override fun deserialize(decoder: Decoder): String? {
        require(decoder is JsonDecoder) { "This serializer can only be used with JSON" }
        val jsonElement = decoder.decodeJsonElement()
        return if (jsonElement is JsonNull) {
            null
        } else {
            jsonElement.jsonPrimitive.content
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) {
            encoder.encodeString(value)
        } else {
            encoder.encodeNull()
        }
    }
}
