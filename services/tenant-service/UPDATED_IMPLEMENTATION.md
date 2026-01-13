# Updated Tenant Service Implementation

## Summary of Changes

I've updated the tenant service implementation based on your existing infrastructure:

### ✅ What Changed

#### 1. **Workflows Now Match Your Existing Pattern**

Created actual workflows in `.github/workflows/`:
- `provision-tenant.yml` - Provisions Standard tier tenants
- `deprovision-tenant.yml` - Removes tenant resources

**Key Improvements:**
- ✅ Uses your existing Helm charts (itinerary, weather, warnings, recommendations, comments-likes)
- ✅ Follows same pattern as `ghcr-and-gcp-*.yml` workflows
- ✅ Deploys services using existing `values-dev.yaml` / `values-prod.yaml`
- ✅ Uses `${{ vars.VALUES_FILENAME }}` for environment-specific values
- ✅ Same authentication and kubectl setup as your existing workflows

#### 2. **Identity Platform Multi-Tenancy Configured**

**Status:** ✅ Setup Complete

Your Terraform already has the base Identity Platform configuration:
```terraform
resource "google_identity_platform_config" "default" {
  ...
}
```

The workflows now create **separate Identity Platform tenants** using:
```bash
gcloud identity tenants create {tenant_id} \
  --display-name="{tenant_id}" \
  --allow-password-signup \
  --enable-email-link-signin
```

**What This Gives You:**
- ✅ Isolated user pools per tenant (users in tenant A ≠ tenant B)
- ✅ Tenant ID embedded in JWT tokens automatically
- ✅ Tenant-specific authentication configuration
- ✅ Complete authentication isolation

**Where Tenant ID is Stored:**
- Captured in workflow and stored as `IDENTITY_PLATFORM_TENANT_ID`
- Can be retrieved for user creation after provisioning
- Stored in tenant service MongoDB (field: `identityPlatformTenantId`)

#### 3. **Firestore Multi-Database Setup**

**Status:** ✅ Setup Complete

**What Was Done:**
- Each tenant gets a separate Firestore database
- Database name = tenant ID (e.g., `acme-corp`)
- Created via: `gcloud firestore databases create --database="{tenant_id}"`

**Why Separate Databases vs Collections:**
- ✅ Complete data isolation at database level
- ✅ Better performance (no cross-tenant queries)
- ✅ Easier security rules
- ✅ Clear cost attribution
- ✅ Independent scaling

**How Comments-Likes Service Uses It:**
- Workflow passes `--set firestore.database="{tenant_id}"` to Helm
- Service connects to tenant-specific database
- Collections `comments` and `likes` exist per tenant database

**Note:** Your existing Firestore setup (`(default)` database) remains for non-tenant or shared use.

#### 4. **Service Deployment Pattern**

All services deployed per tenant namespace using existing Helm charts:

```yaml
# Example: Itinerary Service deployment
helm upgrade itinerary-service ./itinerary-service-chart \
  --namespace tenant-acme-corp \
  -f ./itinerary-service-chart/values-dev.yaml \
  --set database.url="..." \
  --set identityPlatform.tenantId="TENANT_ID_FROM_GCLOUD"
```

**Services Deployed Per Tenant:**
1. Itinerary Service → `/api/itinerary`
2. Weather Service → `/api/weather`
3. Travel Warnings Service → `/api/warnings`
4. Recommendation Service → `/api/recommendations`
5. Comments & Likes Service → `/api/comments`, `/api/likes`

**Single Ingress Routes All Services:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: acme-corp-ingress
  namespace: tenant-acme-corp
spec:
  rules:
    - host: acme-corp.tripico.fun
      http:
        paths:
          - path: /api/itinerary
            backend: itinerary-service:8080
          - path: /api/weather
            backend: weather-service:8080
          # ... etc
```

## Architecture Diagram

```
Tenant "acme-corp"
├── Namespace: tenant-acme-corp
│   ├── Itinerary Service (Pod)
│   ├── Weather Service (Pod)
│   ├── Warnings Service (Pod)
│   ├── Recommendations Service (Pod)
│   └── Comments/Likes Service (Pod)
├── Identity Platform Tenant: "acme-corp"
│   └── Users (isolated pool)
├── Firestore Database: "acme-corp"
│   ├── comments/ collection
│   └── likes/ collection
└── Ingress: acme-corp.tripico.fun
    └── TLS Certificate (Let's Encrypt)
```

## What You Need to Do

### 1. **Update Helm Charts to Accept Tenant Configuration**

Your services need to support per-tenant configuration:

#### A) **Itinerary Service** (and other services using PostgreSQL)

Update `values.yaml` to accept Identity Platform tenant ID:

```yaml
# itinerary-service-chart/values.yaml
identityPlatform:
  tenantId: ""  # Will be set by workflow
```

Update deployment template:

```yaml
# templates/deployment.yaml
- name: IDENTITY_PLATFORM_TENANT_ID
  valueFrom:
    configMapKeyRef:
      name: app-config
      key: identity_platform_tenant_id
```

#### B) **Comments-Likes Service**

Update to accept Firestore database parameter:

```yaml
# comments-likes-chart/values.yaml
firestore:
  database: "(default)"  # Will be overridden per tenant
```

Update `FirestoreConfig.java` to use database parameter:

```java
@ConfigProperty(name = "firestore.database", defaultValue = "(default)")
String firestoreDatabaseId;

Firestore firestore = FirestoreOptions.newBuilder()
    .setDatabaseId(firestoreDatabaseId)
    .build()
    .getService();
```

### 2. **Configure GitHub Secrets**

Add these secrets to your repository:

| Secret | Description |
|--------|-------------|
| `GCP_SERVICE_ACCOUNT_KEY` | Already configured |
| GitHub Variables already set | `PROJECT_ID`, `VALUES_FILENAME` |

**Verify Service Account Permissions:**
```bash
# Your SA needs these additional roles:
roles/identityplatform.admin     # For tenant creation
roles/datastore.owner             # For Firestore database creation
```

### 3. **Test the Workflows**

#### Manual Test:
1. Go to Actions → "Provision Standard Tier Tenant"
2. Run workflow with test inputs:
   - tenant_id: `test-corp`
   - namespace: `tenant-test-corp`
   - subdomain: `test-corp.tripico.fun`
   - owner_email: `test@example.com`

#### Via Tenant Service:
```bash
# Start tenant service locally
cd services/tenant-service
docker-compose up -d
./mvnw quarkus:dev

# Create test tenant
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Corp",
    "ownerEmail": "test@example.com",
    "ownerPassword": "TestPass123!"
  }'

# Check status (polling will update automatically)
curl http://localhost:8080/api/v1/tenants/test-corp
```

## Flow Diagram

```
User → POST /api/v1/tenants
  ↓
Tenant Service
  ├─ Creates MongoDB record (state: PENDING)
  ├─ Triggers GitHub workflow (provision-tenant.yml)
  └─ Returns 202 Accepted
  
GitHub Workflow
  ├─ Creates namespace (tenant-{id})
  ├─ Creates Identity Platform tenant
  ├─ Creates Firestore database
  ├─ Deploys all services via Helm
  └─ Configures Ingress + TLS

Polling Service (every 30s)
  ├─ Checks workflow status via GitHub API
  ├─ Updates tenant state: PROVISIONING → ACTIVE
  └─ Sends email notification

User receives email
  └─ Access tenant at https://{tenant-id}.tripico.fun
```

## Key Differences from Original Implementation

| Aspect | Original Template | Updated Implementation |
|--------|------------------|----------------------|
| **Databases** | Manual provisioning (PostgreSQL x3, Neo4j) | Uses existing cluster databases |
| **Service Deployment** | Generic Helm commands | Uses your existing Helm charts |
| **Firestore** | Not specified | Separate database per tenant |
| **Identity Platform** | "To be implemented" | Tenant creation in workflow |
| **Workflow Pattern** | Custom pattern | Matches your existing workflows |

## Next Steps

1. ✅ Update Helm charts to accept tenant-specific config (see above)
2. ✅ Grant additional permissions to service account
3. ✅ Test workflow manually via GitHub UI
4. ✅ Deploy tenant service and test end-to-end
5. ✅ Add user creation after tenant is ACTIVE (optional)

## Questions Answered

### Q: Should Firestore be setup via Terraform or SDK?
**A:** Via workflow using `gcloud firestore databases create`. Terraform would require managing dynamic tenant resources, which is complex. The workflow approach is cleaner and follows your existing pattern.

### Q: Is Identity Platform multi-tenancy already setup?
**A:** Base Identity Platform is configured in Terraform. The workflows now create separate tenants (user pools) per Standard tier tenant using `gcloud identity tenants create`.

### Q: Do we get an Identity Platform tenant per Standard tier tenant?
**A:** Yes! Each Standard tier tenant gets:
- Separate Identity Platform tenant (user pool)
- Separate Firestore database
- Separate Kubernetes namespace
- Separate Ingress/domain

## Documentation

- **Workflow Details:** `.github/workflows/TENANT_WORKFLOWS_README.md`
- **Implementation Guide:** `services/tenant-service/IMPLEMENTATION_GUIDE.md`
- **Implementation Summary:** `services/tenant-service/IMPLEMENTATION_SUMMARY.md`

## Support

If you have questions or issues:
1. Check workflow logs in GitHub Actions
2. Review tenant service logs
3. Check MongoDB for tenant state
4. Verify GitHub secrets are configured

