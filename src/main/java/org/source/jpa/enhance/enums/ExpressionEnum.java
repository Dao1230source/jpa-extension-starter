package org.source.jpa.enhance.enums;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.function.SFunction;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Lambdas;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public enum ExpressionEnum {

    EQ {
        @Override
        public <T> Function<Object, Specification<T>> express(String fieldName) {
            return value -> (root, cq, cb) -> cb.equal(root.get(fieldName), value);
        }

        @Override
        public String sqlSymbol() {
            return "=";
        }
    },
    IN {
        @Override
        public <T> Function<Object, Specification<T>> express(String fieldName) {
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

        @Override
        public String sqlSymbol() {
            return "in";
        }
    };

    final Logger log = LoggerFactory.getLogger(ExpressionEnum.class);

    public abstract <T> Function<Object, Specification<T>> express(String fieldName);

    public <T, V> String obtainColumnName(SFunction<T, V> field) {
        String fieldName = Lambdas.getFieldName(field);
        BaseExceptionEnum.NOT_NULL.notEmpty(fieldName, "未能正确的获取到字段名称");
        return fieldName;
    }

    public Object obtainValue(Object... values) {
        if (Objects.nonNull(values) && values.length == 1 && Objects.nonNull(values[0])) {
            return values[0];
        }
        throw BaseExceptionEnum.NOT_NULL.except("参数有且只能有一个");
    }

    public abstract String sqlSymbol();

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
        String columnName = obtainColumnName(field);
        Function<Object, Specification<T>> express = this.express(columnName);
        Object fieldValue = obtainValue(values);
        if (log.isDebugEnabled()) {
            log.debug("sql expression: {} {} {}", columnName, this.sqlSymbol(), Jsons.str(fieldValue));
        }
        return express.apply(fieldValue);
    }

}
