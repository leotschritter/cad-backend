# Tenant Service - Implementation Guide

## Overview

The Tenant Service manages the complete lifecycle of Standard tier tenants in the multi-tenant SaaS platform. It orchestrates tenant creation, provisioning, and deletion through GitHub Actions workflows.

## Architecture

### Components

1. **Tenant Model** - MongoDB entity storing tenant metadata and state
2. **REST API** - Endpoints for tenant management (create, get, delete)
3. **GitHub Actions Integration** - Triggers provisioning/deprovisioning workflows
4. **Email Notifications** - SMTP-based notifications to tenant owners
5. **Polling Service** - Monitors workflow status and updates tenant state

### Key Features

- ✅ Tenant creation with automatic ID generation
- ✅ GitHub Actions workflow triggering
- ✅ Workflow status polling and state management
- ✅ Email notifications for provisioning success/failure
- ✅ MongoDB persistence with state tracking
- ✅ RESTful API with OpenAPI documentation

## Prerequisites

### GitHub Workflows

Two GitHub Actions workflows are already created in `.github/workflows/`:

#### 1. `provision-tenant.yml`

This workflow provisions a new Standard tier tenant with:
- Kubernetes namespace with tenant label
- Identity Platform tenant (separate user pool)
- Firestore database (per-tenant isolation)
- Helm deployments of all microservices using existing charts
- Multi-service Ingress with TLS

**Required Inputs:**
```yaml
inputs:
  tenant_id:
    description: 'Sanitized tenant ID (e.g., acme-corp)'
    required: true
  namespace:
    description: 'Kubernetes namespace (e.g., tenant-acme-corp)'
    required: true
  subdomain:
    description: 'Full subdomain (e.g., acme-corp.tripico.fun)'
    required: true
  owner_email:
    description: 'Email of tenant owner'
    required: true
```

**Expected Actions:**
1. Create Kubernetes namespace
2. Provision databases (Terraform or Helm)
3. Deploy all microservices via Helm
4. Configure Ingress and TLS
5. Create Identity Platform tenant
6. Create Identity Platform user for owner

#### 2. `deprovision-tenant.yml`

This workflow removes all tenant resources.

**Required Inputs:**
```yaml
inputs:
  tenant_id:
    description: 'Tenant ID to delete'
    required: true
  namespace:
    description: 'Kubernetes namespace to delete'
    required: true
```

**What It Does:**
1. Deletes Kubernetes namespace (cascades all resources)
2. Deletes Firestore database
3. Deletes Identity Platform tenant
4. Cleans up tenant-specific storage buckets

## Configuration

### Environment Variables

#### GitHub Actions
```bash
GITHUB_TOKEN=ghp_xxxxxxxxxxxx                    # GitHub PAT with workflow permissions
GITHUB_REPO_OWNER=your-org                       # GitHub organization/user
GITHUB_REPO_NAME=cad-backend                     # Repository name
```

#### SMTP Configuration
```bash
SMTP_FROM=noreply.tenant@htwg-konstanz.de
SMTP_HOST=smtp.htwg-konstanz.de
SMTP_PORT=587
SMTP_USER=your-username
SMTP_PASSWORD=your-password
SMTP_START_TLS=REQUIRED
```

#### Tenant Configuration
```bash
TENANT_BASE_DOMAIN=tripico.fun                   # Base domain for subdomains
TENANT_POLLING_INTERVAL_SECONDS=30               # Workflow polling interval
```

#### MongoDB
```bash
MONGODB_CONNECTION_STRING=mongodb://localhost:27017
MONGODB_DATABASE=tenant_db
```

### Helm Deployment

#### Development
```bash
helm upgrade --install tenant-service ./tenant-service-chart \
  -f ./tenant-service-chart/values-dev.yaml \
  --set smtp.user="your-username" \
  --set smtp.password="your-password" \
  --set github.token="ghp_xxxxxxxxxxxx" \
  --set github.repository.owner="your-org" \
  --set github.repository.name="cad-backend" \
  --namespace default
```

#### Production
```bash
helm upgrade --install tenant-service ./tenant-service-chart \
  -f ./tenant-service-chart/values-prod.yaml \
  --set smtp.user="your-username" \
  --set smtp.password="your-password" \
  --set github.token="ghp_xxxxxxxxxxxx" \
  --set github.repository.owner="your-org" \
  --set github.repository.name="cad-backend" \
  --namespace default
```

## API Endpoints

### Base URL
- Development: `https://dev-tenant.tripico.fun/api/v1/tenants`
- Production: `https://tenant.tripico.fun/api/v1/tenants`

### Create Tenant
```http
POST /api/v1/tenants
Content-Type: application/json

{
  "name": "Acme Corp",
  "ownerEmail": "owner@acme.com",
  "ownerPassword": "SecurePassword123!"
}
```

**Response (202 Accepted):**
```json
{
  "id": "67890abcdef",
  "tenantId": "acme-corp",
  "name": "Acme Corp",
  "subdomain": "acme-corp.tripico.fun",
  "state": "PROVISIONING",
  "message": "Tenant provisioning initiated. You will receive an email when your tenant is ready."
}
```

### Get All Tenants
```http
GET /api/v1/tenants
```

**Response (200 OK):**
```json
[
  {
    "id": "67890abcdef",
    "name": "Acme Corp",
    "tenantId": "acme-corp",
    "subdomain": "acme-corp.tripico.fun",
    "ownerEmail": "owner@acme.com",
    "state": "ACTIVE",
    "tier": "STANDARD",
    "namespace": "tenant-acme-corp",
    "errorMessage": null,
    "createdAt": "2026-01-04T12:00:00",
    "updatedAt": "2026-01-04T12:05:00",
    "activatedAt": "2026-01-04T12:05:00"
  }
]
```

### Get Tenant by ID
```http
GET /api/v1/tenants/{tenantId}
```

### Delete Tenant
```http
DELETE /api/v1/tenants/{tenantId}
```

**Response (202 Accepted):**
```json
{
  "message": "Tenant deletion initiated. You will receive an email when complete."
}
```

## Tenant Lifecycle States

| State | Description |
|-------|-------------|
| `PENDING` | Tenant created, workflow not yet triggered |
| `PROVISIONING` | GitHub Actions provisioning workflow running |
| `ACTIVE` | Tenant fully provisioned and operational |
| `DEPROVISIONING` | GitHub Actions deprovision workflow running |
| `DELETED` | Tenant successfully removed |
| `FAILED` | Provisioning or deprovisioning failed |

## Email Notifications

### Activation Email
Sent when tenant provisioning completes successfully.

**Includes:**
- Tenant name and ID
- Frontend URL (subdomain)
- Owner email
- Call-to-action button

### Failure Email
Sent when provisioning fails.

**Includes:**
- Error message
- Workflow run URL for debugging

### Deletion Email
Sent when tenant is successfully deleted.

## Polling Mechanism

The service polls GitHub Actions workflow status every 30 seconds (configurable):

1. Queries tenants in `PROVISIONING` or `DEPROVISIONING` state
2. Fetches workflow run status from GitHub API
3. Updates tenant state based on workflow conclusion:
   - `success` → `ACTIVE` or `DELETED`
   - `failure` / `cancelled` → `FAILED`
4. Sends appropriate email notification

## Local Development

### 1. Start MongoDB
```bash
cd services/tenant-service
docker-compose up -d
```

### 2. Configure Environment
Create `.env` file or export variables:
```bash
export GITHUB_TOKEN=your-token
export GITHUB_REPO_OWNER=your-org
export GITHUB_REPO_NAME=cad-backend
export SMTP_USER=your-username
export SMTP_PASSWORD=your-password
```

### 3. Run in Dev Mode
```bash
./mvnw quarkus:dev
```

### 4. Access Swagger UI
http://localhost:8080/q/swagger-ui

## Testing

### Create a Test Tenant
```bash
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Corp",
    "ownerEmail": "test@example.com",
    "ownerPassword": "TestPassword123!"
  }'
```

### Check Tenant Status
```bash
curl http://localhost:8080/api/v1/tenants/test-corp
```

### Monitor Logs
```bash
# Watch for workflow triggers and polling
./mvnw quarkus:dev
```

## Security Considerations

### Password Storage
Currently, passwords are base64 encoded temporarily during provisioning. In production:
- Use BCrypt or Argon2 for hashing
- Consider external secret management (Vault, GCP Secret Manager)
- Clear password hash after Identity Platform user creation

### GitHub Token
- Use fine-grained PAT with minimal permissions (workflow dispatch only)
- Store in Kubernetes Secret, not ConfigMap
- Rotate regularly

### SMTP Credentials
- Store in Kubernetes Secret
- Use application-specific passwords
- Consider using OAuth2 if supported

## Troubleshooting

### Tenant Stuck in PROVISIONING
1. Check GitHub Actions workflow status manually
2. Verify workflow run ID in tenant record
3. Check workflow logs for errors
4. Verify GitHub token permissions

### Email Not Sent
1. Check SMTP credentials
2. Verify SMTP host/port accessibility
3. Check service logs for mailer errors
4. Test with mock mailer in dev mode

### Workflow Not Triggered
1. Verify GitHub token validity
2. Check repository owner/name configuration
3. Verify workflow file names match configuration
4. Check API rate limits

## Future Enhancements

- [ ] Identity Platform integration for user creation
- [ ] Webhook-based workflow status updates (replace polling)
- [ ] Tenant resource usage monitoring
- [ ] Billing integration
- [ ] Tenant backup and restore
- [ ] Multi-region support
- [ ] Free tier implementation
- [ ] Enterprise tier (dedicated clusters)

## Support

For issues or questions:
- Check service logs: `kubectl logs -l app=tenant-service`
- Review workflow runs on GitHub
- Check MongoDB for tenant state
- Verify configuration in ConfigMap/Secrets

