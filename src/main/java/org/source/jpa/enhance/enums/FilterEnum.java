package org.source.jpa.enhance.enums;


import lombok.Getter;
import org.source.jpa.enhance.FilterConstant;

@Getter
public enum FilterEnum {
    /**
     * 未删除的
     */
    UNDELETED(FilterConstant.UNDELETED, "未删除的"),
    PREFERRED(FilterConstant.PREFERRED, "优先的"),
    USABLE(FilterConstant.USABLE, "可用的"),
    PERMISSION(FilterConstant.PERMISSION, "数据权限"),
    ;

    private final String name;
    private final String desc;

    FilterEnum(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}
