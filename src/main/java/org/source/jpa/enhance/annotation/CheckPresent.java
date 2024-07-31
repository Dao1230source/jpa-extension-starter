package org.source.jpa.enhance.annotation;

import org.source.jpa.enhance.enums.OperateEnum;

import java.lang.annotation.*;

/**
 * 检查数据库是否已经存在记录
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPresent {

    /**
     * 在什么操作中生效
     *
     * @return OperateEnum
     */
    OperateEnum[] operate() default OperateEnum.ALL;
}
