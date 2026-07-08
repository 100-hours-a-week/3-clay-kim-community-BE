package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.CourseSubscription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseSubscriptionRepository extends JpaRepository<CourseSubscription, Long> {
    boolean existsByUser_IdAndCourse_Id(UUID userId, Integer courseId);

    long countByUser_IdAndCourse_Id(UUID userId, Integer courseId);

    @EntityGraph(attributePaths = "course")
    List<CourseSubscription> findByUser_Id(UUID userId);

    @Query("""
            SELECT cs.id AS subscriptionId, cs.user.id AS userId
            FROM course_subscriptions cs
            WHERE cs.course.id = :courseId
              AND cs.id > :lastId
            ORDER BY cs.id
            """)
    List<SubscriberProjection> findSubscriberChunk(
            @Param("courseId") Integer courseId,
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        DELETE FROM course_subscriptions
        WHERE user_id = :userId
          AND course_id = :courseId
    """, nativeQuery = true)
    int deleteByUserIdAndCourseId(@Param("userId") UUID userId, @Param("courseId") Integer courseId);

    interface SubscriberProjection {
        Long getSubscriptionId();

        UUID getUserId();
    }
}
