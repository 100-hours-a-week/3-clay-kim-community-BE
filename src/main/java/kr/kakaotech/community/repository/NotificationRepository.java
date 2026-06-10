package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    long countByUser_IdAndCourseReport_Id(UUID userId, Long courseReportId);

    @Query("select n.user.id from notifications n where n.eventId = :eventId")
    Set<UUID> findUserIdsByEventId(@Param("eventId") UUID eventId);
}
