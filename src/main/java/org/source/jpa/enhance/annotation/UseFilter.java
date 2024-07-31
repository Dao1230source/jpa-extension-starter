package org.source.jpa.enhance.annotation;

import org.source.jpa.enhance.enums.FilterEnum;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface UseFilter {

    FilterEnum[] filter() default {};
}
