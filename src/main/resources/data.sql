-- ============================================================================
-- Seed data. Deliberately CONSISTENT: every denormalized running total
-- (purchase_order_line.quantity_received) matches the receipt_line rows that
-- justify it, and inventory matches the good (undamaged) units received.
-- Run the reconciliation query from schema.sql against this data: zero rows.
--
-- Explicit ids in seed scripts keep tests readable ("PO 1000 has line 2000"),
-- but identity columns keep their own counters — so after inserting explicit
-- ids we must bump each counter past the seeded values (see setval calls at
-- the bottom), or the first runtime INSERT would collide with id 1.
-- ============================================================================

INSERT INTO vendor (vendor_id, vendor_code, name, active) VALUES
  (1, 'ACME',    'Acme Industrial Supply', TRUE),
  (2, 'GLOBEX',  'Globex Logistics',       TRUE),
  (3, 'INITECH', 'Initech Components',     TRUE);

INSERT INTO product (product_id, sku, description, unit_of_measure) VALUES
  (10, 'SKU-BOLT-M8', 'M8 hex bolt, zinc coated, 100-pack', 'EA'),
  (11, 'SKU-NUT-M8',  'M8 hex nut, 100-pack',               'EA'),
  (12, 'SKU-PLT-STD', 'Standard pallet', 'EA'),
  (13, 'SKU-WRAP',    'Stretch wrap roll 500mm',            'EA'),
  (14, 'SKU-TAPE',    'Packing tape, 6-pack',               'EA');

INSERT INTO location (location_id, location_code, location_type) VALUES
  (100, 'DOCK-01', 'DOCK'),
  (101, 'A-01-01', 'STORAGE'),
  (102, 'A-01-02', 'STORAGE'),
  (103, 'QC-HOLD', 'QUARANTINE');

-- ---------------------------------------------------------------------------
-- PO-1000: untouched OPEN order, 3 lines — the main demo/receiving target.
-- ---------------------------------------------------------------------------
INSERT INTO purchase_order (po_id, po_number, vendor_id, status, order_date, expected_date,
                            created_at, created_by, updated_at, updated_by) VALUES
  (1000, 'PO-1000', 1, 'OPEN', DATE '2026-07-10', DATE '2026-07-18',
   now(), 'seed', now(), 'seed');

INSERT INTO purchase_order_line (po_line_id, po_id, line_number, product_id,
                                 quantity_ordered, quantity_received, unit_cost) VALUES
  (2000, 1000, 1, 10,  50, 0, 12.50),
  (2001, 1000, 2, 11, 200, 0,  0.35),
  (2002, 1000, 3, 13,  40, 0,  4.99);

-- ---------------------------------------------------------------------------
-- PO-1001: PARTIALLY_RECEIVED — 5 of 20 pallets arrived (1 damaged), so the
-- running total says 5, inventory says 4 good units, and one receipt row
-- documents the delivery. This is what "consistent seed data" means.
-- ---------------------------------------------------------------------------
INSERT INTO purchase_order (po_id, po_number, vendor_id, status, order_date, expected_date,
                            created_at, created_by, updated_at, updated_by) VALUES
  (1001, 'PO-1001', 2, 'PARTIALLY_RECEIVED', DATE '2026-07-08', DATE '2026-07-15',
   now(), 'seed', now(), 'seed-clerk');

INSERT INTO purchase_order_line (po_line_id, po_id, line_number, product_id,
                                 quantity_ordered, quantity_received, unit_cost) VALUES
  (2010, 1001, 1, 12, 20, 5, 8.00),
  (2011, 1001, 2, 14, 10, 0, 2.25);

INSERT INTO receipt (receipt_id, po_id, received_by, received_at, carrier_reference, notes) VALUES
  (3000, 1001, 'seed-clerk', now() - INTERVAL '2 days', 'BOL-778812', 'First partial delivery');
-- DB2 note: interval arithmetic is spelled  CURRENT TIMESTAMP - 2 DAYS

INSERT INTO receipt_line (receipt_line_id, receipt_id, po_line_id, location_id,
                          quantity_received, quantity_damaged) VALUES
  (4000, 3000, 2010, 101, 5, 1);

-- ---------------------------------------------------------------------------
-- PO-1002: fully received and CLOSED — use it to see the 409 CONFLICT when
-- you try to receive against a closed order.
-- ---------------------------------------------------------------------------
INSERT INTO purchase_order (po_id, po_number, vendor_id, status, order_date, expected_date,
                            created_at, created_by, updated_at, updated_by) VALUES
  (1002, 'PO-1002', 1, 'CLOSED', DATE '2026-06-20', DATE '2026-06-28',
   now(), 'seed', now(), 'seed-clerk');

INSERT INTO purchase_order_line (po_line_id, po_id, line_number, product_id,
                                 quantity_ordered, quantity_received, unit_cost) VALUES
  (2020, 1002, 1, 10, 10, 10, 12.50);

INSERT INTO receipt (receipt_id, po_id, received_by, received_at, carrier_reference, notes) VALUES
  (3001, 1002, 'seed-clerk', now() - INTERVAL '14 days', 'BOL-771203', NULL);

INSERT INTO receipt_line (receipt_line_id, receipt_id, po_line_id, location_id,
                          quantity_received, quantity_damaged) VALUES
  (4001, 3001, 2020, 101, 10, 0);

-- ---------------------------------------------------------------------------
-- PO-1003: CANCELLED — the other 409 case.
-- ---------------------------------------------------------------------------
INSERT INTO purchase_order (po_id, po_number, vendor_id, status, order_date, expected_date,
                            created_at, created_by, updated_at, updated_by) VALUES
  (1003, 'PO-1003', 3, 'CANCELLED', DATE '2026-07-01', NULL,
   now(), 'seed', now(), 'seed-manager');

INSERT INTO purchase_order_line (po_line_id, po_id, line_number, product_id,
                                 quantity_ordered, quantity_received, unit_cost) VALUES
  (2030, 1003, 1, 14, 5, 0, 2.25);

-- ---------------------------------------------------------------------------
-- inventory: matches the GOOD units from the receipts above.
--   PO-1001 delivery: 5 received - 1 damaged = 4 pallets  -> product 12 @ A-01-01
--   PO-1002 delivery: 10 received - 0 damaged             -> product 10 @ A-01-01
-- ---------------------------------------------------------------------------
INSERT INTO inventory (inventory_id, product_id, location_id, quantity_on_hand, updated_at) VALUES
  (5000, 12, 101,  4, now()),
  (5001, 10, 101, 10, now());

-- ---------------------------------------------------------------------------
-- Bump each identity counter past the explicit ids we just inserted.
-- PostgreSQL: setval on the sequence behind the identity column.
-- DB2 equivalent:  ALTER TABLE vendor ALTER COLUMN vendor_id RESTART WITH 4;
-- ---------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('vendor',              'vendor_id'),       100);
SELECT setval(pg_get_serial_sequence('product',             'product_id'),      100);
SELECT setval(pg_get_serial_sequence('location',            'location_id'),     200);
SELECT setval(pg_get_serial_sequence('purchase_order',      'po_id'),          2000);
SELECT setval(pg_get_serial_sequence('purchase_order_line', 'po_line_id'),     3000);
SELECT setval(pg_get_serial_sequence('receipt',             'receipt_id'),     5000);
SELECT setval(pg_get_serial_sequence('receipt_line',        'receipt_line_id'),6000);
SELECT setval(pg_get_serial_sequence('inventory',           'inventory_id'),   7000);
