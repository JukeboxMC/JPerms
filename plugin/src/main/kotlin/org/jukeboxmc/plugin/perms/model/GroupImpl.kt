package org.jukeboxmc.plugin.perms.model

import org.jukeboxmc.plugin.perms.api.model.Group

/**
 * @author Kaooot
 * @version 1.0
 */
class GroupImpl(private val id: String, private val priority: Int) : Group {

    private val inheritedPermissions: MutableSet<String> = HashSet()
    private var permissions: MutableSet<String> = HashSet()
    private var nameTag: String = ""
    private var chatFormat: String = ""
    private var parentId: String = ""

    companion object {
        fun ofApiModel(apiModel: Group): GroupImpl {
            val group = GroupImpl(apiModel.id(), apiModel.priority())
            group.nameTag = apiModel.nameTag()
            group.chatFormat = apiModel.chatFormat()
            group.permissions = apiModel.permissions()
            group.parentId = apiModel.parentId()

            return group
        }
    }

    override fun id() = this.id
    override fun priority() = this.priority
    override fun permissions() = this.permissions
    override fun nameTag() = this.nameTag
    override fun chatFormat() = this.chatFormat
    override fun parentId() = this.parentId

    fun inheritedPermissions() = this.inheritedPermissions

    fun permissions(permissions: MutableSet<String>) {
        this.permissions = permissions
    }

    fun nameTag(nameTag: String) {
        this.nameTag = nameTag
    }

    fun chatFormat(chatFormat: String) {
        this.chatFormat = chatFormat
    }

    fun parentId(parentId: String) {
        this.parentId = parentId
    }
}