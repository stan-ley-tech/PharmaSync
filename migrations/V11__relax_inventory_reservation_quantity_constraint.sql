-- A reservation's quantity tracks how much of it is still outstanding: it is decremented
-- as it gets consumed by dispensing and legitimately reaches zero once fully consumed
-- (the row is kept, with status set to CONSUMED, as part of the movement history). The
-- original constraint required quantity > 0 unconditionally, which rejected that terminal
-- state.
ALTER TABLE inventory_reservations DROP CONSTRAINT chk_inventory_reservations_quantity;
ALTER TABLE inventory_reservations ADD CONSTRAINT chk_inventory_reservations_quantity CHECK (quantity >= 0);
