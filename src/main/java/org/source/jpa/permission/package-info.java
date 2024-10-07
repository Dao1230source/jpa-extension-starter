@FilterDefs(value = {
        @FilterDef(name = FilterConstant.PERMISSION,
                parameters = @ParamDef(name = FilterConstant.SPACE_IDS, type = String.class),
                defaultCondition = "space_id in :" + FilterConstant.SPACE_IDS
        ),
})
package org.source.jpa.permission;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.ParamDef;
import org.source.jpa.enhance.FilterConstant;