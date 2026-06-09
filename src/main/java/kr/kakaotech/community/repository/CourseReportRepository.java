package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.CourseReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseReportRepository extends JpaRepository<CourseReport, Long> {

}
