package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.CourseReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseReportRepository extends JpaRepository<CourseReport, Long> {
    @EntityGraph(attributePaths = "course")
    @Query("select cr from course_reports cr where cr.id = :id")
    Optional<CourseReport> findByIdWithCourse(@Param("id") Long id);
}
