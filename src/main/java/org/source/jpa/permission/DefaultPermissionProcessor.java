package org.source.jpa.permission;

import java.util.Collection;

/**
 * 默认权限处理器
 * <p>
 * 默认实现，直接抛出异常，提示用户需要自定义实现。
 * 项目中应实现 PermissionProcessor 接口来提供自定义的权限逻辑。
 *
 * @author zengfugen
 */
public class DefaultPermissionProcessor implements PermissionProcessor {

    /**
     * 获取用户有权限的 spaceIds
     * <p>
     * 默认实现抛出异常，需要用户自定义实现
     *
     * @param userId 用户ID
     * @return 不返回，直接抛出异常
     * @throws UnsupportedOperationException 提示用户需要实现此方法
     */
    @Override
    public Collection<String> getSpaceIdsByUserId(String userId) {
        throw new UnsupportedOperationException("you must implement this method: getSpaceIdsByUserId");
    }
}
