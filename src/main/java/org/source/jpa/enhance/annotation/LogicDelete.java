package org.source.jpa.enhance.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogicDelete {
    /**
     * 删除状态的value
     *
     * @return value
     */
    boolean deletedValue() default true;
}
