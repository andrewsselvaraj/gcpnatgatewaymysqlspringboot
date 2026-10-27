#!/usr/bin/env bash
# Build the image with Cloud Build and deploy to Cloud Run with all egress via the NAT'd VPC.
set -euo pipefail
source "$(dirname "$0")/config.sh"
cd "$(dirname "$0")/.."

# Optional API key (created by 04-make-public.sh). When the secret exists the app requires it.
API_KEY_SECRET="${API_KEY_SECRET:-mysql-app-api-key}"
SECRETS="DB_USER=${DB_USER_SECRET}:latest,DB_PASSWORD=${DB_PASSWORD_SECRET}:latest"
if gcloud secrets describe "$API_KEY_SECRET" >/dev/null 2>&1; then
  SECRETS="${SECRETS},APP_API_KEY=${API_KEY_SECRET}:latest"
elif [ "$ALLOW_UNAUTHENTICATED" = "true" ]; then
  echo "Refusing to deploy publicly without an API key. Run ./deploy/04-make-public.sh instead." >&2
  exit 1
fi

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
  --set-secrets="$SECRETS" \
  --memory=512Mi \
  $([ "$ALLOW_UNAUTHENTICATED" = "true" ] && echo --allow-unauthenticated || echo --no-allow-unauthenticated)

URL=$(gcloud run services describe "$SERVICE" --region="$REGION" --format='value(status.url)')
echo
echo "Deployed: $URL"
echo "Try:  curl -H \"Authorization: Bearer \$(gcloud auth print-identity-token)\" $URL/api/db/ping"
echo "      curl -H \"Authorization: Bearer \$(gcloud auth print-identity-token)\" \"$URL/api/loans?limit=10\""
