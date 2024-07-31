@FilterDefs(value = {
        @FilterDef(name = FilterConstant.UNDELETED, defaultCondition = "deleted = false"),
        @FilterDef(name = FilterConstant.USABLE, defaultCondition = "status = 0"),
        @FilterDef(name = FilterConstant.PREFERRED, defaultCondition = "preferred = 1")
})
package org.source.jpa.enhance;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
