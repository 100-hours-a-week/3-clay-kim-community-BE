# Backend Deployment

## 문제 정의

기존 CD는 AWS ECR에 이미지를 푸시하고 AWS SSM으로 EC2에 명령을 전달했다. 인프라를 GCP 등 다른 클라우드로 옮기면 ECR, SSM, EC2 태그 조건이 모두 배포 경로의 벤더 종속 지점이 된다.

## 설계 의도

- 컨테이너 이미지는 GHCR에 저장한다.
- GitHub Actions는 Docker Buildx와 GitHub Actions cache로 이미지를 빌드하고 `ghcr.io/{owner}/{repo}`에 푸시한다.
- 대상 서버 배포는 SSH로 수행한다. 서버가 EC2, GCP VM, 온프레미스여도 Docker와 SSH만 있으면 같은 스크립트를 사용한다.
- `deploy/deploy.sh`와 `deploy/compose.app.yaml`은 저장소에서 관리하고 배포 때마다 서버의 `BACKEND_APP_DIR`로 복사한다.
- 운영 compose는 `caddy`, `backend` 두 서비스만 관리한다. Caddy는 프론트 정적 파일 서빙과 `/api/*` reverse proxy를 담당하고, MySQL, Redis 같은 상태 저장소는 별도 인프라로 둔다.
- 운영 `.env`는 서버에만 둔다. DB 비밀번호, JWT secret, CORS origin 같은 런타임 시크릿은 GitHub에 커밋하지 않는다.

## GitHub 설정

필수 repository secrets:

- `DEPLOY_HOST`: 대상 서버 IP 또는 도메인
- `DEPLOY_USER`: SSH 사용자
- `DEPLOY_SSH_KEY`: 대상 서버 접속용 private key

선택 repository secrets:

- `DEPLOY_PORT`: SSH 포트, 기본값 `22`
- `GHCR_USERNAME`: private GHCR package를 pull할 사용자
- `GHCR_TOKEN`: private GHCR package pull 권한이 있는 token

선택 repository variables:

- `BACKEND_APP_DIR`: 서버 배포 디렉터리, 기본값 `/opt/community/V1/docker/app`
- `HEALTH_URL`: 배포 후 헬스체크 URL, 기본값 `http://127.0.0.1:8080/api/actuator/health`

## 서버 준비

대상 서버에는 Docker와 Docker Compose가 설치되어 있어야 한다. `BACKEND_APP_DIR`에는 운영 `.env` 파일을 둔다.

예시:

```dotenv
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/community?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=community
SPRING_DATASOURCE_PASSWORD=change-me
SPRING_DATA_REDIS_HOST=host.docker.internal
SPRING_DATA_REDIS_PORT=6379
JWT_SECRET=change-me
CORS_ALLOWED_ORIGINS=https://example.com
BACKEND_PORT=8080
CADDY_SITE_ADDRESS=:80
CADDY_HTTP_PORT=80
CADDY_HTTPS_PORT=443
```

## 배포 흐름

1. `main` 또는 `dev` push 시 테스트를 실행한다.
2. 테스트 통과 후 Docker 이미지를 `linux/arm64`로 빌드한다.
3. 이미지를 GHCR에 `latest`, `sha-{commit_sha}` 태그로 푸시한다.
4. GitHub Actions가 SSH로 `deploy/deploy.sh`, `deploy/compose.app.yaml`, `deploy/Caddyfile`을 서버에 복사한다.
5. 서버에서 `docker compose pull backend`와 `docker compose up -d backend`를 실행한다.
6. `/api/actuator/health`가 `UP`이 될 때까지 확인한다.

수동 브랜치 배포는 `Deploy Branch to Server` 워크플로에서 실행한다. 이미지 태그는 `manual-{branch}-{short_sha}-{run_number}` 형식이다.

프론트엔드는 Caddy가 `/opt/community/V1/docker/app/frontend/dist`를 정적으로 서빙한다. FE 저장소의 배포 워크플로에서는 빌드 산출물 `dist/`를 이 경로로 업로드한 뒤 서버에서 `./deploy.sh frontend` 또는 `./deploy.sh caddy`를 실행하면 된다.
