CREATE TABLES IF NOT EXISTS sub_roles (
    id SERIAL PRIMARY KEY,
    sub_role_name VARCHAR(100) UNIQUE NOT NULL,
    parent_role_id INTEGER REFERENCES roles(id) NOT NULL,
    display_varchar VARCHAR(160) NOT NULL,
    description TEXT,
    allowed_sensitivity TEXT[] DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);