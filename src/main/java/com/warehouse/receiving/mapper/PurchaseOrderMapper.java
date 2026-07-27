package com.warehouse.receiving.mapper;

import com.warehouse.receiving.domain.PoStatus;
import com.warehouse.receiving.domain.PurchaseOrder;
import com.warehouse.receiving.domain.PurchaseOrderLine;
import com.warehouse.receiving.dto.PurchaseOrderSearchCriteria;
import com.warehouse.receiving.dto.PurchaseOrderSummary;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PurchaseOrderMapper {

    PurchaseOrder findById(@Param("poId") long poId);

    PurchaseOrderLine findLineById(@Param("poLineId") long poLineId);

    List<PurchaseOrderSummary> search(@Param("criteria") PurchaseOrderSearchCriteria criteria);

    Long lockHeader(@Param("poId") long poId);

    int addToLineReceivedQuantity(@Param("poLineId") long poLineId, @Param("delta") int delta);

    int updateStatus(@Param("poId") long poId,
                     @Param("status") PoStatus status,
                     @Param("updatedBy") String updatedBy);
}
