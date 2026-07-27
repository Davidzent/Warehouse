package com.warehouse.receiving.dto;

import java.util.List;

import com.warehouse.receiving.domain.PoStatus;

public record PurchaseOrderSearchCriteria(
        List<PoStatus> statuses,     // e.g. [OPEN, PARTIALLY_RECEIVED] -> SQL IN (...) via <foreach>
        Long vendorId,
        String poNumberContains,     // case-insensitive substring match
        PoSort sort                  // never null; controller defaults it
) {}
