PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS admins(id INTEGER PRIMARY KEY, username TEXT NOT NULL UNIQUE COLLATE NOCASE, display_name TEXT NOT NULL, password_hash TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS categories(id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE);
CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE, contact_person TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '', email TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '');
-- Money is represented as integer minor units (cents), avoiding SQLite floating-point rounding.
CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE, category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT, supplier_id INTEGER NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT, quantity INTEGER NOT NULL CHECK(quantity BETWEEN 0 AND 1000000000), purchase_cents INTEGER NOT NULL CHECK(purchase_cents >= 0), selling_cents INTEGER NOT NULL CHECK(selling_cents >= 0), min_stock INTEGER NOT NULL CHECK(min_stock >= 0), description TEXT NOT NULL DEFAULT '');
CREATE TABLE IF NOT EXISTS stock_transactions(id INTEGER PRIMARY KEY, product_id INTEGER REFERENCES products(id) ON DELETE SET NULL, product_name TEXT NOT NULL, type TEXT NOT NULL CHECK(type IN ('OPENING','IN','OUT','SALE','ADJUSTMENT')), quantity INTEGER NOT NULL CHECK(quantity >= 0), previous_stock INTEGER NOT NULL CHECK(previous_stock >= 0), new_stock INTEGER NOT NULL CHECK(new_stock >= 0), created_at TEXT NOT NULL, remarks TEXT NOT NULL DEFAULT '');
CREATE TABLE IF NOT EXISTS sales(id INTEGER PRIMARY KEY, invoice_number TEXT NOT NULL UNIQUE, created_at TEXT NOT NULL, subtotal_cents INTEGER NOT NULL CHECK(subtotal_cents >= 0), discount_cents INTEGER NOT NULL CHECK(discount_cents >= 0 AND discount_cents <= subtotal_cents), total_cents INTEGER NOT NULL CHECK(total_cents = subtotal_cents - discount_cents));
CREATE TABLE IF NOT EXISTS sale_items(id INTEGER PRIMARY KEY, sale_id INTEGER NOT NULL REFERENCES sales(id), product_id INTEGER REFERENCES products(id) ON DELETE SET NULL, product_name TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity > 0), unit_cents INTEGER NOT NULL CHECK(unit_cents >= 0), total_cents INTEGER NOT NULL CHECK(total_cents = quantity * unit_cents));
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_supplier ON products(supplier_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON stock_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_product ON stock_transactions(product_id);
CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(created_at);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id);
