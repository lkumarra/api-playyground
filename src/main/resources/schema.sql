-- Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name TEXT,
    type TEXT,
    price DECIMAL,
    upc TEXT,
    shipping DECIMAL,
    description TEXT,
    manufacturer TEXT,
    model TEXT,
    url TEXT,
    image TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id TEXT PRIMARY KEY,
    name TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- In-store services table
CREATE TABLE IF NOT EXISTS in_store_services (
    id BIGSERIAL PRIMARY KEY,
    name TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Stores table
CREATE TABLE IF NOT EXISTS stores (
    id BIGSERIAL PRIMARY KEY,
    name TEXT,
    type TEXT,
    address TEXT,
    address2 TEXT,
    city TEXT,
    state TEXT,
    zip TEXT,
    lat DECIMAL,
    lng DECIMAL,
    hours TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Zipcodes table
CREATE TABLE IF NOT EXISTS zipcodes (
    zip TEXT PRIMARY KEY,
    lat DECIMAL,
    lng DECIMAL,
    city TEXT,
    state TEXT
);

-- Product-Category junction table
CREATE TABLE IF NOT EXISTS product_category (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, category_id)
);

-- Store-Service junction table
CREATE TABLE IF NOT EXISTS store_services (
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES in_store_services(id) ON DELETE CASCADE,
    PRIMARY KEY (store_id, service_id)
);

-- Sub-categories junction table
CREATE TABLE IF NOT EXISTS sub_categories (
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    sub_category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (category_id, sub_category_id)
);

-- Category path junction table
CREATE TABLE IF NOT EXISTS category_path (
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    path_category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (category_id, path_category_id)
);
