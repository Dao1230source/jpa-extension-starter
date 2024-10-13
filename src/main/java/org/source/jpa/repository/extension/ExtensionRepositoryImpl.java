package org.source.jpa.repository.extension;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.constant.Constants;
import org.source.utility.utils.Reflects;
import org.source.utility.utils.Streams;
import org.source.utility.utils.Strings;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class ExtensionRepositoryImpl<T, I> extends SimpleJpaRepository<T, I> implements ExtensionRepository<T> {

    @PersistenceContext
    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

    public ExtensionRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
                                   EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    private static final Map<String, TableDetail> TABLE_DETAILS = new ConcurrentHashMap<>();


    @org.springframework.data.jpa.repository.Query
    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int onDuplicateUpdateBatch(Collection<T> ts) {
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

    public Object getColumnValue(Object entity, ColumnField columnField) {
        Object obj = entity;
        if (!CollectionUtils.isEmpty(columnField.embeddedFieldNames)) {
            Iterator<String> iterator = columnField.embeddedFieldNames.iterator();
            while (iterator.hasNext() && Objects.nonNull(obj)) {
                obj = Reflects.getFieldValue(obj, iterator.next());
            }
        }
        if (Objects.nonNull(obj)) {
            return Reflects.getFieldValue(obj, columnField.getFieldName());
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
        List<ColumnField> columnFields = getColumnFields(entityClass, null);
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

        public static ExtensibleValue of(String columnName, Extensible extensible) {
            if (Objects.nonNull(extensible)) {
                return new ExtensibleValue(extensible);
            }
            if (Constants.COLUMN_ID.equals(columnName)) {
                return new ExtensibleValue(false, false);
            } else if (Constants.COLUMN_CREATE_USER.equals(columnName)) {
                return new ExtensibleValue(true, false);
            } else if (Constants.COLUMN_CREATE_TIME.equals(columnName)) {
                return new ExtensibleValue(false, false);
            } else if (Constants.COLUMN_UPDATE_TIME.equals(columnName)) {
                return new ExtensibleValue(false, false);
            } else {
                return new ExtensibleValue(true, true);
            }
        }
    }

}
