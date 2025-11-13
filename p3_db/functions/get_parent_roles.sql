CREATE OR REPLACE FUNCTION get_parent_roles(p_role_id BIGINT)
RETURNS TABLE(id BIGINT, role_name VARCHAR, parent_role_id BIGINT, display_name VARCHAR, description TEXT, allowed_sensitivity TEXT[], created_at TIMESTAMP, updated_at TIMESTAMP) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE parent_roles_cte AS (
        SELECT r.id, r.role_name, r.parent_role_id, r.display_name, r.description, r.allowed_sensitivity, r.created_at, r.updated_at
        FROM roles r
        WHERE r.id = p_role_id
        UNION ALL
        SELECT r.id, r.role_name, r.parent_role_id, r.display_name, r.description, r.allowed_sensitivity, r.created_at, r.updated_at
        FROM roles r
        JOIN parent_roles_cte prc ON r.id = prc.parent_role_id
    )
    SELECT * FROM parent_roles_cte;
END;
$$ LANGUAGE plpgsql;
