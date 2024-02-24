package org.jukeboxmc.plugin.perms.command

import org.jukeboxmc.api.JukeboxMC
import org.jukeboxmc.api.command.Command
import org.jukeboxmc.api.command.CommandSender
import org.jukeboxmc.api.command.ParameterType
import org.jukeboxmc.api.command.annotation.*
import org.jukeboxmc.api.player.Player
import org.jukeboxmc.plugin.perms.api.manager.GroupManager
import org.jukeboxmc.plugin.perms.api.manager.PermissionManager
import org.jukeboxmc.plugin.perms.api.model.Group
import org.jukeboxmc.plugin.perms.model.GroupImpl
import org.jukeboxmc.plugin.perms.repository.GroupsRepository
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository
import org.jukeboxmc.plugin.perms.repository.UsersRepository
import java.util.*

/**
 * @author Kaooot
 * @version 1.0
 */
@Name("jperms")
@Permission("jperms.command")
@Description("JPerms management")
@Parameters(
    parameter = [
        Parameter(name = "permAction", enumValues = ["addperm", "removeperm"]),
        Parameter(name = "player", parameterType = ParameterType.TARGET),
        Parameter(name = "permission", parameterType = ParameterType.STRING)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "userinfo", enumValues = ["userinfo"]),
        Parameter(name = "player", parameterType = ParameterType.TARGET)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "groups", enumValues = ["groups"])
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "creategroup", enumValues = ["creategroup"]),
        Parameter(name = "id", parameterType = ParameterType.STRING),
        Parameter(name = "priority", parameterType = ParameterType.INT),
        Parameter(name = "nameTag", parameterType = ParameterType.STRING),
        Parameter(name = "chatFormat", parameterType = ParameterType.STRING),
        Parameter(name = "parentId", parameterType = ParameterType.STRING, optional = true)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "groupnametag", enumValues = ["groupnametag"]),
        Parameter(name = "group", parameterType = ParameterType.STRING),
        Parameter(name = "nameTag", parameterType = ParameterType.STRING)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "groupchatformat", enumValues = ["groupchatformat"]),
        Parameter(name = "group", parameterType = ParameterType.STRING),
        Parameter(name = "chatFormat", parameterType = ParameterType.STRING)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "groupparent", enumValues = ["groupparent"]),
        Parameter(name = "group", parameterType = ParameterType.STRING),
        Parameter(name = "parent", parameterType = ParameterType.STRING)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "groupPermAction", enumValues = ["addgroupperm", "removegroupperm"]),
        Parameter(name = "group", parameterType = ParameterType.STRING),
        Parameter(name = "permission", parameterType = ParameterType.STRING)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "groupinfo", enumValues = ["groupinfo"]),
        Parameter(name = "group", parameterType = ParameterType.STRING)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "groupAction", enumValues = ["addgroup", "removegroup"]),
        Parameter(name = "player", parameterType = ParameterType.TARGET),
        Parameter(name = "group", parameterType = ParameterType.STRING)
    ]
)
@Parameters(
    parameter = [
        Parameter(name = "deletegroup", enumValues = ["deletegroup"]),
        Parameter(name = "group", parameterType = ParameterType.STRING)
    ]
)
class JPermsCommand(
    private val permissionManager: PermissionManager,
    private val groupManager: GroupManager,
    private val repository: GroupsRepository,
    private val usersRepository: UsersRepository,
    private val permissionsRepository: PermissionsRepository
) : Command {

    private val arguments: List<String> = mutableListOf(
        "addperm", "removeperm", "userinfo",
        "groups", "creategroup", "groupnametag", "groupchatformat", "groupparent", "addgroupperm",
        "removegroupperm", "groupinfo", "addgroup", "removegroup", "deletegroup"
    )

    override fun execute(commandSender: CommandSender, command: String, args: Array<String>) {
        if (commandSender !is Player) {
            commandSender.sendMessage("§cThis command can only be used by a player!")

            return
        }

        if (args.isEmpty()) {
            this.sendHelp(commandSender)

            return
        }

        val argument = args[0]

        if (this.arguments.stream().noneMatch { s: String -> s.equals(argument, ignoreCase = true) }) {
            this.sendHelp(commandSender)

            return
        }
        if (argument.equals("groups", ignoreCase = true)) {
            commandSender.sendMessage("JPerms Groups:")

            for (group: Group in this.groupManager.groups()) {
                commandSender.sendMessage("- " + group.id())
            }

            return
        }

        if (args.size >= 5 && argument.equals("creategroup", ignoreCase = true)) {
            val id = args[1].lowercase(Locale.getDefault())

            try {
                val priority = args[2].toInt()
                val group = GroupImpl(id, priority)
                group.nameTag(args[3])
                group.chatFormat(args[4])

                if (args.size > 5) {
                    val parentId = args[5].lowercase(Locale.getDefault())

                    if (this.groupManager.groupById(parentId).isEmpty) {
                        commandSender.sendMessage("§cThe group that was provided as parent does not exist")

                        return
                    }

                    group.parentId(args[5])
                }

                val success = this.groupManager.createGroup(group)

                if (success) {
                    commandSender.sendMessage("§aThe group §f${group.id()} §ahas been successfully created")
                } else {
                    commandSender.sendMessage("§cThe group could not be created")
                }
            } catch (e: NumberFormatException) {
                commandSender.sendMessage("§cThe provided priority is invalid")

                return
            }

            return
        }

        if (args.size >= 3 && argument.equals("groupnametag", ignoreCase = true)) {
            val id = args[1].lowercase(Locale.getDefault())
            val nameTag = args[2]

            if (this.groupManager.groupById(id).isEmpty) {
                commandSender.sendMessage("§cThe group does not exist")

                return
            }

            this.repository.updateGroupNameTag(id, nameTag)

            commandSender.sendMessage("§aThe name tag of the group §f$id §ahas been set to $nameTag")

            return
        }
        if (args.size >= 3 && argument.equals("groupchatformat", ignoreCase = true)) {
            val id = args[1].lowercase(Locale.getDefault())
            val chatFormat = args[2]

            if (this.groupManager.groupById(id).isEmpty) {
                commandSender.sendMessage("§cThe group does not exist")

                return
            }

            this.repository.updateGroupChatFormat(id, chatFormat)

            commandSender.sendMessage("§aThe chat format of the group §f$id §ahas been set to $chatFormat")

            return
        }
        if (args.size >= 3 && argument.equals("groupparent", ignoreCase = true)) {
            val id = args[1].lowercase(Locale.getDefault())
            val parentId = args[2].lowercase(Locale.getDefault())

            if (this.groupManager.groupById(id).isEmpty) {
                commandSender.sendMessage("§cThe group does not exist")

                return
            }

            if (this.groupManager.groupById(parentId).isEmpty) {
                commandSender.sendMessage("§cThe provided parent does not exist")

                return
            }

            if (id.equals(parentId, ignoreCase = true)) {
                commandSender.sendMessage("§cThe group cannot be its parent at the same time")

                return
            }

            this.repository.updateGroupParent(id, parentId)

            commandSender.sendMessage("§aThe parent of the group §f $id §ahas been set to $parentId")

            return
        }
        if (args.size >= 2 && argument.equals("groupinfo", ignoreCase = true)) {
            val id = args[1].lowercase(Locale.getDefault())
            val optional = this.groupManager.groupById(id)

            if (optional.isEmpty) {
                commandSender.sendMessage("§cThe group does not exist")

                return
            }

            val group = optional.get()

            commandSender.sendMessage("JPerms Group ${group.id()} (priority: ${group.priority()})")
            commandSender.sendMessage("Parent: ${group.parentId().ifEmpty { "§cNone" }}")
            commandSender.sendMessage("Name Tag: " + group.nameTag())
            commandSender.sendMessage("Chat Format: " + group.chatFormat())
            commandSender.sendMessage("Permissions: ${(if (group.permissions().size == 1) group.permissions().toTypedArray<String>()[0] else (if (group.permissions().isEmpty()) "§cNone" else java.lang.String.join(", ", group.permissions())))}")

            return
        }

        if (args.size >= 2 && argument.equals("deletegroup", ignoreCase = true)) {
            val id = args[1].lowercase(Locale.getDefault())

            if (this.groupManager.groupById(id).isEmpty) {
                commandSender.sendMessage("§cThe group does not exist")

                return
            }

            this.groupManager.deleteGroup(id)

            commandSender.sendMessage("§cThe group §f$id §chas been deleted")
        }
        if (args.size >= 3 && argument.equals("addperm", ignoreCase = true)) {
            val username = args[1]
            val permission = args[2].lowercase(Locale.getDefault())

            this.updateUserPerm(username, permission, commandSender, true)
        }

        if (args.size >= 3 && argument.equals("removeperm", ignoreCase = true)) {
            val username = args[1]
            val permission = args[2].lowercase(Locale.getDefault())
            this.updateUserPerm(username, permission, commandSender, false)
        }

        if (args.size >= 3 && argument.equals("addgroupperm", ignoreCase = true)) {
            val id = args[1].lowercase(Locale.getDefault())
            val permission = args[2].lowercase(Locale.getDefault())

            this.updateGroupPerm(id, permission, commandSender, true)
        }
        if (args.size >= 3 && argument.equals("removegroupperm", ignoreCase = true)) {
            val id = args[1].lowercase(Locale.getDefault())
            val permission = args[2].lowercase(Locale.getDefault())

            this.updateGroupPerm(id, permission, commandSender, false)
        }
        if (args.size >= 3 && argument.equals("addgroup", ignoreCase = true)) {
            val username = args[1]
            val id = args[2].lowercase(Locale.getDefault())

            this.updateUserGroup(id, username, commandSender, true)
        }
        if (args.size >= 3 && argument.equals("removegroup", ignoreCase = true)) {
            val username = args[1]
            val id = args[2].lowercase(Locale.getDefault())

            this.updateUserGroup(id, username, commandSender, false)
        }
        if (args.size >= 2 && argument.equals("userinfo", ignoreCase = true)) {
            val username = args[1]
            val target = JukeboxMC.getServer().getPlayer(username)

            if (target != null) {
                val uuid = target.getUUID()

                this.sendUserInfo(commandSender, uuid, target.getName(), this.groupManager.cachedGroup(uuid), this.permissionsRepository.cachedPermissions(uuid))

                return
            }
            this.usersRepository.uuid(username).whenComplete { uuid: UUID?, _: Throwable? ->
                if (uuid == null) {
                    commandSender.sendMessage("§cThis user could not be found")

                    return@whenComplete
                }

                this.usersRepository.name(uuid).whenComplete { name: String?, _: Throwable ->
                    this.groupManager.group(uuid).whenComplete { group: Group?, _: Throwable ->
                        this.permissionsRepository.select(uuid).whenComplete { permissions: MutableSet<String>?, _: Throwable ->
                            this.sendUserInfo(commandSender, uuid, name!!, group, permissions)
                        }
                    }
                }
            }
        }
    }

    private fun sendHelp(player: Player) {
        player.sendMessage("JPerms help")
        player.sendMessage("/jperms addperm <user> <permission>")
        player.sendMessage("/jperms removeperm <user> <permission>")
        player.sendMessage("/jperms userinfo <user>")
        player.sendMessage("/jperms groups")
        player.sendMessage(
            "/jperms creategroup <id> <priority> <nameTag> <chatFormat> " +
                    "[<parentId>]"
        )
        player.sendMessage("/jperms groupnametag <id> <nameTag>")
        player.sendMessage("/jperms groupchatformat <id> <chatFormat>")
        player.sendMessage("/jperms groupparent <id> <parentId>")
        player.sendMessage("/jperms addgroupperm <group> <permission>")
        player.sendMessage("/jperms removegroupperm <group> <permission>")
        player.sendMessage("/jperms groupinfo <group>")
        player.sendMessage("/jperms addgroup <user> <group>")
        player.sendMessage("/jperms removegroup <user> <group>")
        player.sendMessage("/jperms deletegroup <id>")
    }

    private fun updateUserPerm(username: String, permission: String, player: Player, add: Boolean) {
        if (!this.validatePerm(permission)) {
            player.sendMessage("§cThe permission must not contain numbers")

            return
        }

        val target = JukeboxMC.getServer().getPlayer(username)

        if (target != null) {
            if (add) {
                this.addPerm(player, target.getUUID(), target.getName(), permission)
            } else {
                this.removePerm(player, target.getUUID(), target.getName(), permission)
            }

            return
        }

        this.usersRepository.uuid(username).whenComplete { uuid: UUID?, _: Throwable? ->
            if (uuid == null) {
                player.sendMessage("§cThis user could not be found")

                return@whenComplete
            }

            this.usersRepository.name(uuid).whenComplete { name: String?, _: Throwable? ->
                if (add) {
                    this.addPerm(player, uuid, name!!, permission)
                } else {
                    this.removePerm(player, uuid, name!!, permission)
                }
            }
        }
    }

    private fun updateGroupPerm(id: String, permission: String, player: Player, add: Boolean) {
        if (!this.validatePerm(permission)) {
            player.sendMessage("§cThe permission must not contain numbers")

            return
        }

        val optional = this.groupManager.groupById(id)

        if (optional.isEmpty) {
            player.sendMessage("§cThe group does not exist")

            return
        }

        val newPerms: MutableSet<String> = HashSet(optional.get().permissions())

        if ((!add && !newPerms.remove(permission)) || (add && !newPerms.add(permission))) {
            player.sendMessage("§cFailed to ${if (add) "add" else "remove"} permission §f$permission §c${if (add) "to" else "from"}  group §f$id")

            return
        }

        this.repository.updateGroupPerms(id, newPerms)

        player.sendMessage("§aThe permission §f$permission  §ahas been ${if (add) "added to" else "removed from"}  group §f$id")
    }

    private fun updateUserGroup(id: String, username: String, player: Player, add: Boolean) {
        if (this.groupManager.groupById(id).isEmpty) {
            player.sendMessage("§cThe group does not exist")

            return
        }

        val target = JukeboxMC.getServer().getPlayer(username)

        if (target != null) {
            if (add) {
                this.addGroup(player, target.getUUID(), target.getName(), id)
            } else {
                this.removeGroup(player, target.getUUID(), target.getName(), id)
            }

            return
        }

        this.usersRepository.uuid(username).whenComplete { uuid: UUID?, _: Throwable? ->
            if (uuid == null) {
                player.sendMessage("§cThis user could not be found")

                return@whenComplete
            }

            this.usersRepository.name(uuid).whenComplete { name: String?, _: Throwable? ->
                if (add) {
                    this.addGroup(player, uuid, name!!, id)
                } else {
                    this.removeGroup(player, uuid, name!!, id)
                }
            }
        }
    }

    private fun addPerm(player: Player, uuid: UUID, name: String, permission: String) {
        this.permissionManager.addPermission(uuid, permission).whenComplete { success: Boolean?, _: Throwable? ->
            if (success == null || !success) {
                player.sendMessage("§cFailed to add the permission §f$permission §cto §f$name")

                return@whenComplete
            }

            player.sendMessage("§aThe permission §f$permission §ahas been added to §f$name")
        }
    }

    private fun removePerm(player: Player, uuid: UUID, name: String, permission: String) {
        this.permissionManager.removePermission(uuid, permission).whenComplete { success: Boolean?, _: Throwable? ->
            if (success == null || !success) {
                player.sendMessage("§cFailed to remove the permission §f$permission §cfrom §f$name")

                return@whenComplete
            }

            player.sendMessage("§aThe permission §f$permission §ahas been removed from §f$name")
        }
    }

    private fun addGroup(player: Player, uuid: UUID, name: String, group: String) {
        this.groupManager.addGroup(uuid, group).whenComplete { success: Boolean?, _: Throwable? ->
            if (success == null || !success) {
                player.sendMessage("§cFailed to add the group §f$group §cto user §f$name")

                return@whenComplete
            }

            player.sendMessage("§aThe group §f$group §ahas been added to user §f$name")
        }
    }

    private fun removeGroup(player: Player, uuid: UUID, name: String, group: String) {
        this.groupManager.removeGroup(uuid, group).whenComplete { success: Boolean?, _: Throwable? ->
            if (success == null || !success) {
                player.sendMessage("§cFailed to remove the group §f$group §cfrom user §f$name")

                return@whenComplete
            }

            player.sendMessage("§aThe group §f$group §ahas been removed from user §f$name")
        }
    }

    private fun sendUserInfo(player: Player, uuid: UUID, name: String, group: Group?, permissions: Set<String>?) {
        player.sendMessage("JPerms User info for $name")
        player.sendMessage("UUID: $uuid")
        player.sendMessage("Group: " + (group?.id() ?: "§cNone"))
        player.sendMessage("Permissions: ${if (permissions!!.size == 1) permissions.toTypedArray<String>()[0] else (if (permissions.isEmpty()) "§cNone" else java.lang.String.join(", ", permissions))}")
    }

    private fun validatePerm(permission: String): Boolean {
        for (c: Char in "0123456789".toCharArray()) {
            if (permission.contains(c.toString())) {
                return false
            }
        }

        return true
    }
}