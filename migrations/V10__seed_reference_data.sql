-- Minimal reference data so a freshly provisioned environment is immediately usable:
-- one branch, one administrator, two suppliers, and a handful of catalog items that
-- line up with SupplierSimulatorController's in-memory catalogs.

INSERT INTO pharmacies (code, name, address_line1, city, state, postal_code, country, phone)
VALUES ('MAIN01', 'PharmaSync Main Branch', '1 Market Street', 'Lagos', 'Lagos', '100001', 'Nigeria', '+234-800-000-0000');

-- Password: ChangeMe123!  -- rotate on first login in any real deployment.
INSERT INTO users (username, email, password_hash, first_name, last_name, is_active)
VALUES ('admin', 'admin@pharmasync.local',
        '$2a$10$.HCOAImIeUDEv6ROpE/tP.Y8Qvbw5Wqu.T1mZF5N.YXo1SajgqGi2',
        'System', 'Administrator', true);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMIN';

INSERT INTO suppliers (code, name, contact_name, email, phone)
VALUES
    ('MEDCO', 'MedCo Distributors', 'Amaka Obi', 'orders@medco.example', '+234-801-000-0001'),
    ('GLOBALPHARMA', 'GlobalPharma Supply Co.', 'Femi Adeyemi', 'orders@globalpharma.example', '+234-801-000-0002');

INSERT INTO medicines (sku, name, generic_name, form, strength, manufacturer, unit_of_measure,
                        requires_prescription, controlled_substance, reorder_threshold, reorder_quantity,
                        default_supplier_id, unit_price)
VALUES
    ('PARA-500', 'Paracetamol 500mg', 'Paracetamol', 'TABLET', '500mg', 'MedCo', 'TABLET',
     false, false, 200, 1000, (SELECT id FROM suppliers WHERE code = 'MEDCO'), 0.15),
    ('AMOX-250', 'Amoxicillin 250mg', 'Amoxicillin', 'CAPSULE', '250mg', 'MedCo', 'CAPSULE',
     true, false, 100, 500, (SELECT id FROM suppliers WHERE code = 'MEDCO'), 0.35),
    ('IBU-200', 'Ibuprofen 200mg', 'Ibuprofen', 'TABLET', '200mg', 'MedCo', 'TABLET',
     false, false, 150, 600, (SELECT id FROM suppliers WHERE code = 'MEDCO'), 0.20),
    ('METF-500', 'Metformin 500mg', 'Metformin', 'TABLET', '500mg', 'GlobalPharma', 'TABLET',
     true, false, 100, 400, (SELECT id FROM suppliers WHERE code = 'GLOBALPHARMA'), 0.25);
