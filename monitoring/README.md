# 로컬 JVM 메모리 모니터링

Issue #27의 JVM heap과 프로세스 RSS 측정을 위한 로컬 Prometheus 구성이다.

## 실행

```bash
docker compose -f docker-compose.local.yml up -d --build
```

- Spring API: <http://localhost:8080>
- Actuator metrics: <http://localhost:8080/api/actuator/prometheus>
- Prometheus UI: <http://localhost:9090>
- Prometheus target 상태: <http://localhost:9090/targets>

Spring 컨테이너는 `-Xms256m -Xmx512m`, 컨테이너 메모리 한도는 `1g`로 실행된다.
Prometheus는 5초마다 Actuator를 수집하고 데이터를 7일간 보관한다.

## PromQL

Prometheus UI에서 다음 쿼리로 확인한다.

```promql
# JVM heap used
sum(jvm_memory_used_bytes{job="community-app", area="heap"})

# JVM heap committed
sum(jvm_memory_committed_bytes{job="community-app", area="heap"})

# JVM heap max
sum(jvm_memory_max_bytes{job="community-app", area="heap"})

# Java 프로세스 RSS
process_memory_rss_bytes{job="community-app"}
```

RSS는 heap뿐 아니라 metaspace, code cache, thread stack 등 JVM 네이티브 메모리를 포함하므로 heap used보다 크게 측정될 수 있다.

## 종료

```bash
docker compose -f docker-compose.local.yml down
```

측정 데이터를 포함한 볼륨까지 삭제하려면 `down -v`를 사용한다.
