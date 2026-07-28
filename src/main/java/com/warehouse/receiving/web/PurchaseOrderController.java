package com.warehouse.receiving.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.warehouse.receiving.dto.PurchaseOrderDetailResponse;
import com.warehouse.receiving.service.ReceivingService;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final ReceivingService receivingService;

    public PurchaseOrderController(ReceivingService receivingService) {
        this.receivingService = receivingService;
    }

    // GET /api/purchase-orders/{id}
    @GetMapping("/{poId}")
    public PurchaseOrderDetailResponse getById(@PathVariable long poId) {
        return PurchaseOrderDetailResponse.from(receivingService.getPurchaseOrder(poId));
    }
}
