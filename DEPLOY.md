# Deploying JavaJudge

JavaJudge starts a Docker container for every compile and every test, so it needs a server where it can use Docker. Platforms like Render, Railway, Vercel or Heroku don't allow that. Use a small Linux VM.

## 1. Get a VM

Any Ubuntu 22.04 or 24.04 VM with 1 to 2 GB RAM works. Free options for students:

- **Azure for Students**: credit with a college email, no card. Create a `B1s` or `B2s` Ubuntu VM.
- **GitHub Student Developer Pack**: includes DigitalOcean credit. A 1 GB Droplet is enough.
- **Oracle Cloud Always Free**: free long term, but sign-up asks for a card and free VMs are sometimes out of stock.

Pick an x86 (amd64) VM. In the VM's firewall or network settings, open port **8080** (or 80 and 443 if you add HTTPS below).

## 2. Install Docker

SSH into the VM, then:

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
exit   # log out and SSH in again so the group change applies
```

## 3. Run JavaJudge

```bash
git clone https://github.com/sanket1035/java-judge.git
cd java-judge
docker pull eclipse-temurin:17-jdk-alpine      # the sandbox image
sudo mkdir -p /srv/javajudge/runs
docker compose up -d --build
```

Open `http://<your-vm-ip>:8080`. Check it with the Custom run: the second test should fail with the "Number too big for int" hint.

Logs: `docker compose logs -f`. Update after a push: `git pull && docker compose up -d --build`.

## 4. Optional: HTTPS on your own domain

If you own a domain, add an `A` record such as `judge.yourdomain.dev` pointing to the VM IP, then install Caddy, which gets the certificate for you:

```bash
sudo apt install -y caddy
echo 'judge.yourdomain.dev {
    reverse_proxy localhost:8080
}' | sudo tee /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

Open ports 80 and 443 and close 8080 to the outside.

## Security notes

- The app container has the Docker socket mounted so it can start sandbox containers. That makes the app container as powerful as root on the VM, so don't run anything else important on this VM. The submitted code itself never gets the socket: it runs in separate containers with no network.
- Production limits are in `src/main/resources/application-docker.properties`: 2 runs at the same time, 2 s per test, 128 MB heap, 20 tests per submission.
- There is no login and no rate limit per user. For a public demo that's fine. For real users, add a rate limit per IP first.

## Settings

| Property | Default | Meaning |
|---|---|---|
| `runner.sandbox` | `LOCAL` (`DOCKER` in production) | Where code runs |
| `runner.time-limit-ms` | `2000` | Per test |
| `runner.memory-mb` | `128` | JVM heap; the container gets double for JVM overhead |
| `runner.max-output-bytes` | `65536` | Output read per test |
| `runner.max-test-cases` | `20` | Per submission |
| `runner.max-concurrent-runs` | `4` (`2` in production) | Parallel submissions |
| `runner.docker-image` | `eclipse-temurin:17-jdk-alpine` | Sandbox image |
| `runner.work-root` | system temp (`/srv/javajudge/runs` in production) | Must be the same path on the host and in the app container |
