package com.warehouse.receiving.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.warehouse.receiving.domain.PurchaseOrder;
import com.warehouse.receiving.mapper.PurchaseOrderMapper;
import com.warehouse.receiving.service.exception.PurchaseOrderNotFoundException;


@Service
public class ReceivingService {

    private final PurchaseOrderMapper purchaseOrderMapper;

    public ReceivingService(PurchaseOrderMapper purchaseOrderMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrder(long poId) {
        PurchaseOrder po = purchaseOrderMapper.findById(poId);
        if (po == null) {
            throw new PurchaseOrderNotFoundException(poId); // -> 404
        }
        return po;

    }

}
