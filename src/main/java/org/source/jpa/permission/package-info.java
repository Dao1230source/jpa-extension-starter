@FilterDefs(value = {
        // 配合@Filter使用时，设置deduceAliasInjectionPoints=false
        // eg: @Filter(name = FilterConstant.PERMISSION, deduceAliasInjectionPoints = false)
        // 数据权限，userId创建的 + 赋予权限的单条记录 + 赋予权限的空间
        @FilterDef(name = FilterConstant.PERMISSION,
                parameters = @ParamDef(name = FilterConstant.USER_ID, type = String.class),
                defaultCondition = "({alias}.create_user = :" + FilterConstant.USER_ID +
                        " or exists(select 1 from user_permission up" +
                        " where up.permission_id = {alias}.space_id or up.permission_id = {alias}.id" +
                        " and up.user_id = :" + FilterConstant.USER_ID + "))"
        ),
})
package org.source.jpa.permission;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.ParamDef;
import org.source.jpa.enhance.FilterConstant;