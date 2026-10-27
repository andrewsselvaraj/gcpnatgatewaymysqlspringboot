#!/usr/bin/env bash
# Make the service reachable from a browser, protected by an API key.
# Creates a random key in Secret Manager (once), redeploys with the key check on,
# and opens the service to unauthenticated callers. Every /api/** call then needs
# ?key=<key> in the URL or an X-API-Key header.
set -euo pipefail
cd "$(dirname "$0")"
source ./config.sh
API_KEY_SECRET="${API_KEY_SECRET:-mysql-app-api-key}"

if ! gcloud secrets describe "$API_KEY_SECRET" >/dev/null 2>&1; then
  openssl rand -hex 24 | tr -d '\n' | gcloud secrets create "$API_KEY_SECRET" \
    --replication-policy=automatic --data-file=-
fi
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
gcloud secrets add-iam-policy-binding "$API_KEY_SECRET" \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role=roles/secretmanager.secretAccessor >/dev/null

# Keep the service public on future ./deploy/03-deploy.sh runs
sed -i 's/^ALLOW_UNAUTHENTICATED=.*/ALLOW_UNAUTHENTICATED="true"/' ./config.sh
ALLOW_UNAUTHENTICATED="true" ./03-deploy.sh

KEY=$(gcloud secrets versions access latest --secret="$API_KEY_SECRET")
URL=$(gcloud run services describe "$SERVICE" --region="$REGION" --format='value(status.url)')
echo
echo "Your API key (keep it private): $KEY"
echo "Open in a browser:"
echo "  $URL/api/db/ping?key=$KEY"
echo "  $URL/api/loans?limit=10&key=$KEY"
