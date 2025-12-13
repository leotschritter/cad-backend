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

