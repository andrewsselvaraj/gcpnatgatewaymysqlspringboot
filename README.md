# gcpnatgatewaymysqlspringboot

Spring Boot 3 (Java 21) service for Google Cloud Run that reads from an external MySQL
server at `63.142.240.31`. Cloud Run egress goes through a VPC with **Cloud NAT and a
reserved static IP**, so the MySQL server only has to whitelist one address.

```
Cloud Run ──(Direct VPC egress, all traffic)──> subnet ──> Cloud NAT (static IP) ──> 63.142.240.31:3306
```

## Endpoints

| Method & path | What it does |
|---|---|
| `GET /api/db/ping` | MySQL version, server time and current database (connectivity check) |
| `GET /api/loans?orgId=&userId=&status=&limit=50&offset=0` | List loans from `loan_info` (all filters optional, newest first, limit max 500) |
| `GET /api/loans/{loanId}` | One loan, 404 if missing |
| `POST /api/loans` | Insert. `loanId` optional (UUID generated); omitted `interestRate`, `received`, `status`, `applicationDate` use the column defaults. 409 if the id exists |
| `PUT /api/loans/{loanId}` | Update all editable fields (`updatedDatetime` is set automatically) |
| `DELETE /api/loans/{loanId}` | Delete, 204 on success |
| `GET /actuator/health` | Health status |

Example:

```bash
curl -X POST "$URL/api/loans" -H 'Content-Type: application/json' -d '{
  "orgId": "ORG1", "userId": "U1", "loanAmount": 50000.00, "interestRate": 9.5,
  "tenureMonths": 12, "purpose": "Car loan", "createdUser": "admin"
}'

curl -X PUT "$URL/api/loans/<loanId>" -H 'Content-Type: application/json' -d '{
  "orgId": "ORG1", "userId": "U1", "loanAmount": 50000.00, "tenureMonths": 12,
  "status": "APPROVED", "approvalDate": "2026-10-26", "updatedUser": "manager"
}'
```

JSON field names map to columns: `loanId`=`pk_loan_id`, `orgId`=`loan_org_id`,
`userId`=`loan_user_id`, and the rest are the camelCase form of the column name.

## Configuration (environment variables)

| Variable | Default | Notes |
|---|---|---|
| `DB_HOST` | `63.142.240.31` | |
| `DB_PORT` | `3306` | |
| `DB_NAME` | — | required |
| `DB_USER` / `DB_PASSWORD` | — | from Secret Manager on Cloud Run; never commit them |
| `DB_USE_SSL` | `false` | set `true` if the server requires TLS |

## Run locally

```bash
cp .env.example .env   # fill in values
set -a; source .env; set +a
mvn spring-boot:run
curl localhost:8080/api/db/ping
```

(Locally your own public IP must be allowed by the MySQL server.)

## Deploy to Cloud Run

Edit `deploy/config.sh` (project id, region, DB name), then from a shell with
`gcloud` logged in (Cloud Shell works well):

```bash
./deploy/01-network-nat.sh   # VPC, subnet, Cloud Router, Cloud NAT + static IP; prints the IP
./deploy/02-secrets.sh       # prompts for DB user/password, stores them in Secret Manager
./deploy/03-deploy.sh        # Cloud Build image -> Artifact Registry -> Cloud Run
```

After step 1, **whitelist the printed static IP on the MySQL server** (firewall for port
3306, and a MySQL user allowed from that host, e.g. `'appuser'@'<static-ip>'`).

The deploy uses Cloud Run Direct VPC egress (`--network/--subnet --vpc-egress=all-traffic`),
which replaces the older Serverless VPC Access connector and works with Cloud NAT.

## Calling the deployed service

The service is deployed **private** by default (`ALLOW_UNAUTHENTICATED="false"` in
`deploy/config.sh`) because it can change and delete loans. Call it with an identity token:

```bash
curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" "$URL/api/loans"
```

Other callers need `roles/run.invoker` on the service.

## Troubleshooting

- `/api/db/ping` times out: the MySQL firewall isn't allowing the NAT IP, or the NAT
  isn't covering the subnet (`gcloud compute routers nats describe ...`).
- `Access denied for user`: the MySQL user isn't granted for the NAT IP host, or the secret value is wrong.
- `Public Key Retrieval is not allowed`: already handled via `allowPublicKeyRetrieval=true`; prefer `DB_USE_SSL=true` if the server supports TLS.
