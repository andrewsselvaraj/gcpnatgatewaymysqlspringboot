#!/usr/bin/env bash
# One-time: store DB credentials in Secret Manager and let the Cloud Run
# runtime service account read them. Prompts for values so nothing lands in shell history.
set -euo pipefail
source "$(dirname "$0")/config.sh"

read -rp  "MySQL username: " DB_USER
read -rsp "MySQL password: " DB_PASSWORD; echo

for pair in "$DB_USER_SECRET:$DB_USER" "$DB_PASSWORD_SECRET:$DB_PASSWORD"; do
  name="${pair%%:*}"; value="${pair#*:}"
  if gcloud secrets describe "$name" >/dev/null 2>&1; then
    printf '%s' "$value" | gcloud secrets versions add "$name" --data-file=-
  else
    printf '%s' "$value" | gcloud secrets create "$name" --replication-policy=automatic --data-file=-
  fi
done

PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
for name in "$DB_USER_SECRET" "$DB_PASSWORD_SECRET"; do
  gcloud secrets add-iam-policy-binding "$name" \
    --member="serviceAccount:${RUNTIME_SA}" --role=roles/secretmanager.secretAccessor >/dev/null
done
echo "Secrets ready."
