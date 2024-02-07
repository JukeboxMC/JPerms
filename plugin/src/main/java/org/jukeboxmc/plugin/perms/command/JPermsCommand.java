package org.jukeboxmc.plugin.perms.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jukeboxmc.api.JukeboxMC;
import org.jukeboxmc.api.command.Command;
import org.jukeboxmc.api.command.CommandSender;
import org.jukeboxmc.api.command.ParameterType;
import org.jukeboxmc.api.command.annotation.Description;
import org.jukeboxmc.api.command.annotation.Name;
import org.jukeboxmc.api.command.annotation.Parameter;
import org.jukeboxmc.api.command.annotation.Parameters;
import org.jukeboxmc.api.command.annotation.Permission;
import org.jukeboxmc.api.player.Player;
import org.jukeboxmc.plugin.perms.api.manager.GroupManager;
import org.jukeboxmc.plugin.perms.api.manager.PermissionManager;
import org.jukeboxmc.plugin.perms.api.model.Group;
import org.jukeboxmc.plugin.perms.model.GroupImpl;
import org.jukeboxmc.plugin.perms.repository.GroupsRepository;
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository;
import org.jukeboxmc.plugin.perms.repository.UsersRepository;

/**
 * @author Kaooot
 * @version 1.0
 */
@Singleton
@Name("jperms")
@Permission("jperms.command")
@Description("JPerms management")
@Parameters(parameter = {
    @Parameter(name = "permAction", enumValues = {"addperm", "removeperm"}),
    @Parameter(name = "player", parameterType = ParameterType.TARGET),
    @Parameter(name = "permission", parameterType = ParameterType.STRING)
})
@Parameters(parameter = {
    @Parameter(name = "userinfo", enumValues = "userinfo"),
    @Parameter(name = "player", parameterType = ParameterType.TARGET)
})
@Parameters(parameter = {
    @Parameter(name = "groups", enumValues = "groups")
})
@Parameters(parameter = {
    @Parameter(name = "creategroup", enumValues = "creategroup"),
    @Parameter(name = "id", parameterType = ParameterType.STRING),
    @Parameter(name = "priority", parameterType = ParameterType.INT),
    @Parameter(name = "nameTag", parameterType = ParameterType.STRING),
    @Parameter(name = "chatFormat", parameterType = ParameterType.STRING),
    @Parameter(name = "parentId", parameterType = ParameterType.STRING, optional = true),
})
@Parameters(parameter = {
    @Parameter(name = "groupnametag", enumValues = "groupnametag"),
    @Parameter(name = "group", parameterType = ParameterType.STRING),
    @Parameter(name = "nameTag", parameterType = ParameterType.STRING)
})
@Parameters(parameter = {
    @Parameter(name = "groupchatformat", enumValues = "groupchatformat"),
    @Parameter(name = "group", parameterType = ParameterType.STRING),
    @Parameter(name = "chatFormat", parameterType = ParameterType.STRING)
})
@Parameters(parameter = {
    @Parameter(name = "groupparent", enumValues = "groupparent"),
    @Parameter(name = "group", parameterType = ParameterType.STRING),
    @Parameter(name = "parent", parameterType = ParameterType.STRING)
})
@Parameters(parameter = {
    @Parameter(name = "groupPermAction", enumValues = {"addgroupperm", "removegroupperm"}),
    @Parameter(name = "group", parameterType = ParameterType.STRING),
    @Parameter(name = "permission", parameterType = ParameterType.STRING)
})
@Parameters(parameter = {
    @Parameter(name = "groupinfo", enumValues = "groupinfo"),
    @Parameter(name = "group", parameterType = ParameterType.STRING)
})
@Parameters(parameter = {
    @Parameter(name = "groupAction", enumValues = {"addgroup", "removegroup"}),
    @Parameter(name = "player", parameterType = ParameterType.TARGET),
    @Parameter(name = "group", parameterType = ParameterType.STRING)
})
@Parameters(parameter = {
    @Parameter(name = "deletegroup", enumValues = "deletegroup"),
    @Parameter(name = "group", parameterType = ParameterType.STRING)
})
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class JPermsCommand implements Command {

    private final PermissionManager permissionManager;
    private final GroupManager groupManager;
    private final GroupsRepository repository;
    private final UsersRepository usersRepository;
    private final PermissionsRepository permissionsRepository;

    private final List<String> arguments = Arrays.asList("addperm", "removeperm", "userinfo",
        "groups", "creategroup", "groupnametag", "groupchatformat", "groupparent", "addgroupperm",
        "removegroupperm", "groupinfo", "addgroup", "removegroup", "deletegroup");

    @Override
    public void execute(@NotNull CommandSender commandSender, @NotNull String label,
                        @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("§cThis command can only be used by a player!");

            return;
        }

        if (args.length < 1) {
            this.sendHelp(player);

            return;
        }

        final String argument = args[0];

        if (this.arguments.stream().noneMatch(s -> s.equalsIgnoreCase(argument))) {
            this.sendHelp(player);

            return;
        }

        if (argument.equalsIgnoreCase("groups")) {
            player.sendMessage("JPerms Groups:");

            for (final Group group : this.groupManager.groups()) {
                player.sendMessage("- " + group.id());
            }

            return;
        }

        if (args.length >= 5 && argument.equalsIgnoreCase("creategroup")) {
            final String id = args[1].toLowerCase();

            try {
                final int priority = Integer.parseInt(args[2]);

                final GroupImpl group = new GroupImpl(id, priority);
                group.nameTag(args[3]);
                group.chatFormat(args[4]);

                if (args.length > 5) {
                    final String parentId = args[5].toLowerCase();

                    if (this.groupManager.groupById(parentId).isEmpty()) {
                        player.sendMessage("§cThe group that was provided as parent does " +
                            "not exist");

                        return;
                    }

                    group.parentId(args[5]);
                }

                final boolean success = this.groupManager.createGroup(group);

                if (success) {
                    player.sendMessage("§aThe group §f" + group.id() + " §ahas been successfully " +
                        "created");
                } else {
                    player.sendMessage("§cThe group could not be created");
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cThe provided priority is invalid");

                return;
            }

            return;
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("groupnametag")) {
            final String id = args[1].toLowerCase();
            final String nameTag = args[2];

            if (this.groupManager.groupById(id).isEmpty()) {
                player.sendMessage("§cThe group does not exist");

                return;
            }

            this.repository.updateGroupNameTag(id, nameTag);

            player.sendMessage("§aThe name tag of the group §f" + id +
                " §ahas been set to " + nameTag);

            return;
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("groupchatformat")) {
            final String id = args[1].toLowerCase();
            final String chatFormat = args[2];

            if (this.groupManager.groupById(id).isEmpty()) {
                player.sendMessage("§cThe group does not exist");

                return;
            }

            this.repository.updateGroupChatFormat(id, chatFormat);

            player.sendMessage("§aThe chat format of the group §f" + id +
                " §ahas been set to " + chatFormat);

            return;
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("groupparent")) {
            final String id = args[1].toLowerCase();
            final String parentId = args[2].toLowerCase();

            if (this.groupManager.groupById(id).isEmpty()) {
                player.sendMessage("§cThe group does not exist");

                return;
            }

            if (this.groupManager.groupById(parentId).isEmpty()) {
                player.sendMessage("§cThe provided parent does not exist");

                return;
            }

            if (id.equalsIgnoreCase(parentId)) {
                player.sendMessage("§cThe group cannot be its parent at the same time");

                return;
            }

            this.repository.updateGroupParent(id, parentId);

            player.sendMessage("§aThe parent of the group §f" + id +
                " §ahas been set to " + parentId);

            return;
        }

        if (args.length >= 2 && argument.equalsIgnoreCase("groupinfo")) {
            final String id = args[1].toLowerCase();

            final Optional<Group> optional = this.groupManager.groupById(id);

            if (optional.isEmpty()) {
                player.sendMessage("§cThe group does not exist");

                return;
            }

            final Group group = optional.get();

            player.sendMessage("JPerms Group " + group.id() + " (priority: " +
                group.priority() + ")");

            player.sendMessage("Parent: " + (group.parentId() == null ? "§cNone" :
                group.parentId()));
            player.sendMessage("Name Tag: " + group.nameTag());
            player.sendMessage("Chat Format: " + group.chatFormat());
            player.sendMessage("Permissions: " + (group.permissions().size() == 1 ?
                group.permissions().toArray(new String[0])[0] :
                (group.permissions().isEmpty() ? "§cNone" :
                    String.join(", ", group.permissions()))));

            return;
        }

        if (args.length >= 2 && argument.equalsIgnoreCase("deletegroup")) {
            final String id = args[1].toLowerCase();

            if (this.groupManager.groupById(id).isEmpty()) {
                player.sendMessage("§cThe group does not exist");

                return;
            }

            this.groupManager.deleteGroup(id);

            player.sendMessage("§cThe group §f" + id + " §chas been deleted");
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("addperm")) {
            final String username = args[1];
            final String permission = args[2].toLowerCase();

            this.updateUserPerm(username, permission, player, true);
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("removeperm")) {
            final String username = args[1];
            final String permission = args[2].toLowerCase();

            this.updateUserPerm(username, permission, player, false);
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("addgroupperm")) {
            final String id = args[1].toLowerCase();
            final String permission = args[2].toLowerCase();

            this.updateGroupPerm(id, permission, player, true);
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("removegroupperm")) {
            final String id = args[1].toLowerCase();
            final String permission = args[2].toLowerCase();

            this.updateGroupPerm(id, permission, player, false);
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("addgroup")) {
            final String username = args[1];
            final String id = args[2].toLowerCase();

            this.updateUserGroup(id, username, player, true);
        }

        if (args.length >= 3 && argument.equalsIgnoreCase("removegroup")) {
            final String username = args[1];
            final String id = args[2].toLowerCase();

            this.updateUserGroup(id, username, player, false);
        }

        if (args.length >= 2 && argument.equalsIgnoreCase("userinfo")) {
            final String username = args[1];
            final Player target = JukeboxMC.Companion.getServer().getPlayer(username);

            if (target != null) {
                final UUID uuid = target.getUUID();

                this.sendUserInfo(player, uuid, target.getName(),
                    this.groupManager.cachedGroup(uuid),
                    this.permissionsRepository.cachedPermissions(uuid));

                return;
            }

            this.usersRepository.uuid(username).whenComplete((uuid, throwable) -> {
                if (uuid == null) {
                    player.sendMessage("§cThis user could not be found");

                    return;
                }

                this.usersRepository.name(uuid).whenComplete((name, t) ->
                    this.groupManager.group(uuid).whenComplete((group, t1) ->
                        this.permissionsRepository.select(uuid).whenComplete((permissions, t2) ->
                            this.sendUserInfo(player, uuid, name, group, permissions))));
            });
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("JPerms help");
        player.sendMessage("/jperms addperm <user> <permission>");
        player.sendMessage("/jperms removeperm <user> <permission>");
        player.sendMessage("/jperms userinfo <user>");
        player.sendMessage("/jperms groups");
        player.sendMessage("/jperms creategroup <id> <priority> <nameTag> <chatFormat> " +
            "[<parentId>]");
        player.sendMessage("/jperms groupnametag <id> <nameTag>");
        player.sendMessage("/jperms groupchatformat <id> <chatFormat>");
        player.sendMessage("/jperms groupparent <id> <parentId>");
        player.sendMessage("/jperms addgroupperm <group> <permission>");
        player.sendMessage("/jperms removegroupperm <group> <permission>");
        player.sendMessage("/jperms groupinfo <group>");
        player.sendMessage("/jperms addgroup <user> <group>");
        player.sendMessage("/jperms removegroup <user> <group>");
        player.sendMessage("/jperms deletegroup <id>");
    }

    private void updateUserPerm(String username, String permission, Player player, boolean add) {
        if (!this.validatePerm(permission)) {
            player.sendMessage("§cThe permission must not contain numbers");

            return;
        }

        final Player target = JukeboxMC.Companion.getServer().getPlayer(username);

        if (target != null) {
            if (add) {
                this.addPerm(player, target.getUUID(), target.getName(), permission);
            } else {
                this.removePerm(player, target.getUUID(), target.getName(), permission);
            }

            return;
        }

        this.usersRepository.uuid(username).whenComplete((uuid, throwable) -> {
            if (uuid == null) {
                player.sendMessage("§cThis user could not be found");

                return;
            }

            this.usersRepository.name(uuid).whenComplete((name, t) -> {
                if (add) {
                    this.addPerm(player, uuid, name, permission);
                } else {
                    this.removePerm(player, uuid, name, permission);
                }
            });
        });
    }

    private void updateGroupPerm(String id, String permission, Player player, boolean add) {
        if (!this.validatePerm(permission)) {
            player.sendMessage("§cThe permission must not contain numbers");

            return;
        }

        final Optional<Group> optional = this.groupManager.groupById(id);

        if (optional.isEmpty()) {
            player.sendMessage("§cThe group does not exist");

            return;
        }

        final Set<String> newPerms = new HashSet<>(optional.get().permissions());

        if ((!add && !newPerms.remove(permission)) || (add && !newPerms.add(permission))) {
            player.sendMessage("§cFailed to " + (add ? "add" : "remove") +
                " permission §f" + permission + " §c" + (add ? "to" : "from") + " group §f" + id);

            return;
        }

        this.repository.updateGroupPerms(id, newPerms);

        player.sendMessage("§aThe permission §f" + permission + " §ahas been " + (add ? "added to" :
            "removed from") + " group §f" + id);
    }

    private void updateUserGroup(String id, String username, Player player, boolean add) {
        if (this.groupManager.groupById(id).isEmpty()) {
            player.sendMessage("§cThe group does not exist");

            return;
        }

        final Player target = JukeboxMC.Companion.getServer().getPlayer(username);

        if (target != null) {
            if (add) {
                this.addGroup(player, target.getUUID(), target.getName(), id);
            } else {
                this.removeGroup(player, target.getUUID(), target.getName(), id);
            }

            return;
        }

        this.usersRepository.uuid(username).whenComplete((uuid, throwable) -> {
            if (uuid == null) {
                player.sendMessage("§cThis user could not be found");

                return;
            }

            this.usersRepository.name(uuid).whenComplete((name, t1) -> {
                if (add) {
                    this.addGroup(player, uuid, name, id);
                } else {
                    this.removeGroup(player, uuid, name, id);
                }
            });
        });
    }

    private void addPerm(Player player, UUID uuid, String name, String permission) {
        this.permissionManager.addPermission(uuid, permission)
            .whenComplete((success, t2) -> {
                if (success == null || !success) {
                    player.sendMessage("§cFailed to add the permission §f" +
                        permission + " §cto §f" + name);

                    return;
                }

                player.sendMessage("§aThe permission §f" + permission +
                    " §ahas been added to §f" + name);
            });
    }

    private void removePerm(Player player, UUID uuid, String name, String permission) {
        this.permissionManager.removePermission(uuid, permission)
            .whenComplete((success, t2) -> {
                if (success == null || !success) {
                    player.sendMessage("§cFailed to remove the permission §f" +
                        permission + " §cfrom §f" + name);

                    return;
                }

                player.sendMessage("§aThe permission §f" + permission +
                    " §ahas been removed from §f" + name);
            });
    }

    private void addGroup(Player player, UUID uuid, String name, String group) {
        this.groupManager.addGroup(uuid, group).whenComplete((success, t) -> {
            if (success == null || !success) {
                player.sendMessage("§cFailed to add the group §f" + group +
                    " §cto user §f" + name);

                return;
            }

            player.sendMessage("§aThe group §f" + group + " §ahas been added " +
                "to user §f" + name);
        });
    }

    private void removeGroup(Player player, UUID uuid, String name, String group) {
        this.groupManager.removeGroup(uuid, group).whenComplete((success, t) -> {
            if (success == null || !success) {
                player.sendMessage("§cFailed to remove the group §f" +
                    group + " §cfrom " + "user §f" + name);

                return;
            }

            player.sendMessage("§aThe group §f" + group + " §ahas been" +
                " removed from user §f" + name);
        });
    }

    private void sendUserInfo(Player player, UUID uuid, String name, Group group,
                              Set<String> permissions) {
        player.sendMessage("JPerms User info for " + name);
        player.sendMessage("UUID: " + uuid);
        player.sendMessage("Group: " + (group != null ? group.id() : "§cNone"));
        player.sendMessage("Permissions: " +
            (permissions.size() == 1 ? permissions.toArray(new String[0])[0] :
                (permissions.isEmpty() ? "§cNone" :
                    String.join(", ", permissions))));
    }

    private boolean validatePerm(String permission) {
        for (final char c : "0123456789".toCharArray()) {
            if (permission.contains(String.valueOf(c))) {
                return false;
            }
        }

        return true;
    }
}