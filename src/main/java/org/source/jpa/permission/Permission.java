package org.source.jpa.permission;

import java.lang.annotation.*;

/**
 * 数据权限控制注解
 * <p>
 * 标记在方法上，用于启用或禁用数据权限过滤。
 * 当 verify = true 时，只返回当前用户有权限的数据。
 *
 * <pre>
 * 示例：
 * &#64;Permission(verify = true)
 * public List&lt;User&gt; getVisibleUsers() {
 *     return repository.findAll();  // 只返回当前用户有权限的数据
 * }
 *
 * &#64;Permission(verify = false)
 * public List&lt;User&gt; getAllUsers() {
 *     return repository.findAll();  // 返回全部数据，不做权限过滤
 * }
 * </pre>
 *
 * <p>权限过滤器基于 Hibernate Filter 实现：</p>
 * <ul>
 *     <li>Filter 名称：PERMISSION</li>
 *     <li>默认条件：space_id in :spaceIds</li>
 *     <li>spaceIds 由 PermissionProcessor 接口实现类提供</li>
 * </ul>
 *
 * @author zengfugen
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {

    /**
     * 是否启用权限验证
     * <p>
     * 为 true 时，启用权限过滤，只返回当前用户有权限的数据
     * 为 false 时，禁用权限过滤，返回全部数据
     *
     * @return 默认 true
     */
    boolean verify() default true;
}
