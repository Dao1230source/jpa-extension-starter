package org.source.jpa.repository.extension;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Extensible {
    boolean insertable() default true;

    boolean updatable() default true;
}
