package org.source.jpa.enhance.annotation;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Filters {

    String[] filter() default {};
}
