# gcpnatgatewaymysqlspringboot

Spring Boot 3 (Java 21) service for Google Cloud Run that reads from an external MySQL
server at `63.142.240.31`. Cloud Run egress goes through a VPC with **Cloud NAT and a
reserved static IP**, so the MySQL server only has to whitelist one address.

```
Cloud Run ──(Direct VPC egress, all traffic)──> subnet ──> Cloud NAT (static IP) ──> 63.142.240.31:3306
```

## Endpoints

| Path | What it returns |
|---|---|
| `GET /api/db/ping` | MySQL version, server time and current database (connectivity check) |
| `GET /api/items?limit=20` | Rows from the table in `APP_TABLE` (capped by `APP_MAX_ROWS`, default 100) |
| `GET /actuator/health` | Health status |

## Configuration (environment variables)

| Variable | Default | Notes |
|---|---|---|
| `DB_HOST` | `63.142.240.31` | |
| `DB_PORT` | `3306` | |
| `DB_NAME` | — | required |
| `DB_USER` / `DB_PASSWORD` | — | from Secret Manager on Cloud Run; never commit them |
| `APP_TABLE` | `items` | plain table name, optionally `schema.table` |
| `APP_MAX_ROWS` | `100` | |
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

Edit `deploy/config.sh` (project id, region, DB name, table), then from a shell with
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

## Troubleshooting

- `/api/db/ping` times out: the MySQL firewall isn't allowing the NAT IP, or the NAT
  isn't covering the subnet (`gcloud compute routers nats describe ...`).
- `Access denied for user`: the MySQL user isn't granted for the NAT IP host, or the secret value is wrong.
- `Public Key Retrieval is not allowed`: already handled via `allowPublicKeyRetrieval=true`; prefer `DB_USE_SSL=true` if the server supports TLS.
