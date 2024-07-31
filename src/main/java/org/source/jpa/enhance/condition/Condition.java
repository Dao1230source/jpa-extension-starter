package org.source.jpa.enhance.condition;

import org.source.jpa.enhance.enums.ExpressionEnum;
import org.source.utility.function.SFunction;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Objects;

/**
 * 为了方法执行时提供报错信息，暂时保存输入数据
 *
 * @param <T>
 * @param <V>
 */

public record Condition<T, V>(ExpressionEnum expressionEnum, SFunction<T, V> field, Object... values) {

    public Specification<T> express() {
        return this.expressionEnum.express(this.field, this.values);
    }

    public String obtainFieldName() {
        return ExpressionEnum.obtainFieldName(this.field);
    }

    public Object obtainValue() {
        return this.expressionEnum.obtainValue(this.values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Condition<?, ?> condition = (Condition<?, ?>) o;
        return Objects.deepEquals(values, condition.values) && Objects.equals(field, condition.field) && expressionEnum == condition.expressionEnum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expressionEnum, field, Arrays.hashCode(values));
    }

    @Override
    public String toString() {
        return "Condition{" +
                "expressionEnum=" + expressionEnum +
                ", field=" + field +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}
