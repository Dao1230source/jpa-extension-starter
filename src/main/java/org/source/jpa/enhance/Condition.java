package org.source.jpa.enhance;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.source.jpa.enhance.enums.ExpressionEnum;
import org.source.utility.function.SFunction;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Objects;

@Getter
@Slf4j
public class Condition<T> {
    private Specification<T> specification;

    public Condition<T> and(Specification<T> express) {
        if (notFirst(express)) {
            log.debug("sql logic: and");
            this.specification = this.specification.and(express);
        }
        return this;
    }

    public Condition<T> or(Specification<T> express) {
        if (notFirst(express)) {
            log.debug("sql logic: or");
            this.specification = this.specification.or(express);
        }
        return this;
    }

    private boolean notFirst(Specification<T> express) {
        if (Objects.isNull(express)) {
            return false;
        }
        if (Objects.isNull(this.specification)) {
            this.specification = express;
            return false;
        }
        return true;
    }

    public <V> Condition<T> eq(SFunction<T, V> field, V value) {
        return this.and(ExpressionEnum.EQ.express(field, value));
    }

    public <V> Condition<T> in(SFunction<T, V> field, Collection<V> values) {
        return this.and(ExpressionEnum.IN.express(field, values));
    }

    public <V> Condition<T> eqIfPresent(SFunction<T, V> field, V value) {
        if (Objects.isNull(value)) {
            return this;
        }
        return this.eq(field, value);
    }

    public <V> Condition<T> inIfPresent(SFunction<T, V> field, Collection<V> values) {
        if (CollectionUtils.isEmpty(values)) {
            return this;
        }
        return this.in(field, values);
    }

    public Condition<T> or(Condition<T> condition) {
        return this.or(condition.specification);
    }

    public Condition<T> not(Condition<T> condition) {
        if (Objects.isNull(condition.specification)) {
            return this;
        }
        log.debug("sql logic: not");
        return this.and(Specification.not(condition.specification));
    }

}
