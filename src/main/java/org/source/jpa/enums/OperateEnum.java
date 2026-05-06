package org.source.jpa.enums;

/**
 * 操作类型枚举
 * <p>
 * 用于 @CheckExists 注解指定在什么操作中检查记录是否存在
 *
 * @author zengfugen
 */
public enum OperateEnum {

    /**
     * 新增操作
     */
    ADD,

    /**
     * 删除操作
     */
    DELETE,

    /**
     * 更新操作
     */
    UPDATE,

    /**
     * 所有操作
     */
    ALL

}
