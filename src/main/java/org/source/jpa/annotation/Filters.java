package org.source.jpa.annotation;

import java.lang.annotation.*;

/**
 * 启用 Hibernate Filter
 * <p>
 * 标记在方法上，在方法执行期间启用指定的 Hibernate Filter，用于自动过滤数据。
 *
 * <pre>
 * 示例：
 * &#64;Filters(filter = {"NOT_DELETED"})
 * public List&lt;User&gt; getActiveUsers() {
 *     return repository.findAll();  // 只查询未删除的数据
 * }
 *
 * &#64;Filters(filter = {"NOT_DELETED", "USABLE"})
 * public List&lt;User&gt; getAvailableUsers() {
 *     return repository.findAll();  // 组合多个 Filter
 * }
 * </pre>
 *
 * <p>内置 Filter：</p>
 * <ul>
 *     <li>NOT_DELETED - deleted = false</li>
 *     <li>IS_DEFAULTED - defaulted = 1</li>
 *     <li>USABLE - status = 0</li>
 * </ul>
 *
 * @author zengfugen
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Filters {

    /**
     * 要启用的 Filter 名称数组
     *
     * @return Filter 名称列表
     */
    String[] filter() default {};
}
