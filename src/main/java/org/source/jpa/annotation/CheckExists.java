package org.source.jpa.annotation;

import org.source.jpa.enums.OperateEnum;

import java.lang.annotation.*;

/**
 * 检查数据库是否已存在记录
 * <p>
 * 标记在字段上，在执行新增/更新/删除操作前，会检查是否存在重复记录。
 * 如果存在重复记录，将抛出异常。
 *
 * <pre>
 * 示例：
 * &#64;Entity
 * public class User {
 *     &#64;CheckExists(operate = {OperateEnum.ADD, OperateEnum.UPDATE})
 *     &#64;Column(unique = true)
 *     private String username;  // 新增/更新时检查是否已存在相同用户名
 * }
 * </pre>
 *
 * @author zengfugen
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckExists {

    /**
     * 在什么操作中生效
     * <p>
     * 支持以下操作类型：
     * <ul>
     *     <li>ADD - 新增时检查</li>
     *     <li>UPDATE - 更新时检查</li>
     *     <li>DELETE - 删除时检查</li>
     *     <li>ALL - 所有操作都检查（默认）</li>
     * </ul>
     *
     * @return 操作类型数组
     */
    OperateEnum[] operate() default OperateEnum.ALL;
}
