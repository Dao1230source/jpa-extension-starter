package org.source.jpa;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.source.jpa.enums.ExpressionEnum;
import org.source.utility.function.SFunction;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Objects;

/**
 * 链式查询条件构造器
 * <p>
 * 用于构建动态查询条件，支持以下操作：
 * <ul>
 *     <li>eq - 等于条件</li>
 *     <li>in - IN 条件</li>
 *     <li>eqIfPresent - 条件性等于（值不为空时添加）</li>
 *     <li>inIfPresent - 条件性 IN（集合不为空时添加）</li>
 *     <li>and/or/not - 逻辑组合</li>
 * </ul>
 *
 * <pre>
 * 示例：
 * Condition&lt;User&gt; condition = new Condition&lt;User&gt;()
 *     .eqIfPresent(User::getUsername, username)
 *     .inIfPresent(User::getDeptId, deptIds);
 * List&lt;User&gt; users = repository.findAll(condition);
 * </pre>
 *
 * @param <T> 实体类型
 * @author zengfugen
 */
@Getter
@Slf4j
public class Condition<T> {

    /**
     * 构建后的 Specification 查询条件
     */
    private Specification<T> specification;

    /**
     * AND 逻辑组合
     *
     * @param express 待组合的表达式
     * @return this（支持链式调用）
     */
    public Condition<T> and(Specification<T> express) {
        if (notFirst(express)) {
            log.debug("sql logic: and");
            this.specification = this.specification.and(express);
        }
        return this;
    }

    /**
     * OR 逻辑组合
     *
     * @param express 待组合的表达式
     * @return this（支持链式调用）
     */
    public Condition<T> or(Specification<T> express) {
        if (notFirst(express)) {
            log.debug("sql logic: or");
            this.specification = this.specification.or(express);
        }
        return this;
    }

    /**
     * 判断是否是第一个条件
     * <p>
     * 第一个条件直接设置，后续条件需要用 and/or 组合
     *
     * @param express 表达式
     * @return false 表示是第一个条件或表达式为空
     */
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

    /**
     * 等于条件
     * <p>
     * SQL: field = value
     *
     * @param field 字段名（Lambda 表达式）
     * @param value 值
     * @return this（支持链式调用）
     */
    public <V> Condition<T> eq(SFunction<T, V> field, V value) {
        return this.and(ExpressionEnum.EQ.express(field, value));
    }

    /**
     * IN 条件
     * <p>
     * SQL: field IN (values)
     *
     * @param field  字段名（Lambda 表达式）
     * @param values 值集合
     * @return this（支持链式调用）
     */
    public <V> Condition<T> in(SFunction<T, V> field, Collection<V> values) {
        return this.and(ExpressionEnum.IN.express(field, values));
    }

    /**
     * 条件性等于
     * <p>
     * 当值不为 null 时添加等于条件，否则跳过
     *
     * @param field 字段名（Lambda 表达式）
     * @param value 值（可能为 null）
     * @return this（支持链式调用）
     */
    public <V> Condition<T> eqIfPresent(SFunction<T, V> field, V value) {
        if (Objects.isNull(value)) {
            return this;
        }
        return this.eq(field, value);
    }

    /**
     * 条件性 IN
     * <p>
     * 当集合不为空时添加 IN 条件，否则跳过
     *
     * @param field  字段名（Lambda 表达式）
     * @param values 值集合（可能为空）
     * @return this（支持链式调用）
     */
    public <V> Condition<T> inIfPresent(SFunction<T, V> field, Collection<V> values) {
        if (CollectionUtils.isEmpty(values)) {
            return this;
        }
        return this.in(field, values);
    }

    /**
     * OR 逻辑组合另一个 Condition
     *
     * @param condition 待组合的条件
     * @return this（支持链式调用）
     */
    public Condition<T> or(Condition<T> condition) {
        return this.or(condition.specification);
    }

    /**
     * NOT 逻辑
     * <p>
     * SQL: NOT (condition)
     *
     * @param condition 待否定的条件
     * @return this（支持链式调用）
     */
    public Condition<T> not(Condition<T> condition) {
        if (Objects.isNull(condition.specification)) {
            return this;
        }
        log.debug("sql logic: not");
        return this.and(Specification.not(condition.specification));
    }

}
