#!/usr/bin/env bash
# One-time setup: VPC + subnet + Cloud Router + Cloud NAT with a reserved static IP.
# Cloud Run sends all egress through this subnet (Direct VPC egress), so the MySQL
# server at DB_HOST sees every connection coming from the single static IP below.
set -euo pipefail
source "$(dirname "$0")/config.sh"

gcloud config set project "$PROJECT_ID"
gcloud services enable compute.googleapis.com run.googleapis.com \
  artifactregistry.googleapis.com cloudbuild.googleapis.com secretmanager.googleapis.com

gcloud compute networks create "$NETWORK" --subnet-mode=custom || true
gcloud compute networks subnets create "$SUBNET" \
  --network="$NETWORK" --region="$REGION" --range="$SUBNET_RANGE" || true

gcloud compute addresses create "$STATIC_IP_NAME" --region="$REGION" || true

gcloud compute routers create "$ROUTER" --network="$NETWORK" --region="$REGION" || true
gcloud compute routers nats create "$NAT" \
  --router="$ROUTER" --region="$REGION" \
  --nat-custom-subnet-ip-ranges="$SUBNET" \
  --nat-external-ip-pool="$STATIC_IP_NAME" || true

echo
echo "Static egress IP (whitelist this on the MySQL server for port $DB_PORT):"
gcloud compute addresses describe "$STATIC_IP_NAME" --region="$REGION" --format='value(address)'
