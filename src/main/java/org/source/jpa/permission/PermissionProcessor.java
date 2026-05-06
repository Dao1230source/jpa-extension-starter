package org.source.jpa.permission;

import java.util.Collection;

/**
 * 权限处理器接口
 * <p>
 * 用于获取用户有权限访问的 spaceIds。
 * 实现此接口以自定义权限逻辑。
 *
 * <pre>
 * 示例：
 * &#64;Component
 * public class CustomPermissionProcessor implements PermissionProcessor {
 *     &#64;Override
 *     public Collection&lt;String&gt; getSpaceIdsByUserId(String userId) {
 *         // 根据用户ID查询其有权限的 spaceIds
 *         return spaceService.getSpaceIdsByUserId(userId);
 *     }
 * }
 * </pre>
 *
 * @author zengfugen
 */
public interface PermissionProcessor {

    /**
     * 根据用户ID获取其有权限访问的 spaceIds
     * <p>
     * 返回的 spaceIds 会用于 PERMISSION Filter 的参数
     *
     * @param userId 用户ID
     * @return spaceIds 集合
     */
    Collection<String> getSpaceIdsByUserId(String userId);
}
