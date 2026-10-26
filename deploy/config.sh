# Edit these values, then run the scripts in this folder from a shell with gcloud logged in.
PROJECT_ID="your-gcp-project-id"
REGION="us-central1"
SERVICE="mysql-springboot"

# Networking (created by 01-network-nat.sh)
NETWORK="cr-egress-vpc"
SUBNET="cr-egress-subnet"
SUBNET_RANGE="10.10.0.0/26"
ROUTER="cr-egress-router"
NAT="cr-egress-nat"
STATIC_IP_NAME="cr-egress-ip"

# Database (non-secret settings)
DB_HOST="63.142.240.31"
DB_PORT="3306"
DB_NAME="your_database"
APP_TABLE="your_table"

# Secret Manager secret names holding the DB username and password
DB_USER_SECRET="mysql-db-user"
DB_PASSWORD_SECRET="mysql-db-password"

AR_REPO="apps"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${AR_REPO}/${SERVICE}:latest"
