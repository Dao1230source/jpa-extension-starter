package org.source.jpa.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.utility.exception.EnumProcessor;

@Getter
@AllArgsConstructor
public enum JpaExtExceptionEnum implements EnumProcessor<JpaExtException> {

    OBJECT_MUST_HAVE_NO_ARGS_CONSTRUCTOR("object must have no args constructor"),
    OBJECT_NEW_INSTANCE_ERROR("object new instance error"),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }

}
