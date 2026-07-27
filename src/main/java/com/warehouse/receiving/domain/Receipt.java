package com.warehouse.receiving.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Receipt {

    private long receiptId;      
    private long poId;
    private String receivedBy;   // from the JWT subject, never from the request
    private OffsetDateTime receivedAt;
    private String carrierReference;
    private String notes;

    private List<ReceiptLine> lines = new ArrayList<>();

    public long getReceiptId() { return receiptId; }
    public void setReceiptId(long receiptId) { this.receiptId = receiptId; }

    public long getPoId() { return poId; }
    public void setPoId(long poId) { this.poId = poId; }

    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }

    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getCarrierReference() { return carrierReference; }
    public void setCarrierReference(String carrierReference) { this.carrierReference = carrierReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<ReceiptLine> getLines() { return lines; }
    public void setLines(List<ReceiptLine> lines) { this.lines = lines; }
}
