@FilterDefs(value = {
        @FilterDef(name = FilterConstant.NOT_DELETED, defaultCondition = "deleted = false"),
        @FilterDef(name = FilterConstant.IS_DEFAULTED, defaultCondition = "defaulted = 1"),
        @FilterDef(name = FilterConstant.USABLE, defaultCondition = "status = 0")
})
package org.source.jpa.enhance;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
