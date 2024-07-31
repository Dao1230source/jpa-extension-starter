package org.source.jpa.enhance;

import lombok.Getter;
import org.source.jpa.enhance.annotation.CheckPresent;
import org.source.jpa.enhance.annotation.LogicDelete;
import org.source.jpa.enhance.condition.Conditions;
import org.source.jpa.enhance.enums.ExpressionEnum;
import org.source.jpa.enhance.enums.OperateEnum;
import org.source.jpa.repository.UnifiedJpaRepository;
import org.source.spring.exception.BizExceptionEnum;
import org.source.spring.trace.TraceContext;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exceptions.EnumProcessor;
import org.source.utility.function.SFunction;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Reflects;
import org.source.utility.utils.Streams;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * @author zengfugen
 */
@Getter
@Transactional(readOnly = true)
public abstract class AbstractJpaHelper<T, ID> {
    protected final UnifiedJpaRepository<T, ID> repository;
    protected List<String> fieldNamesCanAutoSetUserId;

    protected AbstractJpaHelper(UnifiedJpaRepository<T, ID> repository) {
        this.repository = repository;
        this.fieldNamesCanAutoSetUserId = new ArrayList<>(List.of("createUser", "updateUser"));
    }

    @Transactional(rollbackFor = Exception.class)
    public T add(T t) {
        throwIfPresent(t, OperateEnum.ADD);
        this.autoSetUserId(t);
        return repository.save(t);
    }

    @Transactional(rollbackFor = Exception.class)
    public int saveAll(Collection<T> ts) {
        ts.forEach(this::autoSetUserId);
        return repository.onDuplicateUpdateBatch(ts);
    }

    @Transactional(rollbackFor = Exception.class)
    public T delete(T t) {
        T fromDb = throwIfAbsent(t, OperateEnum.DELETE);
        T deleted = this.logicDelete(fromDb);
        return repository.save(deleted);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(T t) {
        repository.delete(t);
    }


    /**
     * 更新数据记录
     *
     * @param t              输入的entity
     * @param dbEntityUpdate （T） -> T
     *                       <br>
     *                       (数据库查询出来的entity) -> 更新后的entity
     * @return 更新后的entity
     */
    @Transactional(rollbackFor = Exception.class)
    public T update(T t, BinaryOperator<T> dbEntityUpdate) {
        T fromDb = throwIfAbsent(t, OperateEnum.UPDATE);
        T entityUpdated = dbEntityUpdate.apply(fromDb, t);
        return this.repository.save(entityUpdated);
    }

    @Transactional(rollbackFor = Exception.class)
    public T update(T t) {
        this.autoSetUserId(t);
        T fromDb = throwIfAbsent(t, OperateEnum.UPDATE);
        T entityUpdated = this.updateNotNullValues(fromDb, t);
        return this.repository.save(entityUpdated);
    }


    public Optional<T> findOne(T t) {
        return repository.findOne(Example.of(t));
    }

    public Optional<T> findOne(Specification<T> specification) {
        return repository.findOne(specification);
    }

    public Optional<T> findOne(SFunction<T, Object> field, Object value) {
        return repository.findOne(ExpressionEnum.IN.express(field, value));
    }

    public T getOne(Specification<T> specification) {
        return repository.findOne(specification).orElseThrow(BaseExceptionEnum.RECORD_NOT_FOUND::except);
    }

    /**
     * 不能按照 sonar 提示改为 final 方法，会导致spring代理无法代理
     *
     * @param exception  EnumProcessor
     * @param conditions conditions
     * @return T
     */
    public T getOne(Conditions<T> conditions, EnumProcessor<?> exception) {
        String notFoundMessage = conditions.toPlainString();
        return repository.findOne(conditions.and()).orElseThrow(() -> exception.except(notFoundMessage));
    }

    public T getOne(Conditions<T> conditions) {
        return getOne(conditions, BaseExceptionEnum.RECORD_NOT_FOUND);
    }

    public <V> T getOne(SFunction<T, V> field, V value, EnumProcessor<?> exception) {
        return getOne(Conditions.<T>build().add(ExpressionEnum.EQ.of(field, value)), exception);
    }

    public <V> T getOne(SFunction<T, V> field, V value) {
        return getOne(Conditions.<T>build().add(ExpressionEnum.EQ.of(field, value)), BaseExceptionEnum.RECORD_NOT_FOUND);
    }

    public T getOne(T t) {
        return getOne(t, BaseExceptionEnum.RECORD_NOT_FOUND);
    }

    public T getOne(T t, String notFoundMessage) {
        return getOne(t, BaseExceptionEnum.RECORD_NOT_FOUND, notFoundMessage);
    }

    public T getOne(T t, EnumProcessor<?> exception) {
        return getOne(t, exception, null);
    }

    public T getOne(T t, EnumProcessor<?> exception, String notFoundMessage) {
        return findOne(t).orElseThrow(() -> exception.except(notFoundMessage));
    }

    public List<T> findAll() {
        return repository.findAll();
    }

    public List<T> findAll(T t) {
        return repository.findAll(Example.of(t));
    }

    public List<T> findAll(Specification<T> specification) {
        return repository.findAll(specification);
    }

    public List<T> findAll(Conditions<T> conditions) {
        return repository.findAll(conditions.and());
    }

    public void throwIfPresent(T t, OperateEnum operate) {
        T t1 = presentCheck(t, operate);
        Optional<T> fromDbOptional = findOne(t1);
        BaseExceptionEnum.RECORD_HAS_EXISTS.isTrue(fromDbOptional.isEmpty(), Jsons.str(t1));
    }

    public T throwIfAbsent(T t, OperateEnum operate) {
        T t1 = presentCheck(t, operate);
        return getOne(t1, Jsons.str(t1));
    }


    protected T presentCheck(T t, OperateEnum operate) {
        Objects.requireNonNull(operate, "operate must not null");
        @SuppressWarnings("unchecked")
        T o = (T) Reflects.newInstance(t.getClass());
        // 保留用于校验记录是否存在的字段
        Field[] presentFields = Reflects.getFieldsByAnnotation(t.getClass(), CheckPresent.class, a -> {
                    Set<OperateEnum> operations = Arrays.stream(a.operate()).collect(Collectors.toSet());
                    return operations.contains(OperateEnum.ALL) || operations.contains(operate);
                }
        );
        if (presentFields.length == 0) {
            return t;
        }
        long count = Streams.of(presentFields).filter(f -> {
            Object fieldValue = Reflects.getFieldValue(t, f);
            if (Objects.nonNull(fieldValue)) {
                Reflects.setFieldValue(o, f.getName(), fieldValue);
                return true;
            }
            return false;
        }).count();
        return count > 0 ? o : t;
    }

    protected T logicDelete(T t) {
        Field field = Streams.of(t.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(LogicDelete.class)).findFirst()
                .orElseThrow(() -> BizExceptionEnum.NOT_EXISTS_DELETE_COLUMN.except(t.getClass().getSimpleName()));
        LogicDelete logicDelete = field.getAnnotation(LogicDelete.class);
        Reflects.setFieldValue(t, field, logicDelete.deletedValue());
        return t;
    }

    protected T updateNotNullValues(T fromDb, T t) {
        Streams.of(t.getClass().getDeclaredFields()).forEach(f -> {
            Object fieldValue = Reflects.getFieldValue(t, f);
            if (Objects.nonNull(fieldValue)) {
                Reflects.setFieldValue(fromDb, f.getName(), fieldValue);
            }
        });
        return fromDb;
    }

    protected void autoSetUserId(T t) {
        if (!CollectionUtils.isEmpty(this.fieldNamesCanAutoSetUserId)) {
            this.fieldNamesCanAutoSetUserId.forEach(k -> {
                Field field = Reflects.getFieldByName(t, k);
                if (Objects.nonNull(field) && Objects.isNull(Reflects.getFieldValue(t, field))) {
                    Reflects.setFieldValue(t, field, TraceContext.getUserId());
                }
            });
        }
    }

}
