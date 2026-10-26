#!/usr/bin/env bash
# Build the image with Cloud Build and deploy to Cloud Run with all egress via the NAT'd VPC.
set -euo pipefail
source "$(dirname "$0")/config.sh"
cd "$(dirname "$0")/.."

gcloud artifacts repositories create "$AR_REPO" \
  --repository-format=docker --location="$REGION" 2>/dev/null || true

gcloud builds submit --tag "$IMAGE" .

gcloud run deploy "$SERVICE" \
  --image="$IMAGE" \
  --region="$REGION" \
  --network="$NETWORK" \
  --subnet="$SUBNET" \
  --vpc-egress=all-traffic \
  --set-env-vars="DB_HOST=${DB_HOST},DB_PORT=${DB_PORT},DB_NAME=${DB_NAME}" \
  --set-secrets="DB_USER=${DB_USER_SECRET}:latest,DB_PASSWORD=${DB_PASSWORD_SECRET}:latest" \
  --memory=512Mi \
  $([ "$ALLOW_UNAUTHENTICATED" = "true" ] && echo --allow-unauthenticated || echo --no-allow-unauthenticated)

URL=$(gcloud run services describe "$SERVICE" --region="$REGION" --format='value(status.url)')
echo
echo "Deployed: $URL"
echo "Try:  curl -H \"Authorization: Bearer \$(gcloud auth print-identity-token)\" $URL/api/db/ping"
echo "      curl -H \"Authorization: Bearer \$(gcloud auth print-identity-token)\" \"$URL/api/loans?limit=10\""
