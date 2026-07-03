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
- Grafana: <http://localhost:3000>
- JVM Memory dashboard: <http://localhost:3000/d/community-jvm-memory/community-jvm-memory>

Grafana의 로컬 기본 계정은 `admin` / `admin1234`다. 환경변수 `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`로 변경할 수 있으며 외부에 노출하는 환경에서는 기본 비밀번호를 사용하지 않는다.

Spring 컨테이너는 AWS `t3.small`의 2 GiB 메모리 사양을 기준으로 `-Xms512m -Xmx1g`, 컨테이너 메모리 한도 `2g`로 실행된다. 남은 메모리는 metaspace, code cache, thread stack, direct buffer 등 heap 외 JVM 메모리를 위한 여유 공간이다.

Docker의 `mem_limit`은 Spring 컨테이너에만 적용되며 호스트 OS 메모리는 포함하지 않는다. 실제 `t3.small` 전체 메모리 압박을 동일하게 재현하려면 2 GiB 호스트 또는 전체 컨테이너 메모리 예산을 별도로 제한해야 한다.
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

## Grafana dashboard

Grafana가 시작되면 Prometheus datasource와 `Community JVM Memory` dashboard가 자동으로 등록된다. dashboard에는 다음 항목이 포함된다.

- Application target 상태
- JVM heap used, committed, max와 사용률
- Java process RSS와 non-heap used
- GC pause p95와 평균
- HikariCP active, pending connection

## 종료

```bash
docker compose -f docker-compose.local.yml down
```

측정 데이터를 포함한 볼륨까지 삭제하려면 `down -v`를 사용한다.
