# Deploying to Cloud Run, step by step

This takes about 20 minutes the first time. You need a Google Cloud project with
billing enabled and admin access to the MySQL server at `63.142.240.31`.

## Step 0: pick where to run the commands

**Option A, Google Cloud Shell (recommended).** Nothing to install, and `gcloud`, `git`
and `bash` are already there and logged in.

1. Open https://console.cloud.google.com and select (or create) your project at the top.
2. Click the **Activate Cloud Shell** icon (`>_`) in the top right.
3. Run:
   ```bash
   git clone https://github.com/andrewsselvaraj/gcpnatgatewaymysqlspringboot.git
   cd gcpnatgatewaymysqlspringboot
   ```

**Option B, your Windows PC.**

1. Install the Google Cloud CLI: https://cloud.google.com/sdk/docs/install (Windows installer).
2. Open **Git Bash** (comes with Git for Windows). The scripts are bash, so don't use CMD or PowerShell.
3. Log in and go to the repo:
   ```bash
   gcloud auth login
   cd /f/andrew/git/gcpnatgatewaymysqlspringboot
   git pull origin main
   sed -i 's/\r$//' deploy/*.sh   # strip Windows line endings, only needed once
   ```

## Step 1: find your project ID

```bash
gcloud projects list
```

Copy the value in the `PROJECT_ID` column (not the name or number).

## Step 2: fill in `deploy/config.sh`

In Cloud Shell use `nano deploy/config.sh` (or the **Open Editor** button); on Windows use any editor.
Change only these lines:

```bash
PROJECT_ID="your-gcp-project-id"   # from step 1
REGION="us-central1"               # or one close to your MySQL server, e.g. asia-south1
DB_NAME="your_database"            # the MySQL schema that contains loan_info
```

Leave the rest as is. Don't put the DB username or password in this file.

## Step 3: create the network and the static IP

```bash
./deploy/01-network-nat.sh
```

This enables the needed APIs, then creates a VPC, subnet, Cloud Router and Cloud NAT with
a reserved static IP. It ends by printing that IP, for example `34.123.45.67`. **Write it down.**

(If an API was just enabled and a later command fails with "permission" or "not enabled",
wait a minute and run the script again. It is safe to re-run.)

## Step 4: allow that IP on the MySQL server

On the MySQL host (or its hosting provider's firewall panel):

1. **Firewall:** allow inbound TCP port `3306` from the static IP only.
2. **MySQL user:** allow your app user to connect from that IP. As a MySQL admin:
   ```sql
   CREATE USER 'loanapp'@'34.123.45.67' IDENTIFIED BY 'a-strong-password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON your_database.loan_info TO 'loanapp'@'34.123.45.67';
   FLUSH PRIVILEGES;
   ```
   (Use your static IP and database name. If you already have a user, a `GRANT` for the new host is enough.)
3. Make sure MySQL listens on the public interface (`bind-address` in `my.cnf` is not `127.0.0.1`).

## Step 5: store the DB credentials in Secret Manager

```bash
./deploy/02-secrets.sh
```

It asks for the MySQL username and password (the password isn't shown as you type) and
gives Cloud Run permission to read them.

## Step 6: build and deploy

```bash
./deploy/03-deploy.sh
```

Cloud Build builds the Docker image (3 to 5 minutes the first time), pushes it to Artifact
Registry and deploys it to Cloud Run. At the end it prints the service URL, like
`https://mysql-springboot-xxxxx-uc.a.run.app`.

## Step 7: test it

```bash
source deploy/config.sh
URL=$(gcloud run services describe "$SERVICE" --region "$REGION" --format='value(status.url)')
TOKEN=$(gcloud auth print-identity-token)

curl -H "Authorization: Bearer $TOKEN" "$URL/api/db/ping"
curl -H "Authorization: Bearer $TOKEN" "$URL/api/loans?limit=5"
```

`db/ping` should return the MySQL version and server time.

The service is private, so a browser shows 403. That's expected. To open it to the internet,
set `ALLOW_UNAUTHENTICATED="true"` in `config.sh` and re-run step 6. Only do that if you
add your own authentication, since the API can change and delete loans.

## Updating later

After a code change is pushed to GitHub:

```bash
git pull origin main
./deploy/03-deploy.sh
```

Steps 3 to 5 are one-time. Re-run step 5 only to change the DB password.

## If something fails

Logs: `source deploy/config.sh && gcloud run services logs read "$SERVICE" --region "$REGION" --limit 50`

| Symptom | Likely cause |
|---|---|
| 500 with `Connection refused` in the logs | MySQL listens only on localhost: set `bind-address = 127.0.0.1,<server public IP>` (or `0.0.0.0`) in `/etc/mysql/mysql.conf.d/mysqld.cnf` and restart MySQL |
| `db/ping` hangs, then 500 with `Communications link failure` | Firewall on the MySQL side isn't allowing the static IP on 3306, or MySQL isn't listening publicly |
| `Access denied for user 'x'@'34.x.x.x'` | MySQL user not granted for the static IP, or wrong password in step 5 |
| `Unknown database` | `DB_NAME` in `config.sh` is wrong; fix and re-run step 6 |
| Deploy fails with `Permission denied on secret` | Re-run step 5 |
| `403 Forbidden` from curl | Missing or expired token; run the `TOKEN=` line again |
| Billing / API not enabled errors | Enable billing on the project, then re-run step 3 |

Cost note: Cloud NAT and the reserved static IP are billed continuously (a few US dollars
a month) even when Cloud Run is idle.
