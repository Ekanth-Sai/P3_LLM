# auth.py

from typing import List, Dict

class AccessManager:
    """Manages user roles and their associated data access permissions."""

    # In a real application, this would come from a database or configuration service.
    ROLES_PERMISSIONS: Dict[str, Dict[str, List[str]]] = {
        "hr_manager": {
            "allowed_departments": ["HR", "General"],
            "allowed_sensitivity": ["Internal", "Confidential"]
        },
        "accounting_staff": {
            "allowed_departments": ["Accounting", "General"],
            "allowed_sensitivity": ["Internal", "Confidential"]
        },
        "sales_rep": {
            "allowed_departments": ["Sales", "General"],
            "allowed_sensitivity": ["Internal"]
        },
        "executive": {
            "allowed_departments": ["HR", "Accounting", "Sales", "Legal", "General"],
            "allowed_sensitivity": ["Internal", "Confidential", "Highly Confidential"]
        },
        "general_employee": {
            "allowed_departments": ["General"],
            "allowed_sensitivity": ["Public"]
        }
    }

    # Dummy user-role mapping
    USERS_ROLES: Dict[str, List[str]] = {
        "ekanthsaiy@gmail.com": ["executive"],
        "alice": ["hr_manager"],
        "bob": ["accounting_staff"],
        "charlie": ["sales_rep"],
        "david": ["executive"],
        "eve": ["general_employee"],
        "frank": ["hr_manager", "executive"] # User with multiple roles
    }

    def get_user_roles(self, username: str) -> List[str]:
        """Retrieves roles for a given username."""
        return self.USERS_ROLES.get(username, [])

    def get_allowed_metadata_filters(self, username: str) -> Dict[str, List[str]]:
        """
        Aggregates all allowed metadata filters for a user based on their roles.
        Returns a dictionary like {'department': ['HR', 'General'], 'sensitivity': ['Internal']}
        """
        user_roles = self.get_user_roles(username)
        
        allowed_departments = set()
        allowed_sensitivity = set()

        for role in user_roles:
            permissions = self.ROLES_PERMISSIONS.get(role, {})
            allowed_departments.update(permissions.get("allowed_departments", []))
            allowed_sensitivity.update(permissions.get("allowed_sensitivity", []))
        
        filters = {}
        if allowed_departments:
            filters["department"] = list(allowed_departments)
        if allowed_sensitivity:
            filters["sensitivity"] = list(allowed_sensitivity)
            
        return filters