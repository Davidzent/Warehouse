package com.warehouse.receiving.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.warehouse.receiving.domain.Inventory;

@Mapper
public interface InventoryMapper {

    int upsertAddQuantity(@Param("productId") long productId,
                          @Param("locationId") long locationId,
                          @Param("quantity") int quantity);
    
    Inventory findByProductAndLocation(@Param("productId") long productId,
                                       @Param("locationId") long locationId);
    
    List<Inventory> findAll();
}
