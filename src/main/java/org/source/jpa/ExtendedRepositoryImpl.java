package org.source.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.source.jpa.annotation.CheckExists;
import org.source.jpa.annotation.Extensible;
import org.source.jpa.annotation.LogicDelete;
import org.source.jpa.enums.OperateEnum;
import org.source.jpa.exception.JpaExtExceptionEnum;
import org.source.spring.trace.TraceContext;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exception.BaseException;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.PropertyUtil;
import org.source.utility.utils.Streams;
import org.source.utility.utils.Strings;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 扩展的 JPA Repository 实现类
 * <p>
 * 继承 SimpleJpaRepository，实现 ExtendedRepository 接口，提供以下增强功能：
 * <ul>
 *     <li>批量插入/更新：使用 MySQL ON DUPLICATE KEY UPDATE 实现</li>
 *     <li>逻辑删除：将 @LogicDelete 标记的字段设置为删除状态</li>
 *     <li>物理删除：直接从数据库删除记录</li>
 *     <li>自动填充用户ID：createUser、updateUser 字段自动填充</li>
 *     <li>存在性检查：@CheckExists 字段在新增/更新前检查重复</li>
 *     <li>按条件操作：支持 Condition 构造器进行查询和删除</li>
 * </ul>
 *
 * @param <T> 实体类型
 * @param <I> 主键类型
 * @author zengfugen
 */
@Repository
@Slf4j
public class ExtendedRepositoryImpl<T, I> extends SimpleJpaRepository<T, I> implements ExtendedRepository<T, I> {

    /**
     * 表结构详情缓存
     * <p>
     * 用于批量操作时快速生成 SQL
     */
    private static final Map<String, TableDetail> TABLE_DETAILS = new ConcurrentHashMap<>();

    /**
     * 自动填充用户ID的字段名集合
     */
    private static final Set<String> AUTO_SET_USER_ID_FIELD_NAMES = Set.of("createUser", "updateUser");

    @PersistenceContext
    private final EntityManager entityManager;

    private final JpaEntityInformation<T, ?> entityInformation;

    /**
     * 构造函数
     *
     * @param entityInformation 实体信息
     * @param entityManager     EntityManager 实例
     */
    public ExtendedRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
                                  EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    /**
     * 批量插入/更新
     * <p>
     * 使用 MySQL ON DUPLICATE KEY UPDATE 语法实现高性能批量操作
     *
     * @param ts 实体集合
     * @return 影响的行数
     */
    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int onDuplicateUpdateBatch(Collection<T> ts) {
        if (CollectionUtils.isEmpty(ts)) {
            return 0;
        }
        ts.forEach(this::autoSetUserId);
        Class<T> entityClass = entityInformation.getJavaType();
        String tableName = this.getTableName(entityClass);
        TableDetail tableDetail = TABLE_DETAILS.computeIfAbsent(entityClass.getName(),
                n -> getTableDetail(entityClass, tableName));
        String sql = tableDetail.getTableSqlAssembler().assemble(ts.size());
        Query nativeQuery = entityManager.createNativeQuery(sql);
        int idx = 1;
        for (T t : ts) {
            List<ColumnField> columns = tableDetail.getActualColumnFields();
            for (ColumnField columnField : columns) {
                Object value = getColumnValue(t, columnField);
                nativeQuery.setParameter(idx, value);
                idx += 1;
            }
        }
        return nativeQuery.executeUpdate();
    }

    /**
     * 获取实体字段的值
     * <p>
     * 支持嵌套对象（@Embedded），通过 getter 方法获取值
     *
     * @param entity      实体对象
     * @param columnField 字段信息
     * @return 字段值
     */
    public @Nullable Object getColumnValue(Object entity, ColumnField columnField) {
        Object obj = entity;
        if (!CollectionUtils.isEmpty(columnField.embeddedFieldNames)) {
            for (String embeddedFieldName : columnField.embeddedFieldNames) {
                if (Objects.isNull(obj)) {
                    return null;
                }
                obj = PropertyUtil.getProperty(obj, embeddedFieldName);
            }
        }
        if (Objects.nonNull(obj)) {
            return PropertyUtil.getProperty(obj, columnField.getFieldName());
        }
        return null;
    }

    @Data
    static class TableDetail {
        private String name;
        private List<ColumnField> columnFields;
        private List<ColumnField> actualColumnFields;
        private TableSqlAssembler tableSqlAssembler;

        public TableDetail(String name, List<ColumnField> columnFields) {
            this.name = name;
            this.columnFields = columnFields;
            this.actualColumnFields = new ArrayList<>(columnFields.size());
            StringBuilder columnSql = new StringBuilder();
            StringBuilder placeholderSql = new StringBuilder();
            StringBuilder updateColumnSql = new StringBuilder();
            for (int i = 0; i < this.columnFields.size(); i++) {
                ColumnField columnField = this.columnFields.get(i);
                String columnName = columnField.columnName;
                if (i > 0) {
                    if (columnField.extensibleValue.insertable) {
                        columnSql.append(", ");
                        placeholderSql.append(", ");
                    }
                    if (columnField.extensibleValue.updatable) {
                        updateColumnSql.append(", ");
                    }
                }
                if (columnField.extensibleValue.insertable) {
                    columnSql.append(columnName);
                    placeholderSql.append("?");
                    actualColumnFields.add(columnField);
                }
                if (columnField.extensibleValue.updatable) {
                    updateColumnSql.append(columnName).append("=values(").append(columnName).append(")");
                }
            }
            removePrefix(columnSql);
            removePrefix(placeholderSql);
            removePrefix(updateColumnSql);
            tableSqlAssembler = new TableSqlAssembler(name, columnSql, placeholderSql, updateColumnSql);
        }


        private void removePrefix(StringBuilder stringBuilder) {
            if (Constants.COMMA.equals(String.valueOf(stringBuilder.charAt(0)))) {
                stringBuilder.delete(0, 1);
            }
        }

    }

    @AllArgsConstructor
    @Getter
    public static class TableSqlAssembler {
        private final String tableName;
        private final StringBuilder columnSql;
        private final StringBuilder placeholderSql;
        private final StringBuilder updateColumnSql;

        public String assemble(int rowSize) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ").append(tableName)
                    // 字段列表
                    .append(" (").append(columnSql)
                    // 更新值列表
                    .append(") VALUES (").append(placeholderSql);
            if (rowSize > 1) {
                for (int i = 1; i < rowSize; i++) {
                    sql.append("), (").append(placeholderSql);
                }
            }
            // 主键冲突后的更新操作
            sql.append(") ON DUPLICATE KEY UPDATE ").append(updateColumnSql);
            return sql.toString();
        }
    }


    public String getTableName(Class<T> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        return table.name();
    }

    private TableDetail getTableDetail(Class<T> entityClass, String tableName) {
        List<ColumnField> columnFields = getColumnFields(entityClass, List.of());
        return new TableDetail(tableName, columnFields);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ColumnField {
        private String columnName;
        private String fieldName;
        private ExtensibleValue extensibleValue;
        private List<String> embeddedFieldNames;
    }


    static List<ColumnField> getColumnFields(Class<?> entityClass, List<String> embeddedFieldNames) {
        List<ColumnField> columnFieldList = new ArrayList<>();
        Arrays.stream(entityClass.getDeclaredFields()).forEach(f -> {
            ReflectionUtils.makeAccessible(f);
            Column column = f.getAnnotation(Column.class);
            String columnName = column != null ? column.name() : Strings.humpToUnderline(f.getName());
            ExtensibleValue extensibleValue = ExtensibleValue.of(columnName, f.getAnnotation(Extensible.class));
            if (f.isAnnotationPresent(Column.class) || f.isAnnotationPresent(Id.class)) {
                columnFieldList.add(new ColumnField(columnName, f.getName(), extensibleValue, embeddedFieldNames));
            } else if (f.isAnnotationPresent(Embedded.class)) {
                Set<AttrOverride> attrOverrides = obtainAttrOverrides(f);
                Class<?> embeddableCls = f.getType();
                attrOverrides.addAll(obtainAttrOverrides(embeddableCls));
                Map<String, String> nameColumnMap = Streams.toMap(attrOverrides, AttrOverride::getName, AttrOverride::getColumn);
                List<String> embeddedFieldNameList;
                if (CollectionUtils.isEmpty(embeddedFieldNames)) {
                    embeddedFieldNameList = new ArrayList<>(1);
                } else {
                    embeddedFieldNameList = new ArrayList<>(embeddedFieldNames);
                }
                embeddedFieldNameList.add(f.getName());
                List<ColumnField> columnFields = getColumnFields(embeddableCls, embeddedFieldNameList);
                columnFields.forEach(c -> {
                    String overrideColumnName = nameColumnMap.get(c.fieldName);
                    if (StringUtils.hasLength(overrideColumnName)) {
                        c.setColumnName(overrideColumnName);
                    }
                });
                columnFieldList.addAll(columnFields);
            } else {
                columnFieldList.add(new ColumnField(columnName, f.getName(), extensibleValue, embeddedFieldNames));
            }
        });
        return columnFieldList;
    }

    static Set<AttrOverride> obtainAttrOverrides(AnnotatedElement annotatedElement) {
        Set<AttrOverride> attrOverrides = new HashSet<>();
        AttributeOverrides attributeOverrides = annotatedElement.getAnnotation(AttributeOverrides.class);
        if (Objects.nonNull(attributeOverrides)) {
            attrOverrides.addAll(Arrays.stream(attributeOverrides.value()).map(AttrOverride::new).collect(Collectors.toSet()));
        } else {
            attrOverrides.addAll(Arrays.stream(annotatedElement.getAnnotationsByType(AttributeOverride.class)).map(AttrOverride::new).collect(Collectors.toSet()));
        }
        return attrOverrides;
    }

    @Data
    static class AttrOverride {
        private String name;
        private String column;

        public AttrOverride(AttributeOverride attributeOverride) {
            this.name = attributeOverride.name();
            this.column = attributeOverride.column().name();
        }
    }

    @AllArgsConstructor
    @Data
    static class ExtensibleValue {
        private final boolean insertable;
        private final boolean updatable;

        public ExtensibleValue(Extensible extensible) {
            this.insertable = extensible.insertable();
            this.updatable = extensible.updatable();
        }

        public static ExtensibleValue of(String columnName, @Nullable Extensible extensible) {
            if (Objects.nonNull(extensible)) {
                return new ExtensibleValue(extensible);
            }
            return switch (columnName) {
                case Constants.COLUMN_ID, Constants.COLUMN_CREATE_TIME, Constants.COLUMN_UPDATE_TIME ->
                        new ExtensibleValue(false, false);
                case Constants.COLUMN_CREATE_USER -> new ExtensibleValue(true, false);
                default -> new ExtensibleValue(true, true);
            };
        }
    }


    /**
     * 新增、批量保存
     */
    @Override
    public <S extends T> S save(S s) {
        throwIfPresent(s, OperateEnum.ADD);
        this.autoSetUserId(s);
        return super.save(s);
    }

    /**
     * 逻辑删除
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(I id) {
        findById(id).ifPresent(fromDb -> {
            T deleted = this.logicDelete(fromDb);
            this.save(deleted);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(T t) {
        T toQuery = checkExistsQuery(t, OperateEnum.DELETE);
        super.findOne(Example.of(toQuery)).ifPresent(fromDb -> {
            T deleted = this.logicDelete(fromDb);
            this.save(deleted);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Condition<T> condition) {
        List<T> entities = findAll(condition);
        entities.forEach(this::logicDelete);
        this.saveAll(entities);
    }

    /**
     * remove 物理删除
     */
    @Override
    public void removeById(I id) {
        super.deleteById(id);
    }

    @Override
    public void remove(T t) {
        super.delete(t);
    }

    @Override
    public void remove(Condition<T> condition) {
        this.delete(condition.getSpecification());
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
    @Transactional
    @Override
    public T update(T t, BinaryOperator<T> dbEntityUpdate) {
        T fromDb = throwIfAbsent(t, OperateEnum.UPDATE);
        T entityUpdated = dbEntityUpdate.apply(fromDb, t);
        this.autoSetUserId(entityUpdated);
        return this.save(entityUpdated);
    }

    @Transactional
    @Override
    public T update(T t) {
        return update(t, this::updateNotNullValues);
    }

    /**
     * get 查询单条数据，没查询到数据报错
     */
    @Override
    public T get(I id, Supplier<BaseException> exceptionSupplier) {
        return super.findById(id).orElseThrow(exceptionSupplier);
    }

    @Override
    public T get(Condition<T> condition) {
        return super.findOne(condition.getSpecification()).orElseThrow(BaseExceptionEnum.RECORD_NOT_FOUND::newException);
    }

    /**
     * findAll 查询全部符合条件的数据
     */
    @Override
    public List<T> findAll(Condition<T> condition) {
        return super.findAll(condition.getSpecification());
    }

    /**
     * 数据记录如果存在则抛出异常
     *
     * @param t       入参
     * @param operate operate
     */
    public void throwIfPresent(T t, OperateEnum operate) {
        T query = checkExistsQuery(t, operate);
        Optional<T> fromDbOptional = super.findOne(Example.of(query));
        BaseExceptionEnum.RECORD_HAS_EXISTS.isTrue(fromDbOptional.isEmpty(), Jsons.str(query));
    }

    /**
     * 数据记录如果不存在则抛出异常
     *
     * @param t       入参
     * @param operate operate
     * @return 数据库记录
     */
    public T throwIfAbsent(T t, OperateEnum operate) {
        T query = checkExistsQuery(t, operate);
        return super.findOne(Example.of(query)).orElseThrow(() -> BaseExceptionEnum.RECORD_NOT_FOUND.newException(Jsons.str(query)));
    }

    /**
     * 执行新增、修改、删除操作时，组装检查数据库是否已存在重复记录的查询数据
     * 要求jpa相关的实体类必须有无参构造器
     *
     * @param t       目标数据
     * @param operate 操作
     * @return 返回用来查询数据库是否存在记录的数据
     */
    public T checkExistsQuery(T t, OperateEnum operate) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) t.getClass();
            Constructor<T> constructor = ReflectionUtils.accessibleConstructor(clazz);
            T newed = constructor.newInstance();
            AtomicBoolean hasCheckExists = new AtomicBoolean(false);
            ReflectionUtils.doWithFields(clazz, f -> {
                Object v = PropertyUtil.getProperty(t, f);
                if (Objects.nonNull(v)) {
                    hasCheckExists.set(true);
                    PropertyUtil.setProperty(newed, f, v);
                }
            }, f -> {
                CheckExists checkExists = f.getAnnotation(CheckExists.class);
                return Objects.nonNull(checkExists)
                        && Streams.of(checkExists.operate()).anyMatch(o -> OperateEnum.ALL.equals(o) || operate.equals(o));
            });
            return hasCheckExists.get() ? newed : t;
        } catch (NoSuchMethodException e) {
            throw JpaExtExceptionEnum.OBJECT_MUST_HAVE_NO_ARGS_CONSTRUCTOR.newException();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw JpaExtExceptionEnum.OBJECT_NEW_INSTANCE_ERROR.newException();
        }
    }

    /**
     * 逻辑删除
     * <p>
     * 将标记为 @LogicDelete 的字段设置为删除状态值
     *
     * @param t 实体对象
     * @return 处理后的实体对象
     */
    public T logicDelete(T t) {
        Class<?> clazz = t.getClass();
        ReflectionUtils.doWithFields(clazz, f -> {
            LogicDelete logicDelete = f.getAnnotation(LogicDelete.class);
            PropertyUtil.setProperty(t, f, logicDelete.deletedValue());
        }, f -> f.isAnnotationPresent(LogicDelete.class));
        return t;
    }

    /**
     * 更新非空字段值
     * <p>
     * 将输入对象中非空的字段值合并到数据库对象
     *
     * @param fromDb 数据库查询的对象
     * @param t      输入的对象
     * @return 合并后的对象
     */
    public T updateNotNullValues(T fromDb, T t) {
        Class<?> clazz = t.getClass();
        ReflectionUtils.doWithFields(clazz, f -> {
            Object tValue = PropertyUtil.getProperty(t, f);
            if (Objects.nonNull(tValue)) {
                PropertyUtil.setProperty(fromDb, f, tValue);
            }
        });
        return fromDb;
    }

    /**
     * 自动填充用户ID
     * <p>
     * 将 createUser、updateUser 字段设置为当前用户ID
     *
     * @param t 实体对象
     */
    public void autoSetUserId(T t) {
        Class<?> clazz = t.getClass();
        ReflectionUtils.doWithFields(clazz,
                f -> PropertyUtil.setProperty(t, f, TraceContext.getUserId()),
                f -> AUTO_SET_USER_ID_FIELD_NAMES.contains(f.getName()));
    }

}
