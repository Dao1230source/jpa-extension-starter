package org.source.jpa.enhance.condition;

import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.function.SFunction;
import org.source.utility.utils.Streams;
import org.source.utility.utils.Strings;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Conditions<T> {
    private final List<Condition<T, ?>> conditionList;

    public Conditions(List<Condition<T, ?>> conditionList) {
        this.conditionList = conditionList;
    }

    public Specification<T> and() {
        return Specification.allOf(this.conditionList.stream().map(Condition::express).toList());
    }

    public Specification<T> or() {
        return Specification.anyOf(this.conditionList.stream().map(Condition::express).toList());
    }

    public String toPlainString() {
        BaseExceptionEnum.NOT_EMPTY.notEmpty(conditionList);
        return Streams.of(conditionList)
                .map(c -> Strings.format("{}={}", c.obtainFieldName(), c.obtainValue()))
                .collect(Collectors.joining(","));
    }

    public static <T, V> Specification<T> eq(SFunction<T, V> field, V value) {
        return Condition.eq(field, value).express();
    }

    public static <T, V> Specification<T> in(SFunction<T, V> field, Collection<V> values) {
        return Condition.in(field, values).express();
    }

    @SafeVarargs
    public static <T> Conditions<T> of(Condition<T, ?>... conditions) {
        return new Conditions<>(Streams.of(conditions).toList());
    }
}
