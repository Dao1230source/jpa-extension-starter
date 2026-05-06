package org.source.jpa.annotation;

import java.lang.annotation.*;

/**
 * 控制字段在批量操作中的行为
 * <p>
 * 用于 ON DUPLICATE KEY UPDATE 批量操作时，控制字段是否参与插入和更新。
 *
 * <pre>
 * 示例：
 * &#64;Entity
 * public class User {
 *     &#64;Extensible(insertable = true, updatable = false)
 *     private Long createUser;  // 插入时设置，更新时不改变
 *
 *     &#64;Extensible(insertable = false, updatable = false)
 *     private LocalDateTime createTime;  // 数据库自动管理，不参与批量操作
 * }
 * </pre>
 *
 * <p>默认规则（无 &#64;Extensible 时）：</p>
 * <ul>
 *     <li>id 字段：不插入、不更新</li>
 *     <li>createUser 字段：插入、不更新</li>
 *     <li>createTime / updateTime 字段：不插入、不更新</li>
 *     <li>其他字段：插入、更新都参与</li>
 * </ul>
 *
 * @author zengfugen
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Extensible {

    /**
     * 是否参与插入
     * <p>
     * 为 true 时，批量插入会包含该字段的值
     *
     * @return 默认 true
     */
    boolean insertable() default true;

    /**
     * 是否参与更新
     * <p>
     * 为 true 时，ON DUPLICATE KEY UPDATE 会更新该字段
     *
     * @return 默认 true
     */
    boolean updatable() default true;
}
