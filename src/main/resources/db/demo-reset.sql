-- Clears the demo tables so data.sql can be replayed verbatim. Child-first, so
-- no foreign key is ever violated mid-run. Deliberately not TRUNCATE CASCADE:
-- naming the tables means a future table is never wiped by accident.
DELETE FROM receipt_line;
DELETE FROM inventory;
DELETE FROM receipt;
DELETE FROM purchase_order_line;
DELETE FROM purchase_order;
DELETE FROM location;
DELETE FROM product;
DELETE FROM vendor;
