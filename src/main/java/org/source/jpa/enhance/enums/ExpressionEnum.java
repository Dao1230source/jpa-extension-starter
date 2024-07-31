package org.source.jpa.enhance.enums;

import org.source.jpa.enhance.condition.Condition;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.function.SFunction;
import org.source.utility.utils.Lambdas;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public enum ExpressionEnum {
    EQ {
        @Override
        public <T, V> Function<Object, Specification<T>> express(SFunction<T, V> field) {
            String fieldName = obtainFieldName(field);
            return value -> (root, cq, cb) -> cb.equal(root.get(fieldName), value);
        }
    },
    IN {
        @Override
        public <T, V> Function<Object, Specification<T>> express(SFunction<T, V> field) {
            String fieldName = obtainFieldName(field);
            return value -> (root, cq, cb) -> root.get(fieldName).in((Collection<?>) value);
        }

        @Override
        public Object obtainValue(Object... values) {
            Object value = super.obtainValue(values);
            if (value instanceof Collection<?> collection) {
                return collection;
            }
            return List.of(values);
        }
    };

    public abstract <T, V> Function<Object, Specification<T>> express(SFunction<T, V> field);

    public Object obtainValue(Object... values) {
        if (Objects.nonNull(values) && values.length == 1 && Objects.nonNull(values[0])) {
            return values[0];
        }
        throw BaseExceptionEnum.NOT_NULL.except("参数有且只能有一个");
    }

    /**
     * 生成 Specification
     *
     * @param field  field
     * @param values value / collection
     * @param <T>    T
     * @param <V>    V
     * @return Specification
     */
    public final <T, V> Specification<T> express(SFunction<T, V> field, Object... values) {
        return this.express(field).apply(obtainValue(values));
    }

    public <T, V> Condition<T, V> of(SFunction<T, V> field, Object... values) {
        return new Condition<>(this, field, values);
    }

    public static <T, V> String obtainFieldName(SFunction<T, V> field) {
        String fieldName = Lambdas.getFieldName(field);
        BaseExceptionEnum.NOT_NULL.notEmpty(fieldName, "未能正确的获取到字段名称");
        return fieldName;
    }

}
