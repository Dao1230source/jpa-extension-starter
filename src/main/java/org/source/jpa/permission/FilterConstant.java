package org.source.jpa.permission;

/**
 * Hibernate Filter 常量定义
 * <p>
 * 定义内置的 Filter 名称和参数名
 *
 * @author zengfugen
 */
public class FilterConstant {

    private FilterConstant() {
    }

    /**
     * 未删除 Filter
     * <p>
     * 条件：deleted = false
     */
    public static final String NOT_DELETED = "NOT_DELETED";

    /**
     * 默认值 Filter
     * <p>
     * 条件：defaulted = 1
     */
    public static final String IS_DEFAULTED = "IS_DEFAULTED";

    /**
     * 可用状态 Filter
     * <p>
     * 条件：status = 0
     */
    public static final String USABLE = "USABLE";

    /**
     * 权限 Filter
     * <p>
     * 条件：space_id in :spaceIds
     */
    public static final String PERMISSION = "PERMISSION";

    /**
     * 权限 Filter 参数名
     */
    public static final String SPACE_IDS = "spaceIds";
}
