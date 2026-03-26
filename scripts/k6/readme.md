ACCESS_TOKEN=$(curl -s -X POST https://jongju.duckdns.org:8080/api/auth/testuser | jq -r '.data.accessToken')

k6 run --env BASE_URL=http://jongju.duckdns.org:8080 --env API_PREFIX=/api --env ACCESS_TOKEN=$ACCESS_TOKEN scripts/k6/baseline.js
