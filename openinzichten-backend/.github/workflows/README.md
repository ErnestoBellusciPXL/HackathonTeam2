Here is a short overlay of the pipelines.

---

## CI Pipeline

CI pipeline consist of 2 jobs:

1. Check the SonarQube status  
   The first job checks the status of SonarQube, and if it's down/not healthy/has other issues, then it starts docker
   compose with SonarQube.
   It used in testing steps in following job.
2. Build itself  
   Firstly, it sets up JDK 21 to be able to build the app.
   Then, it runs unit-tests and uploads both jUnit and JaCoCo reports as artifacts.
   After that, it runs the code through SonarQube, to check test coverage, find code smells, etc.
   Finally, it build locally stored Docker image.
   Docker images are stored as `.tar` files in `/home/github/docker-images` (nologin user, used to run pipelines locally
   on the machine itself).
   Currently only the last 10 images are stored locally to save the disk space.

---

## Security Audit

An automatic scheduled run that uses Trivy security scanner to find any vulnerabilities (in all filesystem), then upload
trivy report as an `.txt` artifact.
