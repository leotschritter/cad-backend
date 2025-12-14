# Multitenancy Model – Planning Overview

**Purpose**: Define tenant isolation, resource sharing, and lifecycle management strategy for a three-tier SaaS platform.

---

## Multitenancy Approach

| Tier       | Model                           | Rationale                                                                 |
|------------|---------------------------------|---------------------------------------------------------------------------|
| **Free**   | Shared Pool                     | Maximum density, minimal cost. All tenants share services, DB, and storage. |
| **Standard** | Namespace Isolation           | Dedicated deployments per tenant within shared cluster. Own subdomain.    |
| **Enterprise** | Dedicated Cluster           | Full isolation. Separate GKE cluster, complete customization capability.  |

**Core principle**: Isolation increases with tier. Free = shared everything, Standard = logical separation, Enterprise = physical separation.

---

## Architecture by Tier

### **Free Tier: Shared Everything**

**Compute & Services**
- All tenants share a single deployment of each microservice in a shared namespace
- Services handle multi-tenancy via `tenant_id` in every request (headers, JWT claims)
- No resource quotas or limits enforced per tenant
- Reduced feature set (feature flags controlled at application layer)

**Database**
- Reduced database footprint due to limited feature set:
  - **1x Shared PostgreSQL**: Single instance for core relational data (users, basic itineraries, essential features)
  - **1x Shared Firestore**: Shared document store (e.g., real-time data, flexible schemas)
  - **No Neo4j**: Advanced recommendation features not available in Free tier
  - **No additional PostgreSQL instances**: Comments/likes and other premium features not available
- All tenants' data in same database, partitioned by `tenant_id` property/column
- Row-Level Security (RLS) for PostgreSQL: `WHERE tenant_id = current_setting('app.current_tenant')`
- Firestore: Collections structured as `{collection}/{tenant_id}/{documents}` or documents tagged with `tenant_id` field

**Object Storage**
- Single shared bucket per service (e.g., `app-uploads-free-tier`)
- Objects stored with tenant prefix: `{tenant_id}/{object_key}`
- Access controlled via application logic (IAM not tenant-aware)

**Networking**
- All tenants access via same domain: `app.example.com`
- Tenant identified by session/token, not routing
- Shared ingress controller, no per-tenant TLS certificates

**Service Accounts & IAM**
- Single service account per microservice
- Services run with same credentials for all tenants
- Application-layer authorization only

**Provisioning Flow**
1. User registers → tenant record created in control plane DB
2. No infrastructure provisioning needed (instant activation)
3. Feature flags set to "free" profile

---

### **Standard Tier: Namespace-Per-Tenant**

**Compute & Services**
- Each tenant gets dedicated Kubernetes namespace: `tenant-{tenant_id}`
- Full deployment of all microservices within that namespace
- Resource quotas enforced at namespace level (CPU, memory, pod count)
- Services only handle single tenant's traffic (no `tenant_id` routing logic needed)

**Database**
- Dedicated database instances per tenant (one set per tenant):
  - **3x PostgreSQL instances**: `{tenant_id}-postgres-1`, `{tenant_id}-postgres-2`, `{tenant_id}-postgres-3`
  - **1x Neo4j instance**: `{tenant_id}-neo4j`
  - **1x Firestore database**: `{tenant_id}-firestore` (or dedicated collection structure)
- Automated provisioning via IaC (Terraform/Helm) - creates entire database stack
- Enables per-tenant backup schedules, scaling policies, and complete performance isolation
- Each service connects only to tenant's dedicated database instances

**Object Storage**
- Dedicated bucket per tenant: `{tenant_id}-uploads`
- Bucket-level IAM policies for isolation
- Per-tenant encryption keys (optional, for compliance)

**Networking**
- Unique subdomain per tenant: `{tenant_id}.app.example.com`
- Ingress routing based on hostname → directs to tenant's namespace
- Per-tenant TLS certificate (automated via cert-manager)
- Traffic stays within same cluster but logically isolated

**Service Accounts & IAM**
- Dedicated service account per tenant namespace
- Workload Identity binds K8s service account to cloud IAM
- Per-tenant least-privilege permissions to own DB and storage

**Provisioning Flow**
1. User selects Standard tier → triggers automation workflow
2. Provisioning orchestrator (e.g., Argo Workflows, custom operator) creates:
   - Kubernetes namespace
   - Database instance (via Terraform or provider API)
   - Storage bucket
   - IAM service accounts and bindings
   - Helm release with all service deployments
   - DNS record for subdomain
   - TLS certificate
3. Data migration from Free tier (if upgrade): export from shared DB → import to dedicated instance
4. Tenant activated after ~3-5 minutes (depending on DB spin-up time)

**Automation Tooling**
- **Kubernetes Operator** or **Helm Operator** watches tenant CRD (Custom Resource Definition)
- Creating a `Tenant` resource triggers full namespace provisioning
- Example CRD:
  ```yaml
  apiVersion: platform.example.com/v1
  kind: Tenant
  metadata:
    name: tenant-abc123
  spec:
    tier: standard
    subdomain: abc123.app.example.com
    features: [feature-a, feature-b]
  ```

---

### **Enterprise Tier: Dedicated Cluster**

**Compute & Services**
- Fully isolated GKE cluster per tenant
- Tenant controls cluster configuration (node types, scaling policies, regions)
- Full customization: custom service versions, feature flags, integrations

**Database**
- Dedicated database instances in tenant's cluster or managed service:
  - **3x PostgreSQL instances** (with optional HA/replication)
  - **1x Neo4j instance** (with optional cluster mode)
  - **1x Firestore database** (dedicated project/namespace)
- Tenant can choose high-availability, replication, backup strategies per database
- Full schema customization allowed (custom tables, extensions, indexes)
- Optional read replicas for reporting workloads

**Object Storage**
- Dedicated buckets with tenant-managed policies
- Optional: tenant's own cloud project/account with full ownership

**Networking**
- Custom domain support (e.g., `app.tenantcompany.com` with CNAME)
- Optional private VPC peering or VPN to tenant's infrastructure
- Tenant-managed firewall rules and network policies

**Service Accounts & IAM**
- Dedicated cloud project (optional, for billing isolation)
- Tenant-specific service accounts with custom IAM roles
- Full audit logging to tenant-owned log sinks

**Provisioning Flow**
1. Sales/onboarding team contacts ops → manual cluster provisioning initiated
2. Infrastructure team provisions via Terraform:
   - GKE cluster with tenant-specific config
   - Database instances
   - Storage buckets
   - Networking (VPC, subnets, ingress)
   - IAM setup
3. Custom Helm values applied for tenant requirements
4. Monitoring, alerting, and backup configured per tenant SLA
5. Tenant granted access to read-only dashboards and logs
6. Activation time: hours

---

## Database Strategy Details

### **Free Tier**
- **Technology**: 
  - **1x Shared PostgreSQL instance** (multi-tenant, e.g., Cloud SQL) - covers basic features only
  - **1x Shared Firestore** (multi-tenant document store) - for real-time data
  - **No Neo4j** (recommendations/graph features not available in Free tier)
  - **No additional PostgreSQL instances** (comments/likes and other premium features not available)
- **Isolation**: 
  - PostgreSQL: Row-Level Security + `tenant_id` column filtering
  - Firestore: Document-level `tenant_id` field or collection-per-tenant structure
- **Schema**: Single schema with all tenants' data mixed
- **Feature Limitation**: Reduced database footprint reflects reduced feature set (no recommendations, limited social features)
- **Concerns**: 
  - Noisy neighbor across database types
  - Backup/restore all or nothing (affects all tenants)
  - Schema migrations affect all tenants simultaneously
  - Limited feature set may encourage upgrades to Standard tier

### **Standard Tier**
- **Technology**: 
  - **3x Dedicated PostgreSQL instances** per tenant (e.g., Cloud SQL small instances)
  - **1x Dedicated Neo4j instance** per tenant
  - **1x Dedicated Firestore database/namespace** per tenant
- **Isolation**: Complete database-level isolation for each DB type
- **Schema**: Tenant owns entire schema for each database
- **Benefits**:
  - Independent scaling and performance tuning per database type
  - Per-tenant backups and point-in-time recovery for all databases
  - Schema migrations can be rolled out gradually per tenant
  - Neo4j graph operations don't interfere across tenants
  - Firestore security rules simplified (no multi-tenancy logic)
- **Concerns**:
  - Higher cost (5 database instances per tenant)
  - Management overhead (monitoring N × 5 instances)
  - Neo4j and Firestore costs can escalate quickly

### **Enterprise Tier**
- **Technology**: 
  - **3x Dedicated PostgreSQL** (HA clusters with replication)
  - **1x Dedicated Neo4j** (cluster mode with multiple replicas)
  - **1x Dedicated Firestore** (in tenant's own GCP project)
- **Isolation**: Complete physical/logical separation with optional geographic distribution
- **Schema**: Fully customizable across all databases
- **Benefits**:
  - SLA guarantees (uptime, performance) for each database
  - Custom backup schedules per database type
  - Compliance requirements (data residency, encryption at rest/transit)
  - Tenant can request direct DB access or read replicas
  - Performance isolation for graph traversals (Neo4j) and real-time operations (Firestore)
- **Advanced Options**:
  - PostgreSQL: Multi-region replication, custom extensions
  - Neo4j: Causal clustering for high availability
  - Firestore: Cross-region replication, custom indexes

---

## Object Storage Strategy

### **Free Tier**
- **Setup**: Shared bucket (e.g., `app-shared-uploads`)
- **Path**: `{tenant_id}/uploads/{filename}`
- **Access Control**: Application logic checks `tenant_id` before generating signed URLs
- **Limitations**: No per-tenant quotas, encryption, or lifecycle policies

### **Standard Tier**
- **Setup**: Dedicated bucket per tenant (`{tenant_id}-uploads`)
- **Access Control**: IAM binding to tenant's service account only
- **Features**:
  - Per-tenant storage quotas enforced by bucket policies
  - Lifecycle policies (auto-delete after N days)
  - Versioning enabled for compliance
  - Optional customer-managed encryption keys (CMEK)

### **Enterprise Tier**
- **Setup**: Dedicated bucket(s), potentially in tenant's cloud project
- **Access Control**: Tenant-managed IAM policies
- **Features**:
  - Full control over replication, versioning, retention
  - Integration with tenant's data governance tools
  - Object-level audit logging

---

## Networking & Routing

### **Free Tier**
- **Domain**: `app.example.com`
- **Routing**: Ingress → shared services → app identifies tenant via token/session
- **TLS**: Single wildcard cert for `app.example.com`

### **Standard Tier**
- **Domain**: `{tenant_id}.app.example.com`
- **Routing**: Ingress controller routes by hostname to tenant namespace
  - Example (NGINX Ingress):
    ```yaml
    host: tenant123.app.example.com
    backend:
      service:
        name: api-gateway
        namespace: tenant-tenant123
    ```
- **TLS**: Cert-manager auto-provisions Let's Encrypt certs per subdomain
- **DNS**: Wildcard A record `*.app.example.com` → cluster ingress IP

### **Enterprise Tier**
- **Domain**: Custom domain (e.g., `app.companyname.com`)
- **Routing**: Dedicated ingress in tenant's cluster
- **TLS**: Customer-provided or auto-provisioned cert
- **Options**: Private VPC, VPN, or public internet access

---

## Service Accounts & IAM

### **Free Tier**
- **K8s Service Account**: `free-tier-sa` (shared across all tenants)
- **Cloud IAM**: Single service account with broad permissions to shared resources
- **Access**: Application enforces authorization logic

### **Standard Tier**
- **K8s Service Account**: One per tenant namespace (`tenant-{tenant_id}-sa`)
- **Cloud IAM**: Workload Identity binds K8s SA → Cloud IAM SA
- **Permissions**: Least-privilege access to tenant's DB, bucket, and secrets
- **Example**:
  - `tenant-123-sa@project.iam.gserviceaccount.com`
  - Permissions: `roles/cloudsql.client` on `db-tenant-123`, `roles/storage.objectAdmin` on `tenant-123-uploads`

### **Enterprise Tier**
- **K8s Service Account**: Tenant-managed (multiple per workload if needed)
- **Cloud IAM**: Tenant's own project with custom IAM roles
- **Permissions**: Fully customizable, audited via Cloud Logging

---

## Tenant Provisioning Workflows

### **Free Tier Provisioning**
1. User completes signup form
2. Control plane API creates tenant record in central DB
3. Initial user account created
4. Tenant activated immediately (no infrastructure to provision)
5. User logs in → services filter data by `tenant_id`

**Time to activation**: < 1 minute

---

### **Standard Tier Provisioning**

**Trigger**: User selects "Upgrade to Standard" in UI

**Automated Steps**:
1. **Control Plane** creates `Tenant` custom resource in K8s
2. **Tenant Operator** watches for new `Tenant` CR and executes:
   - Create namespace: `kubectl create namespace tenant-{id}`
   - Apply resource quotas and network policies
   - **Database Provisioning** (5 instances total):
     - Provision **3x PostgreSQL instances** (via `gcloud sql instances create` or Terraform)
     - Provision **1x Neo4j instance** (via marketplace or custom deployment)
     - Provision **1x Firestore database** (create database in Firestore project)
     - Wait for all DBs to be ready (~3-5 min depending on Neo4j)
     - Initialize schemas, create users, apply migrations to each database
   - **Storage Provisioning**:
     - Create bucket via cloud API
     - Configure IAM policies
   - **IAM Setup**:
     - Create cloud service account
     - Bind K8s SA → Cloud SA via Workload Identity
   - **Service Deployment**:
     - Deploy Helm chart with tenant-specific values
     - Inject DB connection strings, storage bucket names via ConfigMap/Secrets
   - **Networking**:
     - Create Ingress resource with subdomain
     - Cert-manager provisions TLS cert
     - Update DNS if needed (or rely on wildcard)
3. **Data Migration** (if upgrading from Free):
   - Export tenant's data from shared DB (via `pg_dump` with `WHERE tenant_id = X`)
   - Import into new dedicated DB
   - Run validation checks
   - Switch tenant's routing to new namespace
4. **Status Update**: Mark tenant as "active" in control plane
5. **Notification**: Email user that their Standard tier is ready

**Time to activation**: 3-5 minutes

**Error Handling**:
- Operator retries failed steps (idempotent operations)
- Logs errors to monitoring system
- Rolls back on critical failures (deletes partially created resources)

---

### **Enterprise Tier Provisioning**

**Trigger**: Sales/CS team receives customer request

**Manual Steps**:
1. Requirements gathering (features, SLA, compliance, region, custom domain)
2. Cost estimation and contract finalization
3. **Infrastructure Provisioning** (via Terraform):
   ```
   terraform apply -var="tenant_id=acme-corp" -var="region=us-east1"
   ```
   - Provisions GKE cluster, VPC, subnets, firewall rules
   - Database instances (HA setup)
   - Storage buckets
   - IAM roles and service accounts
4. **Application Deployment**:
   - Deploy services via Helm with custom values file
   - Configure feature flags, integrations, SSO
5. **Monitoring Setup**:
   - Tenant-specific dashboards in Grafana
   - Alerting rules sent to tenant's on-call
   - Log shipping to tenant's SIEM (if required)
6. **Handoff**:
   - Provide tenant with access credentials
   - Share runbooks and escalation paths
   - Schedule onboarding call

**Time to activation**: hours

---

## Cost & Telemetry Impact

### **Cost Attribution**

**Free Tier**:
- No per-tenant cost tracking
- Aggregate costs for entire free tier pool
- Useful for internal budget planning only

**Standard Tier**:
- **Compute**: Namespace-level resource usage tracked via K8s metrics (CPU, memory)
- **Database**: Per-instance billing directly visible in cloud bill
- **Storage**: Per-bucket billing and usage metrics
- **Network**: Egress traffic attributable to tenant namespace
- **Total Cost per Tenant**: Easily calculated for chargeback or pricing adjustments

**Enterprise Tier**:
- Full cost transparency (dedicated cluster = dedicated line item in billing)
- Tenant can receive detailed billing reports
- Enables accurate TCO analysis

### **Telemetry & Observability**

**Key Requirement**: All metrics, logs, and traces must include tenant identifier

**Free Tier**:
- Logs include `tenant_id` field for filtering
- Metrics tagged with `tenant_id` label
- Used for abuse detection, not billing

**Standard Tier**:
- Namespace-scoped metrics automatically isolated
- Prometheus queries: `sum by (namespace) (container_cpu_usage_seconds_total{namespace=~"tenant-.*"})`
- Per-tenant dashboards showing resource consumption
- Alerts on quota limits or performance degradation

**Enterprise Tier**:
- Dedicated monitoring stack per cluster (or shared aggregation)
- Tenant receives read-only access to Grafana dashboards
- Custom SLOs/SLIs tracked per tenant

---

## Feature Differentiation

| Feature Category       | Free                     | Standard                  | Enterprise               |
|------------------------|--------------------------|---------------------------|--------------------------|
| **Subdomain**          | Shared (`app.com`)       | Dedicated (`X.app.com`)   | Custom domain            |
| **Databases**          | 1 PostgreSQL + 1 Firestore (shared) | 3 PostgreSQL + Neo4j + Firestore (dedicated) | Full stack with HA |
| **Features Available** | Basic only (no recommendations, limited social) | Full feature set | Full + custom features |
| **API Rate Limits**    | Low (e.g., 100 req/min)  | Medium (e.g., 1000/min)   | Custom/unlimited         |
| **Storage Quota**      | 1 GB                     | 50 GB (configurable)      | Unlimited                |
| **Uptime SLA**         | Best effort              | 99.5%                     | 99.9% or custom          |
| **Support**            | Community/email          | Email + ticket            | Dedicated account manager|
| **Customization**      | None                     | Limited (feature toggles) | Full (code, schema, infra)|
| **Backup Retention**   | 7 days (shared)          | 30 days (dedicated)       | Custom (e.g., 1 year)    |
| **Data Export**        | Manual via UI            | API + scheduled exports   | Direct DB access         |

---

## User Management & Tenant Administration

### **Current State: Google Identity Platform with Multi-Tenancy**

**Architecture Decision**: Use Identity Platform's built-in [multi-tenancy feature](https://cloud.google.com/identity-platform/docs/multi-tenancy)

**What Identity Platform Handles**:
- User authentication (login/logout, password management)
- User identity storage (email, name, profile)
- Token issuance (JWT with user claims and tenant context)
- **Tenant isolation at authentication layer** - each tenant gets own user pool
- Email verification and password reset flows
- **Authentication Method**: Email + password only (all tiers)

**What Identity Platform Multi-Tenancy Provides**:
- Separate user namespace per tenant (users in tenant A cannot see users in tenant B)
- Tenant-specific authentication configuration
- Tenant ID embedded in JWT tokens automatically
- Simplified user provisioning per tenant

**What Identity Platform Does NOT Handle**:
- Tenant-level roles and permissions (tenant admin vs. regular user)
- Application-level authorization (what users can do within a tenant)
- Tenant ownership and administration workflows
- Feature-based access control

### **Tenant User Management Strategy**

**Approach**: Identity Platform multi-tenancy for authentication + custom RBAC for authorization

**Architecture Overview**:
```
┌─────────────────────────────┐
│ Identity Platform           │ → Authentication per tenant
│ Multi-Tenancy Feature       │   (separate user pools)
│                             │
│ ┌─────────┐ ┌─────────┐   │
│ │Tenant A │ │Tenant B │   │
│ │Users    │ │Users    │   │
│ └─────────┘ └─────────┘   │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────┐
│ Application DB              │ → Authorization (roles/permissions)
│ (user_tenants table)        │
└─────────────────────────────┘
```

**Key Benefits**:
- Each tenant has isolated user pool in Identity Platform
- No risk of user email conflicts across tenants
- Tenant context automatically included in JWT
- Simplified user provisioning (create user directly in tenant's Identity Platform namespace)

**Database Schema (PostgreSQL)**:
```sql
-- Users table (mirrors Identity Platform users)
CREATE TABLE users (
  id UUID PRIMARY KEY,
  firebase_uid VARCHAR(128) UNIQUE NOT NULL,  -- Links to Identity Platform
  email VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Tenants table
CREATE TABLE tenants (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  tier VARCHAR(20) NOT NULL,  -- 'free', 'standard', 'enterprise'
  created_at TIMESTAMP DEFAULT NOW()
);

-- User-Tenant association with roles
CREATE TABLE user_tenants (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  tenant_id UUID REFERENCES tenants(id),
  role VARCHAR(50) NOT NULL,  -- 'owner', 'admin', 'member', 'readonly'
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(user_id, tenant_id)
);
```

### **Role-Based Access Control (RBAC)**

**Proposed Roles per Tenant**:

| Role       | Permissions                                                                 |
|------------|-----------------------------------------------------------------------------|
| **Owner**  | Full control. Can delete tenant, manage billing, add/remove admins.         |
| **Admin**  | Manage users, configure settings, access all data. Cannot delete tenant.    |
| **Member** | Standard user access. Can create/edit own data, view shared data.           |
| **ReadOnly** | View-only access. Cannot modify data. Useful for auditors/observers.      |

**Permission Examples**:
- `tenant:delete` → Owner only
- `tenant:billing:manage` → Owner only
- `tenant:users:invite` → Owner, Admin
- `tenant:users:remove` → Owner, Admin
- `tenant:settings:update` → Owner, Admin
- `tenant:data:write` → Owner, Admin, Member
- `tenant:data:read` → All roles

### **Authentication Flow**

1. **User logs in** via Identity Platform with tenant-specific endpoint
   - Login URL includes tenant identifier: `https://[PROJECT_ID].firebaseapp.com/?tenantId={tenant_id}`
   - User provides email + password
   - Identity Platform validates against tenant's user pool
   - Returns JWT with `firebase_uid` and `tenant_id` automatically embedded
   
2. **Backend validates JWT** and extracts both `firebase_uid` and `tenant_id`
   - Tenant context is already in the token (no additional lookup needed for tenant identification)

3. **Lookup user role** within tenant:
   ```sql
   SELECT ut.role
   FROM user_tenants ut
   WHERE ut.user_id = (SELECT id FROM users WHERE firebase_uid = ?)
     AND ut.tenant_id = ?
   ```

4. **Inject tenant context** into request:
   - `tenant_id` already available from JWT
   - All downstream services filter by this `tenant_id`

5. **Authorize action** based on role:
   ```javascript
   if (action === 'delete_tenant' && user.role !== 'owner') {
     throw ForbiddenError('Only tenant owner can delete tenant');
   }
   ```

**Simplified Flow**: Identity Platform multi-tenancy eliminates the need to manually track which tenant a user belongs to at authentication time - it's built into the login flow.

### **Tenant Administration by Tier**

**Free Tier**:
- Single-user tenant (user who signed up is automatically the owner)
- No ability to invite other users
- User manages only their own data

**Standard Tier**:
- Multi-user support enabled
- Owner/Admin can invite users via email
- Invited users receive email → create Identity Platform account → linked to tenant
- Role assignment at invitation time
- Use case: Small team (5-20 users) working on shared itineraries

**Enterprise Tier**:
- Full multi-user support with advanced features:
  - Custom roles beyond the 4 standard ones
  - Audit logs of all user actions
  - Dedicated support for bulk user management
  - **Authentication**: Email + password (same as other tiers)
- Use case: Large organization (100+ users) with compliance requirements

### **Implementation Approach**

**Selected: Identity Platform Multi-Tenancy + Custom RBAC** ✅

**Architecture**:
- **Identity Platform Multi-Tenancy** handles:
  - Tenant-isolated user pools
  - Authentication (email + password)
  - Tenant ID embedded in JWT automatically
  - User identity management per tenant
  
- **Custom RBAC** handles:
  - Role assignment (Owner, Admin, Member, ReadOnly)
  - Permission checks in application middleware
  - User invitation workflows
  - Tenant administration UI

**Benefits**:
- Clean separation: authentication (Identity Platform) vs. authorization (application)
- No user email conflicts across tenants
- Tenant context always available in JWT
- Flexibility to add custom roles and permissions
- Scales well across all tiers

**Implementation**:
- Store roles in application database (see schema above)
- Middleware extracts `tenant_id` from JWT, queries role from database
- Control plane service manages user invitations and role assignments
- Each tenant gets own Identity Platform tenant instance

### **Open Questions for Clarification**

1. **Multi-Tenant Users**: Should Standard/Enterprise users be able to belong to multiple tenants (e.g., consultant working for 3 companies)?
   - If YES → User must select active tenant on login, session stores `current_tenant_id`
   - If NO → Simpler, one user = one tenant, easier to manage

2. **User Invitations**: How should invitations work?
   - Email invitation with signup link?
   - Admin pre-creates account and user sets password on first login?

3. **Tenant Ownership Transfer**: Can Free tier user transfer ownership to another user? Or does "tenant" = "personal account"?

4. **Role Granularity**: Are 4 roles enough, or need feature-level permissions (e.g., "can manage itineraries" vs "can manage comments")?


### **Recommended Implementation Phases**

For a student project context with gradual complexity:

**Phase 1 (MVP - Free Tier Only)**:
- Identity Platform multi-tenancy for authentication
- Create one Identity Platform tenant per application tenant
- Single user per tenant (no multi-user support)
- No RBAC needed (user is implicitly owner of their tenant)
- `tenant_id` automatically included in JWT

**Phase 2 (Standard Tier - Multi-User)**:
- Add `user_tenants` table with roles
- Implement invitation flow (email invite → signup in tenant's Identity Platform namespace)
- Add role checks in backend middleware
- Build admin UI for user management

**Phase 3 (Enterprise Tier - Advanced)**:
- Add audit logging
- Add custom role definitions
- Bulk user management UI
- Enhanced compliance features

This phased approach lets you start simple and add complexity as needed.

---

## Key Assumptions

1. **Cluster Capacity**: Standard tier assumes a single shared cluster can handle 100-500 tenant namespaces before requiring additional clusters.
2. **Database Limits**: Cloud provider supports creating and managing hundreds of small DB instances cost-effectively.
3. **Automation Maturity**: Kubernetes operator or equivalent automation exists to handle Standard tier provisioning.
4. **Free → Standard Upgrade**: Majority of free users will not upgrade, so migration volume is manageable.
5. **Enterprise Volume**: Low number of Enterprise tenants (< 10 initially) makes manual provisioning acceptable.

---

## Open Questions

1. **Standard Tier Database Sizing**: What are default resource allocations (vCPU, RAM, storage) for new Standard tenant DBs?
2. **Namespace Quotas**: What are safe default CPU/memory quotas per Standard tenant to prevent cluster resource exhaustion?
3. **Standard Tier Auto-Scaling**: Should tenant namespaces auto-scale (HPA), or fixed resource limits?
4. **Monitoring Overhead**: At what Standard tenant count does monitoring and logging become cost-prohibitive?
5. **Free Tier Abuse Prevention**: How to detect and throttle malicious free tier usage before it impacts shared resources?
6. **Backup Strategy**: Automated backup schedules for Standard tier DBs—daily, weekly, or configurable per tenant?
7. **Disaster Recovery**: What's the RTO/RPO for Standard tier? Can tenants restore to arbitrary point-in-time?
8. **Secret Management**: How to securely distribute DB credentials and API keys to tenant namespaces (Vault, K8s Secrets, external secret operator)?
9. **Multi-Region**: Should Standard tier support regional selection? Or all in one region initially?
10. **Enterprise Control Plane**: Should Enterprise tenants share the central control plane (authentication, billing) or fully isolated?
11. **User-Tenant Model**: Should Standard/Enterprise users be able to belong to multiple tenants simultaneously?

---

## Next Steps

1. **Prototype Standard Tier Provisioning**:
   - Build basic Kubernetes operator or Helm-based automation
   - Test end-to-end flow: tenant creation → namespace + DB + storage + ingress
   - Measure provisioning time and failure modes

2. **Define Resource Quotas**:
   - Benchmark application resource usage per tenant
   - Set initial CPU/memory quotas for Standard namespaces

3. **Database Instance Sizing**:
   - Determine minimum viable DB instance size for Standard tier
   - Evaluate cost vs. performance trade-offs

4. **Cost Modeling**:
   - Calculate cost per Standard tenant (DB + storage + compute)
   - Set pricing to ensure margin

5. **Implement Tenant-Aware Observability**:
   - Ensure all services emit `tenant_id` in logs and metrics
   - Build Grafana dashboards for per-tenant resource consumption

6. **Design Data Migration Process**:
   - Build tooling for Free → Standard data export/import
   - Test with sample tenant data

7. **Document Runbooks**:
   - Standard tier provisioning failures and troubleshooting
   - Enterprise tier onboarding checklist

---

**Document Status**: Planning Draft  
**Owner**: Cloud Architecture Team  
**Last Updated**: 2025-12-13

