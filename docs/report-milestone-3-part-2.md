## 3 Development View

### 3.1 Software Components

#### Repository Organization

**Backend Monorepo:** https://github.com/leotschritter/cad-backend

```
cad-backend/
├── .github/workflows/           # CI/CD pipelines
│   ├── terraform.yml           # Infrastructure provisioning (tenant creation)
│   ├── build-and-push.yml      # Docker image builds
│   └── deploy-multi-namespace.yml  # Helm deployments per tenant
├── terraform/
│   ├── environment/
│   │   ├── free/               # Freemium tier infrastructure
│   │   ├── standard/           # Standard tier tenant template
│   │   └── enterprise/         # Enterprise tier tenant template
│   └── modules/
│       ├── api-gateway/        # API Gateway configuration
│       ├── artifact-registry/  # Container registry
│       ├── dns/                # DNS zone and records
│       ├── firestore/          # Firestore database
│       ├── gke/                # GKE Autopilot cluster
│       ├── iam/                # Service accounts and bindings
│       ├── project/            # GCP APIs and Identity Platform
│       └── storage/            # Cloud Storage buckets
├── services/
│   ├── itinerary-service/      # Core itinerary management
│   ├── recommendation-service/ # Neo4j-based recommendations
│   ├── comments-likes-service/ # Social interactions
│   ├── travel_warnings/        # Travel safety alerts
│   └── weather-forecast-service/  # Weather integration
├── load/                       # Locust load testing suite
└── docs/                       # Documentation and diagrams
```

**Frontend Repository:** https://github.com/leotschritter/cad-frontend

#### Software Components Overview

| Component | Language | Framework | Purpose |
|-----------|----------|-----------|---------|
| **Itinerary Service** | Java 21 | Quarkus 3.28.1 | Core itinerary and user management |
| **Recommendation Service** | Java 21 | Quarkus 3.x | Graph-based recommendations |
| **Comments & Likes Service** | Java 21 | Quarkus 3.x | Social interactions |
| **Travel Warnings Service** | Java 21 | Quarkus 3.29.3 | Travel safety alerts |
| **Weather Forecast Service** | Java 21 | Quarkus 3.x | Weather data integration |
| **Frontend** | TypeScript | Vue.js 3 + Vuetify | User interface |

#### Key Libraries

**Backend Services (Common):**
- Quarkus REST - RESTful web services
- Firebase Admin SDK - Multi-tenant authentication
- Hibernate ORM with Panache - PostgreSQL persistence
- MapStruct - DTO mapping
- SmallRye OpenAPI - API documentation

**Service-Specific:**
- **Recommendation:** Neo4j Java Driver 5.x
- **Comments & Likes:** Google Cloud Firestore SDK
- **Travel Warnings:** Quarkus Mailer, Scheduler, Caffeine Cache
- **Weather:** Quarkus REST Client (Meteosource API)

---

## 4 DevOps

### 4.1 Environments and Initial Infrastructure Setup

#### Starting from Blank GCP Project

To set up Tripico infrastructure from a blank GCP project:

**1. Prerequisites:**
```bash
# Enable billing on GCP project
# Create Terraform state bucket
gsutil mb -l europe-west1 gs://${PROJECT_ID}-terraform-state

# Create service account for CI/CD
gcloud iam service-accounts create terraform-sa \
  --display-name="Terraform Service Account"

# Grant required roles
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:terraform-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/owner"
```

**2. GitHub Secrets Configuration:**
```
GCP_SERVICE_ACCOUNT_KEY: (Production project key)
GCP_SERVICE_ACCOUNT_KEY_2: (Development project key)
```

**3. Initial Infrastructure Deployment:**
```bash
# Deploy base infrastructure (free tier)
# This creates: GKE cluster, VPC, DNS zone, Artifact Registry, Firestore, IAM

# Via GitHub Actions workflow_dispatch:
# - environment: free
# - action: apply

# Or manually:
cd terraform/environment/free
terraform init -backend-config="bucket=${TF_STATE_BUCKET}" \
               -backend-config="prefix=terraform/state/free"
terraform apply -var-file="terraform.tfvars"
```

**4. Infrastructure Created by Free Tier:**

| Resource | Name | Purpose |
|----------|------|---------|
| GKE Cluster | tripico-cluster | Shared Kubernetes cluster |
| VPC Network | tripico-network | Shared network |
| Static IP | tripico-ingress-ip | Shared ingress IP |
| DNS Zone | tripico-fun-zone | DNS management |
| Artifact Registry | docker-repo | Container images |
| Firestore | (default) | Document database |
| Identity Platform | Project config | Multi-tenant auth |
| Service Account | tripico-sa | Workload identity |

### 4.2 Pipelines and Release of new Features

#### Branching Strategy

```
main (production)
├── develop (staging/dev environment)
│   ├── feature/new-feature
│   ├── feature/tenant-terraform
│   └── bugfix/fix-issue
└── hotfix/critical-fix
```

#### CI/CD Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CI/CD PIPELINE FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐             │
│  │  Commit  │───>│  Build   │───>│  Test    │───>│  Push    │             │
│  │  to Git  │    │  Docker  │    │  Image   │    │  to AR   │             │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘             │
│       │                                               │                    │
│       │                                               v                    │
│       │         ┌─────────────────────────────────────────────┐           │
│       │         │          deploy-multi-namespace.yml         │           │
│       │         ├─────────────────────────────────────────────┤           │
│       │         │                                             │           │
│       │         │  ┌──────────────┐  ┌──────────────┐        │           │
│       │         │  │   Freemium   │  │   Standard   │        │           │
│       │         │  │  Namespace   │  │  Namespace   │        │           │
│       │         │  │  helm deploy │  │  helm deploy │        │           │
│       │         │  └──────────────┘  └──────────────┘        │           │
│       │         │                                             │           │
│       │         └─────────────────────────────────────────────┘           │
│       │                                                                    │
│       v                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐         │
│  │                    terraform.yml                              │         │
│  │  (Manual trigger for tenant creation)                        │         │
│  │                                                              │         │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                      │         │
│  │  │  Free   │  │Standard │  │Enterprise│                      │         │
│  │  │  Tier   │  │  Tier   │  │  Tier   │                      │         │
│  │  │(shared) │  │(tenant) │  │(cluster)│                      │         │
│  │  └─────────┘  └─────────┘  └─────────┘                      │         │
│  └──────────────────────────────────────────────────────────────┘         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Workflow Files

**1. build-and-push.yml** - Builds and pushes Docker images
- Triggers: Push to main/develop with service path changes
- Builds: Multi-arch Docker images
- Pushes: To Artifact Registry with commit SHA tags

**2. deploy-multi-namespace.yml** - Deploys services to namespaces
- Triggers: After successful image build
- Deploys: To freemium and standard namespaces
- Uses: Helm charts with environment-specific values

**3. terraform.yml** - Infrastructure provisioning
- Triggers: Manual workflow_dispatch, or terraform/ path changes
- Actions: plan, apply, destroy, validate
- Supports: free, standard, enterprise environments

### 4.3 Creation of new Tenants

#### Tenant Creation Process

**Freemium Tenant (Automatic)**
- Users sign up without tenant context
- Authentication at project level (no tenant ID)
- Shared resources in `freemium` namespace
- No manual steps required

**Standard Tenant Creation:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    STANDARD TENANT CREATION FLOW                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. TRIGGER TERRAFORM WORKFLOW                                              │
│     ┌────────────────────────────────────────────────────────────────┐     │
│     │  GitHub Actions → workflow_dispatch                            │     │
│     │  - environment: standard                                       │     │
│     │  - action: apply                                               │     │
│     │  - tenant_number: 1 (or 2, 3, ...)                            │     │
│     └────────────────────────────────────────────────────────────────┘     │
│                                      │                                      │
│                                      v                                      │
│  2. TERRAFORM CREATES RESOURCES                                             │
│     ┌────────────────────────────────────────────────────────────────┐     │
│     │  - Identity Platform Tenant (std-standard-1)                   │     │
│     │  - Workload Identity Bindings (for tenant namespace)           │     │
│     │  - Storage Bucket (tenant-specific)                           │     │
│     │  - API Gateway Configuration                                   │     │
│     │  - State stored: terraform/state/standard-1                   │     │
│     └────────────────────────────────────────────────────────────────┘     │
│                                      │                                      │
│                                      v                                      │
│  3. DEPLOY SERVICES (Manual or CI/CD)                                       │
│     ┌────────────────────────────────────────────────────────────────┐     │
│     │  helm upgrade --install itinerary-service \                    │     │
│     │    -n standard-1 --create-namespace \                          │     │
│     │    --set identityPlatform.tenantId=<tenant-id>                │     │
│     │    -f values-standard-prod.yaml                               │     │
│     └────────────────────────────────────────────────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Enterprise Tenant Creation:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   ENTERPRISE TENANT CREATION FLOW                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. TRIGGER TERRAFORM WORKFLOW                                              │
│     ┌────────────────────────────────────────────────────────────────┐     │
│     │  GitHub Actions → workflow_dispatch                            │     │
│     │  - environment: enterprise                                     │     │
│     │  - action: apply                                               │     │
│     │  - tenant_number: 1                                            │     │
│     │  - tenant_name: "acme-corp" (optional custom name)            │     │
│     └────────────────────────────────────────────────────────────────┘     │
│                                      │                                      │
│                                      v                                      │
│  2. TERRAFORM CREATES FULL INFRASTRUCTURE                                   │
│     ┌────────────────────────────────────────────────────────────────┐     │
│     │  - Dedicated GKE Cluster (tripico-acme-corp-cluster)          │     │
│     │  - Dedicated VPC Network (tripico-acme-corp-network)          │     │
│     │  - Dedicated Static IP                                         │     │
│     │  - DNS Wildcard (*.acme-corp.tripico.fun)                     │     │
│     │  - Identity Platform Tenant (ent-acme-corp)                   │     │
│     │  - Dedicated Storage Bucket                                    │     │
│     │  - Workload Identity Bindings                                  │     │
│     │  - NGINX Ingress Controller (Helm)                            │     │
│     │  - API Gateway Configuration                                   │     │
│     │  - State stored: terraform/state/acme-corp                    │     │
│     └────────────────────────────────────────────────────────────────┘     │
│                                      │                                      │
│                                      v                                      │
│  3. DEPLOY SERVICES (All services including shared)                         │
│     ┌────────────────────────────────────────────────────────────────┐     │
│     │  # Connect to enterprise cluster                               │     │
│     │  gcloud container clusters get-credentials \                   │     │
│     │    tripico-acme-corp-cluster --region europe-west1            │     │
│     │                                                                │     │
│     │  # Deploy all services (including weather, warnings)          │     │
│     │  helm upgrade --install itinerary-service \                    │     │
│     │    --set identityPlatform.tenantId=<tenant-id>                │     │
│     │    -f values-enterprise-prod.yaml                             │     │
│     └────────────────────────────────────────────────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Terraform Workflow Configuration

```yaml
# .github/workflows/terraform.yml
workflow_dispatch:
  inputs:
    action:
      type: choice
      options: [plan, apply, destroy, validate]
    environment:
      type: choice
      options: [free, standard, enterprise]
    tenant_number:
      type: string
      default: '1'
    tenant_name:
      type: string
      description: 'Custom name for enterprise (e.g., acme-corp)'
```

#### Manual Steps Required

| Step | Freemium | Standard | Enterprise |
|------|----------|----------|------------|
| Trigger Terraform | N/A (base infra) | Manual | Manual |
| Deploy Helm Charts | Automatic (CI/CD) | Manual/CI | Manual |
| Configure Frontend | N/A | Update tenant config | Update tenant config |
| DNS Propagation | N/A | Automatic (wildcard) | Automatic (~5 min) |
| Get Tenant ID | N/A | From Terraform output | From Terraform output |

#### Terraform Outputs for Tenant Onboarding

```hcl
# Standard/Enterprise outputs
output "identity_platform_tenant_id" {
  description = "The Identity Platform tenant ID for backend configuration"
  value       = google_identity_platform_tenant.tenant.name
}

output "tenant_subdomain" {
  description = "The subdomain for this tenant"
  value       = local.tenant_subdomain
}

output "api_gateway_url" {
  description = "The API Gateway URL for this tenant"
  value       = module.api_gateway.gateway_url
}
```

### 4.4 Monitoring

#### Health Monitoring

**Kubernetes Health Checks:**

All services implement Quarkus SmallRye Health endpoints:

```yaml
# Deployment health probes
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5

startupProbe:
  httpGet:
    path: /q/health/started
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 30
```

**GKE Autopilot Built-in Monitoring:**
- Node health automatically managed
- Pod scheduling optimized
- Resource scaling automatic

#### Logging

**Cloud Logging Integration:**

All GKE workloads automatically send logs to Google Cloud Logging:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        LOGGING ARCHITECTURE                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│  │  Pod Logs   │────>│ GKE Agent   │────>│Cloud Logging│                  │
│  │  (stdout)   │     │ (Fluentd)   │     │             │                  │
│  └─────────────┘     └─────────────┘     └─────────────┘                  │
│                                                 │                          │
│                                                 v                          │
│                                          ┌─────────────┐                  │
│                                          │ Log Explorer│                  │
│                                          │  (Console)  │                  │
│                                          └─────────────┘                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Log Queries:**

```
# Query logs for specific tenant namespace
resource.type="k8s_container"
resource.labels.namespace_name="standard-1"
resource.labels.container_name="itinerary-service"

# Query error logs across all services
resource.type="k8s_container"
severity>=ERROR

# Query logs for specific user request (trace ID)
resource.type="k8s_container"
jsonPayload.traceId="abc123"
```

**Log Retention:**
- Default: 30 days
- Can be extended via Log Router sinks to Cloud Storage

#### Alerting

**GCP Cloud Monitoring Alerts:**

| Alert | Condition | Action |
|-------|-----------|--------|
| Pod CrashLoopBackOff | Restart count > 5 in 10 min | Email notification |
| High Error Rate | 5xx responses > 5% | Email notification |
| High Latency | P95 > 5s for 5 min | Email notification |
| Node Not Ready | Node unhealthy > 5 min | Auto-remediation (GKE) |

**HPA Scaling Events:**
- Logged automatically in Cloud Logging
- Can trigger alerts on scale-up events

---

## 5 Performance Tests

*Performance tests from Milestone 2 remain applicable. The multi-tenant architecture was tested with the same load testing suite. Key findings:*

### 5.1 Periodic Workload

**Scenario 1: 100 Concurrent Users**
- **Result:** Excellent performance (0.051% failure rate)
- **Avg Response Time:** 244 ms
- **Throughput:** 33 req/s
- **No auto-scaling triggered**

**Scenario 2: 1000 Concurrent Users**
- **Result:** Severe degradation (22.32% failure rate)
- **Root Cause:** Cloud SQL connection pool exhaustion
- **Recommendation:** Upgrade to production-tier Cloud SQL

### 5.2 Once-in-a-Lifetime Workload

**Viral Traffic Simulation (2000 users, gradual ramp):**
- **Breaking Point:** ~900 concurrent users
- **Graceful Degradation:** System survived but with 65s response times
- **Failure Rate:** 0.66% (better than sudden spike)
- **Recommendation:** Implement request queue limits and optimize auto-scaling

---

## 6 Commercial Model

### 6.1 Tenant Types

#### Comparison Matrix

| Aspect | Freemium | Standard | Enterprise |
|--------|----------|----------|------------|
| **Target** | Individual users | SMBs | Large organizations |
| **Pricing** | Free | EUR 199/month | EUR 999/month |
| **Infrastructure** | Shared cluster, shared namespace | Shared cluster, dedicated namespace | Dedicated cluster |
| **User Isolation** | Project-level auth | Tenant-level auth | Tenant-level auth |
| **Data Isolation** | Shared collections | Tenant-prefixed collections | Fully isolated |
| **Max Users** | Unlimited (rate-limited) | 1,000 | Unlimited |
| **Itineraries** | 10 per user | Unlimited | Unlimited |
| **Auto-scaling** | OFF | ON (max 2 replicas) | ON (max 10 replicas) |
| **Shared Services** | Yes (weather, warnings) | Yes | No (dedicated) |
| **SLA** | None | 99% uptime | 99.9% uptime |
| **Support** | Community | Email (24h response) | 24/7 dedicated |
| **Custom Domain** | No | No | Yes (*.tenant.tripico.fun) |

#### Functional Capabilities

| Feature | Freemium | Standard | Enterprise |
|---------|----------|----------|------------|
| Itinerary CRUD | Basic | Full | Full |
| Recommendations | Basic (popular only) | Full (personalized) | Full + custom algorithms |
| Weather Forecasts | 7-day | 7-day + hourly | 7-day + hourly + historical |
| Travel Warnings | Basic alerts | Full alerts + email | Full + SMS + webhook |
| Social Features | Like only | Like + Comment | Like + Comment + Share |
| API Access | No | Yes (rate-limited) | Yes (unlimited) |
| Analytics | No | Basic dashboard | Advanced analytics |

#### Thresholds and Limits

| Limit | Freemium | Standard | Enterprise |
|-------|----------|----------|------------|
| API Calls/day | 1,000 | 100,000 | Unlimited |
| Storage | 100 MB | 10 GB | 100 GB |
| File Upload Size | 5 MB | 50 MB | 500 MB |
| Concurrent Users | 100 | 1,000 | 10,000 |
| Webhook Integrations | 0 | 5 | Unlimited |

### 6.2 Pricing Model

#### Fixed Tier Pricing

| Tier | Monthly Price | Annual Price (20% discount) |
|------|--------------|----------------------------|
| **Freemium** | EUR 0 | EUR 0 |
| **Standard** | EUR 199 | EUR 1,910 |
| **Enterprise** | EUR 999 | EUR 9,590 |

#### Pricing Parameters

**Standard Tier:**
- Base fee covers: Dedicated namespace, tenant auth, dedicated bucket
- Included: 100k API calls, 10GB storage, 1,000 users
- Overage: EUR 0.002/API call, EUR 0.10/GB storage

**Enterprise Tier:**
- Base fee covers: Dedicated cluster, VPC, all services
- Included: Unlimited API calls, 100GB storage, 10,000 users
- Overage: EUR 0.05/GB storage beyond 100GB

#### Free Quotas

| Resource | Freemium | Standard | Enterprise |
|----------|----------|----------|------------|
| Users | Unlimited | 1,000 included | 10,000 included |
| Storage | 100 MB | 10 GB | 100 GB |
| API Calls | 1,000/day | 100,000/month | Unlimited |
| Email Alerts | 10/month | 1,000/month | Unlimited |

### 6.3 Cost Model

#### Shared Infrastructure Costs (Monthly)

| Component | Cost (EUR) | Notes |
|-----------|------------|-------|
| GKE Autopilot (shared) | ~150 | 4 nodes base |
| Cloud DNS | ~1 | Zone + queries |
| Artifact Registry | ~20 | Image storage |
| Cloud Firestore | ~25 | Shared database |
| Static IP | ~7 | Shared ingress |
| NGINX Ingress | ~30 | Load balancer |
| Shared Services (weather + warnings) | ~64 | Dedicated pods |
| **Total Shared** | **~297** | |

#### Per-Tenant Costs

**Freemium Namespace:**
| Component | Cost (EUR) |
|-----------|------------|
| itinerary-service | ~9 |
| recommendation-service | ~9 |
| neo4j | ~22 |
| comments-likes-service | ~9 |
| postgres | ~8 |
| **Total** | **~57** |

**Standard Namespace:**
| Component | Cost (EUR) |
|-----------|------------|
| itinerary-service | ~9 |
| recommendation-service | ~18 |
| neo4j | ~40 |
| comments-likes-service | ~9 |
| postgres | ~8 |
| **Total** | **~84** |

**Enterprise Cluster:**
| Component | Cost (EUR) |
|-----------|------------|
| Dedicated GKE Cluster | ~73 |
| All services (dedicated) | ~100 |
| Dedicated weather/warnings | ~64 |
| Static IP | ~7 |
| NGINX Ingress | ~30 |
| **Total** | **~274** |

#### Profitability Scenarios

**Best Case: 10 Freemium + 10 Standard + 3 Enterprise**

| Component | Cost (EUR) | Revenue (EUR) |
|-----------|------------|---------------|
| Shared Infrastructure | 297 | - |
| 10x Freemium Namespace | 570 | 0 |
| 10x Standard Namespace | 840 | 1,990 |
| 3x Enterprise Cluster | 822 | 2,997 |
| **Total** | **2,529** | **4,987** |
| **Profit** | | **2,458 (49% margin)** |

**Average Case: 20 Freemium + 5 Standard + 1 Enterprise**

| Component | Cost (EUR) | Revenue (EUR) |
|-----------|------------|---------------|
| Shared Infrastructure | 297 | - |
| 20x Freemium Namespace | 1,140 | 0 |
| 5x Standard Namespace | 420 | 995 |
| 1x Enterprise Cluster | 274 | 999 |
| **Total** | **2,131** | **1,994** |
| **Profit** | | **-137 (LOSS)** |

**Worst Case: 50 Freemium + 2 Standard + 0 Enterprise**

| Component | Cost (EUR) | Revenue (EUR) |
|-----------|------------|---------------|
| Shared Infrastructure | 297 | - |
| 50x Freemium Namespace | 2,850 | 0 |
| 2x Standard Namespace | 168 | 398 |
| **Total** | **3,315** | **398** |
| **Profit** | | **-2,917 (LOSS)** |

#### Key Insights

1. **Freemium users are costly** - EUR 57/month each with zero revenue
2. **Break-even requires:** ~3 Standard or ~1 Enterprise tenant to cover shared costs
3. **Enterprise is most profitable:** EUR 725/tenant margin (73%)
4. **Standard margin:** EUR 115/tenant (58%)
5. **Recommendation:** Limit freemium users and focus on conversion to paid tiers

#### Break-Even Analysis

| Metric | Value |
|--------|-------|
| Fixed Costs (Shared Infrastructure) | EUR 297/month |
| Break-even Standard Tenants | 3 tenants |
| Break-even Enterprise Tenants | 1 tenant |
| Optimal Freemium:Standard:Enterprise Ratio | 10:5:2 |

---

*Last updated: January 27, 2026*
