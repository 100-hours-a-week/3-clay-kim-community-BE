package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    long countByUser_IdAndCourseReport_Id(UUID userId, Long courseReportId);
}
