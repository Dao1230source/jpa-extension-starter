package org.source.jpa.annotation;

import java.lang.annotation.*;

/**
 * 标记逻辑删除字段
 * <p>
 * 标记在字段上，调用 deleteById() 等删除方法时，不会真正删除记录，
 * 而是将该字段设置为删除状态值。
 *
 * <pre>
 * 示例：
 * &#64;Entity
 * public class User {
 *     &#64;LogicDelete(deletedValue = true)
 *     private Boolean deleted;  // 删除时自动设为 true
 *
 *     &#64;LogicDelete(deletedValue = 1)
 *     private Integer status;  // 删除时自动设为 1
 * }
 *
 * // 调用删除方法
 * repository.deleteById(1L);  // 实际执行：UPDATE user SET deleted = true WHERE id = 1
 * </pre>
 *
 * @author zengfugen
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogicDelete {

    /**
     * 删除状态的值
     * <p>
     * 调用删除方法时，字段会被设置为此值
     *
     * @return 默认 true
     */
    boolean deletedValue() default true;
}
