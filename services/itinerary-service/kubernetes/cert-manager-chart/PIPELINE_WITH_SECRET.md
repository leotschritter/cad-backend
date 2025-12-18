# Finale Pipeline für Cert-Manager mit DNS01
# (Secret wird automatisch in Pipeline erstellt)

## Vorteile dieser Methode

✅ **Sicherer** - Key liegt nur in GitHub Secrets, nicht auf lokaler Maschine
✅ **Automatisch** - Secret wird bei jedem Deployment aktualisiert
✅ **Reproduzierbar** - Konsistent über alle Deployments
✅ **Kein manueller Schritt** - Alles passiert automatisch

## Voraussetzung: GitHub Secret

Sie haben bereits erstellt:
- ✅ `CERT_MANAGER_DNS01_SA_KEY_DEV` (base64 encoded key)

Für Production später:
- ⏳ `CERT_MANAGER_DNS01_SA_KEY_PROD`

## Pipeline Code

Ersetzen Sie den kompletten `deploy-cert-manager` Job in `.github/workflows/ghcr-and-gcp-itinerary.yml`:

```yaml
  deploy-cert-manager:
    runs-on: ubuntu-latest
    needs: check-versions
    if: ${{ needs.check-versions.outputs.cert_manager_changed == 'true' || github.event_name == 'workflow_dispatch' }}
    environment: ${{ github.ref_name == 'main' && 'production' || 'develop' }}
    permissions:
      id-token: write
      contents: read

    steps:
      - uses: actions/checkout@v4

      - name: Authenticate to Google Cloud
        uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_SERVICE_ACCOUNT_KEY }}

      - name: Set up Cloud SDK
        uses: google-github-actions/setup-gcloud@v2

      - name: Install GKE Auth Plugin
        run: |
          gcloud components install gke-gcloud-auth-plugin --quiet

      - name: Configure kubectl for GKE
        run: |
          gcloud container clusters get-credentials tripico-cluster --region europe-west1

      - name: Install Helm
        uses: azure/setup-helm@v4
        with:
          version: 'latest'

      - name: Set environment variables
        id: env
        run: |
          if [ "${{ github.ref_name }}" = "main" ]; then
            echo "VALUES_FILE=values-prod.yaml" >> $GITHUB_OUTPUT
            echo "CERT_MANAGER_SA_KEY=${{ secrets.CERT_MANAGER_DNS01_SA_KEY_PROD }}" >> $GITHUB_OUTPUT
          else
            echo "VALUES_FILE=values-dev.yaml" >> $GITHUB_OUTPUT
            echo "CERT_MANAGER_SA_KEY=${{ secrets.CERT_MANAGER_DNS01_SA_KEY_DEV }}" >> $GITHUB_OUTPUT
          fi

      - name: Create cert-manager DNS01 Service Account Secret
        env:
          CERT_MANAGER_SA_KEY: ${{ steps.env.outputs.CERT_MANAGER_SA_KEY }}
        run: |
          echo "Creating cert-manager DNS01 service account secret..."

          # Decode base64 secret and create JSON file
          echo "$CERT_MANAGER_SA_KEY" | base64 -d > /tmp/cert-manager-dns01-key.json

          # Create namespace if not exists
          kubectl create namespace cert-manager --dry-run=client -o yaml | kubectl apply -f -

          # Create or update the secret
          kubectl create secret generic cert-manager-dns01-sa \
            --from-file=key.json=/tmp/cert-manager-dns01-key.json \
            --namespace=cert-manager \
            --dry-run=client -o yaml | kubectl apply -f -

          # Clean up temp file immediately
          rm -f /tmp/cert-manager-dns01-key.json

          echo "✅ Secret created/updated successfully"

      - name: Verify secret was created
        run: |
          echo "Verifying secret exists..."
          kubectl get secret cert-manager-dns01-sa -n cert-manager
          echo "✅ Secret verified"

      - name: Deploy cert-manager with DNS01
        working-directory: ./services/itinerary-service/kubernetes
        run: |
          helm repo add jetstack https://charts.jetstack.io
          helm repo update
          helm dependency update ./cert-manager-chart
          helm dependency build ./cert-manager-chart

          echo "Deploying cert-manager with values file: ${{ steps.env.outputs.VALUES_FILE }}"

          helm upgrade --install cert-manager ./cert-manager-chart \
            --namespace cert-manager \
            --create-namespace \
            -f ./cert-manager-chart/${{ steps.env.outputs.VALUES_FILE }} \
            --set cert-manager.installCRDs=true \
            --set cert-manager.global.rbac.create=true \
            --set cert-manager.serviceAccount.create=true \
            --set cert-manager.webhook.serviceAccount.create=true \
            --set cert-manager.cainjector.serviceAccount.create=true \
            --wait --timeout 5m

      - name: Wait for cert-manager to be ready
        run: |
          echo "Waiting for cert-manager pods to be ready..."
          kubectl wait --for=condition=Available --timeout=300s \
            deployment/cert-manager -n cert-manager
          kubectl wait --for=condition=Available --timeout=300s \
            deployment/cert-manager-webhook -n cert-manager
          kubectl wait --for=condition=Available --timeout=300s \
            deployment/cert-manager-cainjector -n cert-manager

          echo "All cert-manager deployments are ready!"
          kubectl get pods -n cert-manager

      - name: Verify ClusterIssuer
        run: |
          echo "Verifying ClusterIssuer..."

          # Wait a bit for ClusterIssuer to be created
          sleep 10

          kubectl get clusterissuer letsencrypt-prod -o yaml || echo "ClusterIssuer not ready yet"
          kubectl describe clusterissuer letsencrypt-prod

          # Check if ClusterIssuer is ready
          READY=$(kubectl get clusterissuer letsencrypt-prod -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' || echo "Unknown")
          if [ "$READY" = "True" ]; then
            echo "✅ ClusterIssuer is ready!"
          else
            echo "⚠️ ClusterIssuer not ready yet, status: $READY"
          fi

      - name: Verify Deployment
        run: |
          echo "=== Helm Release Status ==="
          helm status cert-manager --namespace cert-manager
          echo ""
          echo "=== Pod Status ==="
          kubectl get pods -n cert-manager -o wide
          echo ""
          echo "=== Service Status ==="
          kubectl get svc -n cert-manager
          echo ""
          echo "=== ClusterIssuer Status ==="
          kubectl get clusterissuer
          echo ""
          echo "=== Secret Status ==="
          kubectl get secret cert-manager-dns01-sa -n cert-manager -o jsonpath='{.metadata.creationTimestamp}'
          echo ""
```

## Was diese Pipeline macht:

### 1. **Set environment variables**
- Entwicklung → `values-dev.yaml` + `CERT_MANAGER_DNS01_SA_KEY_DEV`
- Production → `values-prod.yaml` + `CERT_MANAGER_DNS01_SA_KEY_PROD`

### 2. **Create cert-manager DNS01 Service Account Secret**
- Dekodiert GitHub Secret (base64)
- Erstellt temporäre JSON-Datei
- Erstellt/Updated Kubernetes Secret
- **Löscht sofort** die temp-Datei (Sicherheit!)

### 3. **Verify secret was created**
- Prüft ob Secret existiert
- Fail-safe check

### 4. **Deploy cert-manager**
- Deployed mit korrektem values-File
- DNS01 automatisch aktiviert

### 5. **Verify ClusterIssuer**
- Prüft ob ClusterIssuer "Ready" ist
- Zeigt Status

## Sicherheitsaspekte

✅ **GitHub Secret (base64):**
- Liegt verschlüsselt in GitHub
- Nur in Actions verfügbar
- Nicht in Logs sichtbar (GitHub maskiert es)

✅ **Temp-Datei:**
- Existiert nur wenige Sekunden
- Wird sofort gelöscht
- Nur im Container der Action

✅ **Kubernetes Secret:**
- Verschlüsselt at-rest in GKE
- Nur cert-manager kann es lesen

## Was Sie NICHT mehr manuell machen müssen:

❌ Key auf lokale Maschine herunterladen
❌ Secret manuell im Cluster erstellen
❌ Key manuell löschen
❌ Secret-Updates bei Rotation

## Alles automatisch! 🎉

Einfach pushen und die Pipeline macht alles:

```bash
git add .
git commit -m "feat: add cert-manager DNS01 with automatic secret creation"
git push origin develop
```

## Für Production später:

1. Service Account in Production erstellen:
```bash
gcloud config set project graphite-plane-474510-s9
gcloud iam service-accounts create cert-manager-dns01 --display-name="Cert-Manager DNS01"
gcloud projects add-iam-policy-binding graphite-plane-474510-s9 \
  --member="serviceAccount:cert-manager-dns01@graphite-plane-474510-s9.iam.gserviceaccount.com" \
  --role="roles/dns.admin"
gcloud iam service-accounts keys create /tmp/cert-manager-dns01-key-prod.json \
  --iam-account=cert-manager-dns01@graphite-plane-474510-s9.iam.gserviceaccount.com
cat /tmp/cert-manager-dns01-key-prod.json | base64
```

2. GitHub Secret erstellen:
- Name: `CERT_MANAGER_DNS01_SA_KEY_PROD`
- Environment: `production`
- Value: [base64 string]

3. Push zu `main` Branch → Automatisches Deployment! 🚀
