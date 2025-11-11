CREATE TABLES IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(100) UNIQUE NOT NULL,
    parent_role_id INTEGER REFERENCES roles(id),
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    allowed_sensitivity TEXT[] DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPDEFAULT CURRENT_TIMESTAMP
);

-- Base roles
INSERT INTO roles (role_name, parent_role_id, display_name, description, allowed_sensitivity) VALUES
('CEO', NULL, 'Chief Executive Officer', 'Top-level executive with full access', 
    ARRAY['Public', 'Internal', 'Confidential', 'Highly Confidential']),
('CTO', 1, 'Chief Technology Officer', 'Technology department head', 
    ARRAY['Public', 'Internal', 'Confidential', 'Highly Confidential']),
('CPO', 1, 'Chief Product Officer', 'Product department head', 
    ARRAY['Public', 'Internal', 'Confidential', 'Highly Confidential']),
('CFO', 1, 'Chief Financial Officer', 'Finance department head', 
    ARRAY['Public', 'Internal', 'Confidential', 'Highly Confidential']),
('HR', 1, 'Human Resources', 'HR department head', 
    ARRAY['Public', 'Internal', 'Confidential']),
('PRODUCT_MANAGER', 3, 'Product Manager', 'Product management role', 
    ARRAY['Public', 'Internal', 'Confidential']),
('PROJECT_MANAGER', 3, 'Project Manager', 'Project management role', 
    ARRAY['Public', 'Internal', 'Confidential']),
('TEAM_LEAD', 2, 'Team Lead', 'Engineering team lead', 
    ARRAY['Public', 'Internal', 'Confidential']),
('SENIOR_DEVELOPER', 8, 'Senior Developer', 'Senior engineering role', 
    ARRAY['Public', 'Internal']),
('DEVELOPER', 9, 'Developer', 'Engineering role', 
    ARRAY['Public', 'Internal']),
('JUNIOR_DEVELOPER', 10, 'Junior Developer', 'Junior engineering role', 
    ARRAY['Public']),
('INTERN', 11, 'Intern', 'Entry-level role', 
    ARRAY['Public'])
ON CONFLICT (role_name) DO NOTHING;

-- Sub Roles for HR
INSERT INTO sub_roles (sub_role_name, parent_role_id, display_name, description, allowed_sensitivity) VALUES
('HR_HIRING', 5, 'HR - Hiring', 'Recruitment and hiring', ARRAY['Public', 'Internal', 'Confidential']),
('HR_PAYROLL', 5, 'HR - Payroll', 'Salary and compensation', ARRAY['Confidential', 'Highly Confidential']),
('HR_COMPLIANCE', 5, 'HR - Compliance', 'Legal and compliance', ARRAY['Internal', 'Confidential']),
('HR_TRAINING', 5, 'HR - Training', 'Employee training and development', ARRAY['Public', 'Internal'])
ON CONFLICT (sub_role_name) DO NOTHING;

-- Sub Roles for CTO
INSERT INTO sub_roles (sub_role_name, parent_role_id, display_name, description, allowed_sensitivity) VALUES
('CTO_ARCHITECTURE', 2, 'CTO - Architecture', 'System architecture decisions', ARRAY['Public', 'Internal', 'Confidential']),
('CTO_SECURITY', 2, 'CTO - Security', 'Security and compliance', ARRAY['Confidential', 'Highly Confidential']),
('CTO_INFRASTRUCTURE', 2, 'CTO - Infrastructure', 'Infrastructure management', ARRAY['Internal', 'Confidential'])
ON CONFLICT (sub_role_name) DO NOTHING;
