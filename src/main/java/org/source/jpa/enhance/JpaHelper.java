package org.source.jpa.enhance;

import org.source.jpa.enhance.annotation.CheckExists;
import org.source.jpa.enhance.annotation.LogicDelete;
import org.source.jpa.enhance.enums.ExpressionEnum;
import org.source.jpa.enhance.enums.OperateEnum;
import org.source.jpa.exception.JpaExtExceptionEnum;
import org.source.jpa.repository.UnifiedJpaRepository;
import org.source.spring.trace.TraceContext;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exception.BaseException;
import org.source.utility.exception.EnumProcessor;
import org.source.utility.function.SFunction;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public interface JpaHelper<T, I> {
    Set<String> AUTO_SET_USER_ID_FIELD_NAMES = Set.of("createUser", "updateUser");

    UnifiedJpaRepository<T, I> getRepository();

    default Set<String> getAutoSetUserIdFieldNames() {
        return AUTO_SET_USER_ID_FIELD_NAMES;
    }

    /*
    新增、批量保存
     */
    @Transactional(rollbackFor = Exception.class)
    default T add(T t) {
        throwIfPresent(t, OperateEnum.ADD);
        this.autoSetUserId(t);
        return this.getRepository().save(t);
    }

    /*
    逻辑删除
     */
    default void deleteById(I id) {
        findById(id).ifPresent(fromDb -> {
            T deleted = this.logicDelete(fromDb);
            this.getRepository().save(deleted);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    default void delete(T t) {
        T query = checkExistsQuery(t, OperateEnum.DELETE);
        find(query).ifPresent(fromDb -> {
            T deleted = this.logicDelete(fromDb);
            this.getRepository().save(deleted);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    default void delete(Condition<T> condition) {
        List<T> entities = findAll(condition);
        entities.forEach(this::logicDelete);
        this.getRepository().saveAll(entities);
    }

    /*
    remove 物理删除
     */
    default void removeById(I id) {
        findById(id).ifPresent(this.getRepository()::delete);
    }

    @Transactional(rollbackFor = Exception.class)
    default void remove(T t) {
        T t1 = checkExistsQuery(t, OperateEnum.DELETE);
        find(t1).ifPresent(this.getRepository()::delete);
    }

    @Transactional(rollbackFor = Exception.class)
    default void remove(Condition<T> condition) {
        this.getRepository().delete(condition.getSpecification());
    }

    /*
    修改
     */

    /**
     * 更新数据记录
     *
     * @param t              输入的entity
     * @param dbEntityUpdate （T,T） -> T (数据库查询出来的entity, 需要更新的entity) -> 更新后的entity
     * @return 更新后的entity
     */
    @Transactional(rollbackFor = Exception.class)
    default T update(T t, BinaryOperator<T> dbEntityUpdate) {
        T fromDb = throwIfAbsent(t, OperateEnum.UPDATE);
        T entityUpdated = dbEntityUpdate.apply(fromDb, t);
        return this.getRepository().save(entityUpdated);
    }

    @Transactional(rollbackFor = Exception.class)
    default T update(T t) {
        this.autoSetUserId(t);
        T fromDb = throwIfAbsent(t, OperateEnum.UPDATE);
        T entityUpdated = this.updateNotNullValues(fromDb, t);
        return this.getRepository().save(entityUpdated);
    }

    /*
    批量保存（批量新增、批量修改）ON DUPLICATE KEY UPDATE 语句
     */
    @Transactional(rollbackFor = Exception.class)
    default int saveAll(Collection<T> ts) {
        ts.forEach(this::autoSetUserId);
        return this.getRepository().onDuplicateUpdateBatch(ts);
    }

    /*
    get 查询单条数据，没查询到数据报错
     */
    default T getById(I id) {
        return findById(id).orElseThrow(BaseExceptionEnum.RECORD_NOT_FOUND::newException);
    }

    default T getById(I id, Supplier<BaseException> exceptionSupplier) {
        return findById(id).orElseThrow(exceptionSupplier);
    }

    /**
     * 查询单条数据，如果不存在则报错
     *
     * @param t                 t
     * @param exceptionSupplier BaseException
     * @return from database
     */
    default T get(T t, Supplier<BaseException> exceptionSupplier) {
        return find(t).orElseThrow(exceptionSupplier);
    }

    default T get(T t) {
        return get(t, BaseExceptionEnum.RECORD_NOT_FOUND::newException);
    }

    default T get(T t, String notFoundMessage) {
        return get(t, () -> BaseExceptionEnum.RECORD_NOT_FOUND.newException(notFoundMessage));
    }

    default T get(T t, EnumProcessor<?> exception) {
        return get(t, exception::newException);
    }

    default T get(T t, EnumProcessor<?> exception, String notFoundMessage) {
        return get(t, () -> exception.newException(notFoundMessage));
    }

    default T get(Specification<T> specification, Supplier<BaseException> exceptionSupplier) {
        return this.getRepository().findOne(specification).orElseThrow(exceptionSupplier);
    }

    default T get(Specification<T> specification) {
        return this.get(specification, BaseExceptionEnum.RECORD_NOT_FOUND::newException);
    }

    default T get(Specification<T> specification, EnumProcessor<?> exception) {
        return this.get(specification, exception::newException);
    }

    default T get(Condition<T> condition) {
        return this.get(condition.getSpecification(), BaseExceptionEnum.RECORD_NOT_FOUND);
    }

    default T get(Condition<T> condition, EnumProcessor<?> exception) {
        return this.get(condition.getSpecification(), exception::newException);
    }

    default T get(SFunction<T, Object> field, Object value, Supplier<BaseException> exceptionSupplier) {
        return this.getRepository().findOne(ExpressionEnum.IN.express(field, value)).orElseThrow(exceptionSupplier);
    }

    default <V> T get(SFunction<T, V> field, V value) {
        return this.get(new Condition<T>().eq(field, value), BaseExceptionEnum.RECORD_NOT_FOUND);
    }

    default <V> T get(SFunction<T, V> field, V value, EnumProcessor<?> exception) {
        return this.get(new Condition<T>().eq(field, value), exception);
    }

    /*
    find 查询单条数据，返回Optional
     */
    default Optional<T> findById(I id) {
        return this.getRepository().findById(id);
    }

    default List<T> findByIds(Collection<I> ids) {
        return this.getRepository().findAllById(ids);
    }

    default Optional<T> find(T t) {
        return this.getRepository().findOne(Example.of(t));
    }

    default Optional<T> find(Specification<T> specification) {
        return this.getRepository().findOne(specification);
    }

    default Optional<T> find(SFunction<T, Object> field, Object value) {
        return this.getRepository().findOne(ExpressionEnum.IN.express(field, value));
    }

    /*
    findAll 查询全部符合条件的数据
     */
    default List<T> findAll() {
        return this.getRepository().findAll();
    }

    default List<T> findAll(T t) {
        return this.getRepository().findAll(Example.of(t));
    }

    default List<T> findAll(Condition<T> condition) {
        return findAll(condition.getSpecification());
    }

    default List<T> findAll(Specification<T> specification) {
        return this.getRepository().findAll(specification);
    }

    /**
     * 数据记录如果存在则抛出异常
     *
     * @param t       入参
     * @param operate operate
     */
    default void throwIfPresent(T t, OperateEnum operate) {
        T query = checkExistsQuery(t, operate);
        Optional<T> fromDbOptional = find(query);
        BaseExceptionEnum.RECORD_HAS_EXISTS.isTrue(fromDbOptional.isEmpty(), Jsons.str(query));
    }

    /**
     * 数据记录如果不存在则抛出异常
     *
     * @param t       入参
     * @param operate operate
     * @return 数据库记录
     */
    default T throwIfAbsent(T t, OperateEnum operate) {
        T query = checkExistsQuery(t, operate);
        return get(query, () -> BaseExceptionEnum.RECORD_NOT_FOUND.newException(Jsons.str(query)));
    }

    /**
     * 执行新增、修改、删除操作时，组装检查数据库是否已存在重复记录的查询数据
     *
     * @param t       目标数据
     * @param operate 操作
     * @return 返回用来查询数据库是否存在记录的数据
     */
    default T checkExistsQuery(T t, OperateEnum operate) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) t.getClass();
            Constructor<T> constructor = ReflectionUtils.accessibleConstructor(clazz);
            T newed = constructor.newInstance(t.getClass());
            AtomicBoolean hasCheckExists = new AtomicBoolean(false);
            ReflectionUtils.doWithFields(clazz, f -> {
                Object v = ReflectionUtils.getField(f, t);
                if (Objects.nonNull(v)) {
                    hasCheckExists.set(true);
                    ReflectionUtils.setField(f, newed, v);
                }
            }, f -> {
                // 保留用于校验记录是否存在的字段
                CheckExists checkExists = f.getAnnotation(CheckExists.class);
                return Streams.of(checkExists.operate()).anyMatch(o -> OperateEnum.ALL.equals(o) || operate.equals(o));
            });
            return hasCheckExists.get() ? newed : t;
        } catch (NoSuchMethodException e) {
            throw JpaExtExceptionEnum.OBJECT_MUST_HAVE_NO_ARGS_CONSTRUCTOR.newException();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw JpaExtExceptionEnum.OBJECT_NEW_INSTANCE_ERROR.newException();
        }
    }

    default T logicDelete(T t) {
        Class<?> clazz = t.getClass();
        ReflectionUtils.doWithFields(clazz, f -> {
            LogicDelete logicDelete = f.getAnnotation(LogicDelete.class);
            ReflectionUtils.setField(f, t, logicDelete.deletedValue());
        }, f -> f.isAnnotationPresent(LogicDelete.class));
        return t;
    }

    default T updateNotNullValues(T fromDb, T t) {
        Class<?> clazz = t.getClass();
        ReflectionUtils.doWithFields(clazz, f -> {
            Object tValue = ReflectionUtils.getField(f, t);
            if (Objects.nonNull(tValue)) {
                ReflectionUtils.setField(f, fromDb, tValue);
            }
        });
        return fromDb;
    }

    default void autoSetUserId(T t) {
        Class<?> clazz = t.getClass();
        ReflectionUtils.doWithFields(clazz,
                f -> ReflectionUtils.setField(f, t, TraceContext.getUserId()),
                f -> this.getAutoSetUserIdFieldNames().contains(f.getName()));
    }

}
