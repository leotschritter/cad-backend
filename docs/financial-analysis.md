# Tripico Multi-Tenant SaaS Financial Analysis

This document provides a comprehensive financial analysis of the Tripico multi-tenant SaaS platform, including infrastructure costs, pricing models, and profitability projections.

## Table of Contents

1. [Infrastructure Overview](#infrastructure-overview)
2. [Cost Analysis by Tier](#cost-analysis-by-tier)
3. [Profitability Analysis](#profitability-analysis)
4. [Pricing Models](#pricing-models)
5. [Dynamic Pricing Implementation](#dynamic-pricing-implementation)
6. [Telemetry Requirements](#telemetry-requirements)
7. [Break-Even Analysis](#break-even-analysis)
8. [Recommendations](#recommendations)

---

## Infrastructure Overview

### Tier Architecture

| Tier | Infrastructure Model | Target Customer |
|------|---------------------|-----------------|
| **Freemium** | Shared cluster, shared namespace | Individual users, trial |
| **Standard** | Shared cluster, dedicated namespace | Small-medium businesses |
| **Enterprise** | Dedicated cluster, full isolation | Large organizations |

### Resource Allocation by Tier

| Resource | Freemium | Standard | Enterprise |
|----------|----------|----------|------------|
| GKE Cluster | Shared | Shared | Dedicated |
| Kubernetes Namespace | Shared (default) | Dedicated | Dedicated (default) |
| VPC Network | Shared | Shared | Dedicated |
| Storage Bucket | Shared | Dedicated | Dedicated |
| API Gateway | Shared | Dedicated | Dedicated |
| Identity Platform Tenant | None (project-level) | Dedicated | Dedicated |
| Static IP | Shared | Shared | Dedicated |
| DNS Records | Shared wildcard | Shared wildcard | Dedicated wildcard |

---

## Cost Analysis by Tier

### Freemium Tier - Base Infrastructure Costs

These are the foundational costs shared across all tenants:

| Resource | Monthly Cost (EUR) | Notes |
|----------|-------------------|-------|
| GKE Autopilot Cluster | ~73.00 | Management fee (~0.10/hour) |
| GKE Compute (base) | ~150-300 | Depends on pod scaling |
| Cloud DNS Zone | ~0.20 | Single managed zone |
| Artifact Registry | ~10-50 | Image storage + egress |
| Firestore | ~25-100 | Depends on usage |
| Cloud Storage (base bucket) | ~5-20 | Per-GB pricing |
| API Gateway | ~3.50 | Per million calls |
| Static IP | ~7.30 | Regional IP |
| Identity Platform | Free | Up to 50k MAU free |
| **Base Monthly Total** | **~275-555** | Without compute scaling |

### Standard Tier - Per-Tenant Incremental Costs

Additional costs for each Standard tenant:

| Resource | Monthly Cost (EUR) | Notes |
|----------|-------------------|-------|
| Namespace overhead | ~0 | Logical isolation only |
| Storage Bucket | ~5-20 | Dedicated bucket |
| API Gateway Config | ~3.50 | Additional gateway |
| Workload Identity Binding | ~0 | IAM configuration |
| Identity Platform Tenant | ~0 | Included in free tier |
| Additional Compute | ~50-150 | Tenant workload pods |
| **Per-Tenant Monthly** | **~60-175** | |

### Enterprise Tier - Per-Tenant Full Costs

Complete isolated infrastructure per Enterprise tenant:

| Resource | Monthly Cost (EUR) | Notes |
|----------|-------------------|-------|
| Dedicated GKE Cluster | ~73.00 | Management fee |
| GKE Compute | ~200-500 | Full cluster workloads |
| Dedicated VPC Network | ~0 | No direct cost |
| VPC Subnets | ~0 | No direct cost |
| Firewall Rules | ~0 | No direct cost |
| Storage Bucket | ~10-50 | Higher usage expected |
| API Gateway | ~7-20 | Higher traffic |
| Static IP | ~7.30 | Dedicated regional IP |
| DNS Wildcard Record | ~0.50 | Per record |
| NGINX Ingress Controller | ~20-50 | Dedicated load balancer |
| Identity Platform Tenant | ~0 | Included in free tier |
| **Per-Tenant Monthly** | **~320-700** | |

### Cost Summary Table

| Tier | Monthly Infrastructure Cost | Notes |
|------|---------------------------|-------|
| Freemium (base) | EUR 275-555 | Shared by all users |
| Standard (incremental) | EUR 60-175 per tenant | On top of base |
| Enterprise (full) | EUR 320-700 per tenant | Fully isolated |

---

## Profitability Analysis

### Scenario: 100 Freemium + 10 Standard + 2 Enterprise Tenants

**Infrastructure Costs:**
| Component | Calculation | Monthly Cost (EUR) |
|-----------|-------------|-------------------|
| Base infrastructure | Fixed | 400 |
| Standard tenants (10x) | 10 x 120 | 1,200 |
| Enterprise tenants (2x) | 2 x 500 | 1,000 |
| **Total Infrastructure** | | **2,600** |

**Revenue (Fixed Pricing Model):**
| Tier | Tenants | Price/Month | Revenue (EUR) |
|------|---------|-------------|---------------|
| Freemium | 100 | 0 | 0 |
| Standard | 10 | 199 | 1,990 |
| Enterprise | 2 | 999 | 1,998 |
| **Total Revenue** | | | **3,988** |

**Profitability:**
- **Gross Profit:** EUR 3,988 - EUR 2,600 = EUR 1,388/month
- **Gross Margin:** 34.8%

### Break-Even Analysis

| Tier | Cost/Tenant | Price | Margin | Break-Even |
|------|------------|-------|--------|------------|
| Standard | EUR 120 | EUR 199 | EUR 79 (40%) | 1 tenant |
| Enterprise | EUR 500 | EUR 999 | EUR 499 (50%) | 1 tenant |

**To cover base infrastructure (EUR 400/month):**
- Need ~5 Standard tenants, OR
- Need ~1 Enterprise tenant, OR
- Mix: 3 Standard + partial Enterprise

---

## Pricing Models

### Model A: Fixed Tier Pricing

Simple, predictable pricing for customers:

| Tier | Monthly Price (EUR) | Included |
|------|---------------------|----------|
| **Freemium** | 0 | Basic features, rate-limited |
| **Standard** | 199 | Full features, dedicated namespace |
| **Enterprise** | 999 | Full isolation, SLA, support |

**Pros:**
- Simple to understand and sell
- Predictable revenue
- Easy to implement

**Cons:**
- May leave money on table for heavy users
- Light users may feel overcharged

### Model B: Usage-Based Dynamic Pricing

Pricing based on actual resource consumption:

| Tier | Base Fee | Variable Components |
|------|----------|---------------------|
| **Freemium** | EUR 0 | N/A (rate-limited) |
| **Standard** | EUR 99 | + EUR 0.001/API call + EUR 0.05/GB storage |
| **Enterprise** | EUR 499 | + EUR 0.0008/API call + EUR 0.03/GB storage + EUR 0.10/compute-hour |

**Example Standard Tenant Bill:**
```
Base fee:                    EUR  99.00
API calls (500,000):         EUR   0.50
Storage (50 GB):             EUR   2.50
-----------------------------------------
Total:                       EUR 102.00
```

**Example Heavy Enterprise Tenant Bill:**
```
Base fee:                    EUR 499.00
API calls (10,000,000):      EUR   8.00
Storage (500 GB):            EUR  15.00
Compute (1000 hours):        EUR 100.00
-----------------------------------------
Total:                       EUR 622.00
```

### Model C: Hybrid Tiered Pricing (Recommended)

Combines predictability with usage fairness:

| Tier | Monthly Fee | Included Allowance | Overage Rates |
|------|------------|-------------------|---------------|
| **Freemium** | EUR 0 | 10k API calls, 1GB storage | Upgrade required |
| **Standard** | EUR 149 | 1M API calls, 50GB storage | EUR 0.002/call, EUR 0.10/GB |
| **Enterprise** | EUR 799 | 10M API calls, 500GB storage, 500 compute-hours | EUR 0.001/call, EUR 0.05/GB, EUR 0.15/hour |

---

## Dynamic Pricing Implementation

### Required Telemetry Metrics

To implement usage-based or hybrid pricing, collect these metrics:

#### 1. API Gateway Metrics
```yaml
metrics:
  - name: api_requests_total
    type: counter
    labels: [tenant_id, service, endpoint, method, status_code]

  - name: api_request_duration_seconds
    type: histogram
    labels: [tenant_id, service, endpoint]

  - name: api_request_size_bytes
    type: histogram
    labels: [tenant_id, service, direction]
```

#### 2. Storage Metrics
```yaml
metrics:
  - name: storage_bytes_used
    type: gauge
    labels: [tenant_id, bucket]

  - name: storage_operations_total
    type: counter
    labels: [tenant_id, operation_type]

  - name: storage_egress_bytes
    type: counter
    labels: [tenant_id]
```

#### 3. Compute Metrics
```yaml
metrics:
  - name: container_cpu_usage_seconds
    type: counter
    labels: [tenant_id, namespace, pod, container]

  - name: container_memory_usage_bytes
    type: gauge
    labels: [tenant_id, namespace, pod, container]

  - name: pod_uptime_seconds
    type: counter
    labels: [tenant_id, namespace, pod]
```

#### 4. Database Metrics
```yaml
metrics:
  - name: firestore_read_operations
    type: counter
    labels: [tenant_id, collection]

  - name: firestore_write_operations
    type: counter
    labels: [tenant_id, collection]

  - name: firestore_document_count
    type: gauge
    labels: [tenant_id, collection]
```

### Implementation Architecture

```
+------------------+     +------------------+     +------------------+
|   API Gateway    |---->|  Metrics Export  |---->|   Prometheus     |
|   (per tenant)   |     |  (OpenTelemetry) |     |   (time-series)  |
+------------------+     +------------------+     +------------------+
                                                          |
+------------------+     +------------------+              v
|   GKE Services   |---->|  kube-state-     |     +------------------+
|   (per tenant)   |     |  metrics         |---->|   Billing        |
+------------------+     +------------------+     |   Aggregator     |
                                                  +------------------+
+------------------+     +------------------+              |
|   Cloud Storage  |---->|  Stackdriver     |              v
|   (per tenant)   |     |  Monitoring      |---->+------------------+
+------------------+     +------------------+     |   Invoice        |
                                                  |   Generator      |
+------------------+     +------------------+     +------------------+
|   Firestore      |---->|  Firestore       |              |
|   (collections)  |     |  Metrics         |              v
+------------------+     +------------------+     +------------------+
                                                  |   Customer       |
                                                  |   Dashboard      |
                                                  +------------------+
```

### Implementation Steps

1. **Phase 1: Metric Collection**
   - Deploy Prometheus with tenant-aware scraping
   - Configure API Gateway to export per-tenant metrics
   - Set up GCP monitoring integration

2. **Phase 2: Aggregation Pipeline**
   - Build daily/monthly aggregation jobs
   - Store aggregated usage in billing database
   - Create tenant usage dashboards

3. **Phase 3: Billing Integration**
   - Implement invoice generation service
   - Add usage alerts and notifications
   - Create customer-facing usage portal

4. **Phase 4: Dynamic Pricing Engine**
   - Implement pricing rules engine
   - Add volume discounts logic
   - Enable custom enterprise pricing

---

## Telemetry Requirements

### Minimum Viable Telemetry (MVP)

For basic usage-based billing:

| Metric | Source | Granularity | Retention |
|--------|--------|-------------|-----------|
| API Calls | API Gateway logs | Per-request | 90 days |
| Storage Size | GCS bucket metrics | Daily | 1 year |
| Active Users | Identity Platform | Monthly | 1 year |

### Full Telemetry Stack

For advanced analytics and pricing:

| Component | Technology | Purpose |
|-----------|------------|---------|
| Metric Collection | OpenTelemetry + Prometheus | Real-time metrics |
| Log Aggregation | Cloud Logging + BigQuery | Detailed analysis |
| Trace Collection | Cloud Trace | Performance debugging |
| Dashboards | Grafana + Looker | Visualization |
| Alerting | Cloud Monitoring | Anomaly detection |
| Billing DB | PostgreSQL/Firestore | Usage records |

### Cost of Telemetry

| Component | Monthly Cost (EUR) |
|-----------|-------------------|
| Cloud Monitoring | 0-50 (free tier available) |
| Cloud Logging | 0.50/GB after free tier |
| BigQuery | 5/TB storage + 5/TB query |
| Prometheus (self-hosted) | ~50-100 (compute) |
| Grafana Cloud | 0-49 (free-pro tiers) |
| **Total Telemetry** | **~50-200** |

---

## Break-Even Analysis

### Minimum Viable Business

To cover all costs including telemetry:

| Cost Component | Monthly (EUR) |
|----------------|---------------|
| Base Infrastructure | 400 |
| Telemetry Stack | 100 |
| **Total Fixed Costs** | **500** |

**Break-even scenarios:**
- 7 Standard tenants @ EUR 149 (hybrid) = EUR 1,043 revenue, EUR 543 profit
- 1 Enterprise tenant @ EUR 799 (hybrid) = EUR 799 revenue, EUR 299 profit
- 3 Standard + 1 Enterprise = EUR 1,246 revenue, EUR 746 profit

### Scaling Economics

| Scale | Fixed Costs | Variable Costs | Revenue | Margin |
|-------|-------------|----------------|---------|--------|
| 10 Std + 1 Ent | EUR 500 | EUR 1,700 | EUR 2,289 | 4% |
| 25 Std + 3 Ent | EUR 500 | EUR 4,500 | EUR 6,122 | 18% |
| 50 Std + 5 Ent | EUR 500 | EUR 8,500 | EUR 11,445 | 21% |
| 100 Std + 10 Ent | EUR 500 | EUR 17,000 | EUR 22,890 | 24% |

*Note: Margins improve with scale due to fixed cost dilution*

---

## Recommendations

### Short-Term (0-6 months)
1. **Start with fixed pricing** (Model A) for simplicity
2. **Implement basic telemetry** (API calls, storage, users)
3. **Target**: 5 Standard + 1 Enterprise tenant to reach profitability

### Medium-Term (6-12 months)
1. **Transition to hybrid pricing** (Model C) as telemetry matures
2. **Build customer usage dashboards**
3. **Add volume discounts** for high-usage customers

### Long-Term (12+ months)
1. **Full dynamic pricing** with real-time usage tracking
2. **Custom enterprise contracts** with committed usage discounts
3. **Automated cost optimization** recommendations for tenants

### Key Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Customer Acquisition Cost | < EUR 500 | Marketing spend / new customers |
| Monthly Recurring Revenue | > EUR 5,000 | Sum of all subscriptions |
| Gross Margin | > 25% | (Revenue - Infra Cost) / Revenue |
| Churn Rate | < 5% monthly | Lost customers / total customers |
| Net Revenue Retention | > 100% | Expansion - churn |

---

## Appendix

### GCP Pricing References (as of 2025)

| Service | Pricing Model | Approximate Cost |
|---------|---------------|------------------|
| GKE Autopilot | Per vCPU-hour + memory-hour | ~EUR 0.05/vCPU-hr |
| Cloud Storage | Per GB/month + operations | ~EUR 0.02/GB |
| Firestore | Per document read/write + storage | ~EUR 0.06/100k reads |
| API Gateway | Per million calls | ~EUR 3.50/million |
| Cloud DNS | Per zone + queries | ~EUR 0.20/zone |
| Static IP | Per hour (unused) | ~EUR 0.01/hour |

### Terraform Module Cost Mapping

| Module | Resources Created | Cost Driver |
|--------|------------------|-------------|
| `modules/gke` | Cluster, VPC, Subnets | Compute hours |
| `modules/storage` | GCS Bucket | Storage + operations |
| `modules/api-gateway` | Gateway, Config | API calls |
| `modules/project` | Identity Platform | MAU (free tier) |

---

*Last updated: January 2025*
*Analysis based on GCP europe-west1 (Belgium) region pricing*
