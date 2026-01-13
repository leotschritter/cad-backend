# Backend configuration for Enterprise Tier Tenants (Development)
#
# MULTI-TENANT SETUP:
# Each enterprise tenant requires a unique state prefix to isolate their infrastructure.
# This file serves as a template - the actual values should be set dynamically
# in the CI/CD pipeline (terraform.yml) based on the tenant being deployed.
#
# Example pipeline usage:
#   cat > backend-config-dev.hcl << EOF
#   bucket = "${{ env.TF_STATE_BUCKET }}"
#   prefix = "terraform/state/enterprise/${TENANT_NAME}"
#   EOF
#
# This ensures each tenant (e.g., enterprise-1, acme-corp) has isolated state at:
#   - terraform/state/enterprise-1
#   - terraform/state/acme-corp

bucket = "iaas-476910-terraform-state"
prefix = "terraform/state/${TENANT_NAME}"

# Optional: Enable state encryption
# encryption_key = "your-encryption-key"

