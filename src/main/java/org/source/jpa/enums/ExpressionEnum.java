package org.source.jpa.enums;

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

/**
 * SQL 表达式枚举
 * <p>
 * 用于构建 JPA Specification 查询条件，支持以下表达式：
 * <ul>
 *     <li>EQ - 等于 (=)</li>
 *     <li>IN - IN 查询</li>
 * </ul>
 *
 * @author zengfugen
 */
public enum ExpressionEnum {

    /**
     * 等于表达式
     * <p>
     * SQL: field = value
     */
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

    /**
     * IN 表达式
     * <p>
     * SQL: field IN (values)
     */
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

    /**
     * 根据字段名生成 Specification 构建函数
     *
     * @param fieldName 字段名
     * @return Specification 构建函数
     */
    public abstract <T> Function<Object, Specification<T>> express(String fieldName);

    /**
     * 从 Lambda 表达式获取字段名
     *
     * @param field Lambda 字段表达式
     * @return 字段名
     */
    public <T, V> String obtainColumnName(SFunction<T, V> field) {
        String fieldName = Lambdas.getFieldName(field);
        BaseExceptionEnum.NOT_NULL.notEmpty(fieldName, "未能正确的获取到字段名称");
        return fieldName;
    }

    /**
     * 获取参数值
     * <p>
     * 确保参数有且只有一个非空值
     *
     * @param values 参数数组
     * @return 参数值
     */
    public Object obtainValue(Object... values) {
        if (Objects.nonNull(values) && values.length == 1 && Objects.nonNull(values[0])) {
            return values[0];
        }
        throw BaseExceptionEnum.NOT_NULL.newException("参数有且只能有一个");
    }

    /**
     * 获取 SQL 符号
     *
     * @return SQL 符号（如 =, in）
     */
    public abstract String sqlSymbol();

    /**
     * 生成 Specification 查询条件
     * <p>
     * 使用 Lambda 表达式指定字段，自动解析字段名并构建查询条件
     *
     * @param field  Lambda 字段表达式（如 User::getUsername）
     * @param values 值或值集合
     * @return Specification 查询条件
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
