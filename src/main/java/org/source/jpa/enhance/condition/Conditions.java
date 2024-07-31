package org.source.jpa.enhance.condition;

import lombok.Getter;
import org.source.jpa.enhance.enums.ExpressionEnum;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.function.SFunction;
import org.source.utility.utils.Streams;
import org.source.utility.utils.Strings;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class Conditions<T> {
    private final Function<Iterable<Specification<T>>, Specification<T>> and = Specification::allOf;
    private final Function<Iterable<Specification<T>>, Specification<T>> or = Specification::anyOf;
    private final List<Condition<T, ?>> conditionList;

    public Conditions() {
        this(new ArrayList<>());
    }

    public Conditions(List<Condition<T, ?>> conditionList) {
        this.conditionList = conditionList;
    }

    public Specification<T> and() {
        return this.and.apply(this.experss());
    }

    public Specification<T> or() {
        return this.or.apply(this.experss());
    }

    public List<Specification<T>> experss() {
        BaseExceptionEnum.NOT_EMPTY.notEmpty(conditionList);
        return Streams.of(conditionList).map(Condition::express).toList();
    }

    public String toPlainString() {
        BaseExceptionEnum.NOT_EMPTY.notEmpty(conditionList);
        return Streams.of(conditionList)
                .map(c -> Strings.format("{}={}", c.obtainFieldName(), c.obtainValue()))
                .collect(Collectors.joining(","));
    }


    public <V> Conditions<T> add(Condition<T, V> condition) {
        this.conditionList.add(condition);
        return this;
    }

    public <V> Conditions<T> addEq(SFunction<T, V> field, V value) {
        this.conditionList.add(ExpressionEnum.EQ.of(field, value));
        return this;
    }

    public <V> Conditions<T> addIn(SFunction<T, V> field, Collection<V> values) {
        this.conditionList.add(ExpressionEnum.IN.of(field, values));
        return this;
    }

    public static <T> Conditions<T> build() {
        return new Conditions<>();
    }

    public static <T, V> Conditions<T> eq(SFunction<T, V> field, V value) {
        return Conditions.<T>build().addEq(field, value);
    }

    public static <T, V> Conditions<T> in(SFunction<T, V> field, Collection<V> values) {
        return Conditions.<T>build().addIn(field, values);
    }
}
