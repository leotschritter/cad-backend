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
- No resource quotas or limits enforced per tenant
- Reduced feature set (feature flags controlled at application layer)
  - Itinerary creation, social interaction
  - no recommendations, no travel warnings and no weather information

**Database**
- Reduced database footprint due to limited feature set:
  - **PostgreSQL**: Single instance for core relational data (users, itineraries)
  - **Firestore**: Shared document store (comments, likes)
- All tenants' data in same database

**Object Storage**
- Single shared bucket per service (e.g., `app-uploads-free-tier`)
- Objects stored with tenant prefix: `{tenant_id}/{object_key}`
- Access controlled via application logic (IAM not tenant-aware)

**Service Accounts & IAM**
- Single service account per microservice
- Services run with same credentials for all tenants
- Application-layer authorization only

**Provisioning Flow**
1. User registers → tenant record created in control plane DB
2. No infrastructure provisioning needed (instant activation)

---

### **Standard Tier: Namespace-Per-Tenant**

**Compute & Services**
- Each tenant gets dedicated Kubernetes namespace: `tenant-{tenant_id}`
- Full deployment of all microservices within that namespace
- Resource quotas enforced at namespace level (CPU, memory, pod count)
- Services only handle single tenant's traffic (no `tenant_id` routing logic needed)

**Database**
- Dedicated database instances per tenant (one set per tenant):
  - **3x PostgreSQL instances**: `{tenant_id}-itinerary-postgres`, `{tenant_id}-weather-postgres`, `{tenant_id}-warnings-postgres`
  - **1x Neo4j instance**: `{tenant_id}-neo4j`
  - **1x Firestore database**: `{tenant_id}-firestore` (or dedicated collection structure)
- Automated provisioning via IaC (Terraform/Helm) - creates entire database stack

**Object Storage**
- Dedicated bucket per tenant: `{tenant_id}-uploads`
- Bucket-level IAM policies for isolation

**Networking**
- Unique subdomain per tenant: `{tenant_id}.tripico.fun`
- Ingress routing based on hostname → directs to tenant's namespace

**Service Accounts & IAM**
- Dedicated service account per tenant namespace
- Workload Identity binds K8s service account to cloud IAM
- Per-tenant least-privilege permissions to own DB and storage

**Provisioning Flow**
1. User selects Standard tier → triggers automation workflow
2. Provisioning triggers pipeline that creates:
   - Kubernetes namespace
   - Database instance (via Terraform or provider API)
   - Storage bucket
   - IAM service accounts and bindings
   - Helm release with all service deployments
3. Tenant activated after a few minutes (depending on DB spin-up time)

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

**Object Storage**
- Dedicated bucket per tenant: `{tenant_id}-uploads`
- Bucket-level IAM policies for isolation

**Networking**
- Unique subdomain per tenant: `{tenant_id}.tripico.fun`
- Ingress routing based on hostname → directs to tenant's namespace

**Service Accounts & IAM**
- Dedicated service account per tenant namespace
- Workload Identity binds K8s service account to cloud IAM
- Per-tenant least-privilege permissions to own DB and storage

**Provisioning Flow**
1. Customer requests Enterprise tier with customizations
2. Infrastructure team provisions via Terraform:
   - GKE cluster with tenant-specific config
   - Database instances
   - Storage buckets
   - IAM setup
3. Custom Helm values applied for tenant requirements
4. Activation time: hours

---

## Cost Attribution

**Free Tier**:
- No per-tenant cost tracking
- Aggregate costs for entire free tier pool
- Useful for internal budget planning only

**Standard Tier**:
- **Kubernetes**: Namespace-level resource usage (CPU, memory)
- **Database**: Per-instance billing directly visible in cloud bill
- **Storage**: Per-bucket billing and usage metrics
- **Network**: Egress traffic attributable to tenant namespace

**Enterprise Tier**:
- **Kubernetes**: Cluster-level resource usage (node hours, CPU, memory)
- **Database**: Per-instance billing directly visible in cloud bill
- **Storage**: Per-bucket billing and usage metrics
- **Network**: Egress traffic attributable to tenant namespace

---

## User Management & Tenant Administration

### **Google Identity Platform with Multi-Tenancy**

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