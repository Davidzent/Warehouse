package com.warehouse.receiving.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.warehouse.receiving.domain.Receipt;
import com.warehouse.receiving.domain.ReceiptLine;

@Mapper
public interface ReceiptMapper {

    int insertReceipt(Receipt receipt);

    int insertReceiptLine(ReceiptLine line);

    Receipt findById(@Param("receiptId") long receiptId);
}
