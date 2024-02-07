package org.jukeboxmc.plugin.perms.model;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jukeboxmc.plugin.perms.api.model.Group;

/**
 * @author Kaooot
 * @version 1.0
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
public class GroupImpl implements Group {

    @Getter
    private final String id;
    @Getter
    private final int priority;
    @Getter
    private final Set<String> inheritedPermissions = new HashSet<>();

    @Setter
    @Getter
    private Set<String> permissions = new HashSet<>();
    @Setter
    @Getter
    private String nameTag;
    @Setter
    @Getter
    private String chatFormat;
    @Setter
    @Getter
    private String parentId;

    public static GroupImpl ofApiModel(Group apiModel) {
        final GroupImpl group = new GroupImpl(apiModel.id(), apiModel.priority());
        group.nameTag(apiModel.nameTag());
        group.chatFormat(apiModel.chatFormat());
        group.permissions(apiModel.permissions());
        group.parentId(apiModel.parentId());

        return group;
    }
}