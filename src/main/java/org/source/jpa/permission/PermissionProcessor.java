package org.source.jpa.permission;

import java.util.Collection;

public interface PermissionProcessor {
    /**
     * getSpaceIdsByUserId
     *
     * @param userId userId
     * @return spaceIds
     */
    Collection<String> getSpaceIdsByUserId(String userId);
}
