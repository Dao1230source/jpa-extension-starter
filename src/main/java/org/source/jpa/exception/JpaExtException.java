package org.source.jpa.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.source.utility.exception.BaseException;
import org.source.utility.exception.EnumProcessor;
import org.springframework.lang.Nullable;

/**
 * spring extension exception
 */
@EqualsAndHashCode(callSuper = true)
@Getter
public class JpaExtException extends BaseException {

    public JpaExtException(EnumProcessor<?> content, @Nullable Throwable cause, @Nullable String extraMessage, @Nullable Object... objects) {
        super(content, cause, extraMessage, objects);
    }

    public JpaExtException(EnumProcessor<?> content, String extraMessage, Object... objects) {
        super(content, extraMessage, objects);
    }

    public JpaExtException(EnumProcessor<?> content, Throwable e) {
        super(content, e);
    }

    public JpaExtException(EnumProcessor<?> content) {
        super(content);
    }

}
