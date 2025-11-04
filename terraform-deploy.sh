#!/bin/bash
# Terraform Deployment Script for Travel App Backend
# This script helps with the complete deployment workflow

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/terraform"
PROJECT_ROOT="$SCRIPT_DIR"

# Functions
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_requirements() {
    print_info "Checking requirements..."

    if ! command -v terraform &> /dev/null; then
        print_error "Terraform is not installed. Please install it from https://www.terraform.io/downloads"
        exit 1
    fi

    if ! command -v gcloud &> /dev/null; then
        print_error "gcloud CLI is not installed. Please install it from https://cloud.google.com/sdk/docs/install"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install it from https://docs.docker.com/get-docker/"
        exit 1
    fi

    print_info "All requirements met!"
}

init_terraform() {
    print_info "Initializing Terraform..."
    cd "$TERRAFORM_DIR"
    terraform init
    print_info "Terraform initialized successfully!"
}

plan_terraform() {
    print_info "Creating Terraform plan..."
    cd "$TERRAFORM_DIR"
    terraform plan -out=tfplan
    print_info "Plan created! Review the changes above."
}

apply_terraform() {
    print_info "Applying Terraform configuration..."
    cd "$TERRAFORM_DIR"

    if [ -f "tfplan" ]; then
        terraform apply tfplan
        rm tfplan
    else
        terraform apply
    fi

    print_info "Infrastructure deployed successfully!"
}

build_and_push() {
    print_info "Building and pushing Docker image..."

    cd "$TERRAFORM_DIR"

    # Get values from Terraform output
    PROJECT_ID=$(terraform output -raw project_id 2>/dev/null || echo "")
    REGION=$(terraform output -raw region 2>/dev/null || echo "")
    ARTIFACT_REGISTRY=$(terraform output -raw artifact_registry_name 2>/dev/null || echo "")
    APP_NAME=$(terraform output -raw cloud_run_service_name 2>/dev/null || echo "")

    if [ -z "$PROJECT_ID" ] || [ -z "$REGION" ]; then
        print_error "Could not get project information from Terraform. Make sure infrastructure is deployed."
        exit 1
    fi

    IMAGE_TAG=${1:-latest}
    IMAGE_URL="${REGION}-docker.pkg.dev/${PROJECT_ID}/${ARTIFACT_REGISTRY}/${APP_NAME}:${IMAGE_TAG}"

    print_info "Authenticating with Artifact Registry..."
    gcloud auth configure-docker "${REGION}-docker.pkg.dev"

    print_info "Building Docker image..."
    cd "$PROJECT_ROOT"
    docker build -t "$IMAGE_URL" .

    print_info "Pushing Docker image..."
    docker push "$IMAGE_URL"

    print_info "Image pushed successfully: $IMAGE_URL"

    # Update Cloud Run service
    print_info "Updating Cloud Run service..."
    gcloud run services update "$APP_NAME" \
        --image "$IMAGE_URL" \
        --region "$REGION" \
        --project "$PROJECT_ID"

    print_info "Deployment complete!"
}

show_outputs() {
    print_info "Terraform Outputs:"
    cd "$TERRAFORM_DIR"
    terraform output

    echo ""
    print_info "Service URLs:"
    terraform output -json service_urls | jq -r 'to_entries[] | "\(.key): \(.value)"'
}

destroy_infrastructure() {
    print_warning "This will destroy ALL infrastructure!"
    read -p "Are you sure? Type 'yes' to confirm: " confirm

    if [ "$confirm" != "yes" ]; then
        print_info "Destruction cancelled."
        exit 0
    fi

    print_info "Destroying infrastructure..."
    cd "$TERRAFORM_DIR"
    terraform destroy
    print_info "Infrastructure destroyed."
}

show_help() {
    cat << EOF
Travel App Backend - Terraform Deployment Script

Usage: $0 [COMMAND] [OPTIONS]

Commands:
    check           Check if all requirements are installed
    init            Initialize Terraform
    plan            Create and show Terraform execution plan
    apply           Apply Terraform configuration
    deploy [TAG]    Build, push Docker image and deploy (TAG defaults to 'latest')
    outputs         Show Terraform outputs
    destroy         Destroy all infrastructure
    full [TAG]      Run full deployment (init, plan, apply, deploy)
    help            Show this help message

Examples:
    $0 check                # Check requirements
    $0 init                 # Initialize Terraform
    $0 plan                 # Plan infrastructure changes
    $0 apply                # Apply infrastructure changes
    $0 deploy v1.0.0        # Deploy application with tag v1.0.0
    $0 full latest          # Full deployment with latest tag
    $0 outputs              # Show outputs
    $0 destroy              # Destroy infrastructure

EOF
}

# Main script
case "${1:-help}" in
    check)
        check_requirements
        ;;
    init)
        check_requirements
        init_terraform
        ;;
    plan)
        check_requirements
        plan_terraform
        ;;
    apply)
        check_requirements
        apply_terraform
        ;;
    deploy)
        check_requirements
        build_and_push "${2:-latest}"
        ;;
    outputs)
        show_outputs
        ;;
    destroy)
        destroy_infrastructure
        ;;
    full)
        check_requirements
        init_terraform
        plan_terraform
        read -p "Do you want to apply this plan? (yes/no): " confirm
        if [ "$confirm" = "yes" ]; then
            apply_terraform
            show_outputs
            echo ""
            read -p "Do you want to build and deploy the application? (yes/no): " deploy_confirm
            if [ "$deploy_confirm" = "yes" ]; then
                build_and_push "${2:-latest}"
            fi
        fi
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac

