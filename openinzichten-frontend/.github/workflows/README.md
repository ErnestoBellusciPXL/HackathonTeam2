Here is a short overlay of the pipelines.

---
## CI Pipeline 
CI pipeline consist of 2 jobs:
1. Check the SonarQube status  
  The first job checks the status of SonarQube, and if it's down/not healthy/has other issues, then it starts docker compose with SonarQube.
It used in testing steps in following job.
2. Build itself  
  Firstly, it fetches the source code, installs Node and Java (requirement for Sonar-CLI).
  Then it fetches all dependencies for the frontend and Sonar-CLI files.
  After that, it runs SonarQube code quality scan (visible in SonarQube web interface), Trivy security scan (filtered on HIGH and CRITICAL issues) and npm scan.
  After successful security scans, it builds a docker image (locally), makes a `.tar` file of it and saves the `.tar` artifact in backup folder `/home/github/docker-images`, the home folder of action-runner nologin user.
  The older images are deleted to save on disk space, so only 10 latest images are present in the backup folder.
---
## Allure Report
To see Allure report page, you must download archive (artifact from the pipeline run), unzip it, and in the folder with the files run `python3 -m http.server 8000`.  
Then you can open http://localhost:8000/ in your browser.
