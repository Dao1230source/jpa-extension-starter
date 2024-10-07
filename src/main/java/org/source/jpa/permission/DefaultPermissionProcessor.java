package org.source.jpa.permission;

import java.util.Collection;

public class DefaultPermissionProcessor implements PermissionProcessor {
    @Override
    public Collection<String> getSpaceIdsByUserId(String userId) {
        throw new UnsupportedOperationException("you must implement this method: getSpaceIdsByUserId");
    }
}
