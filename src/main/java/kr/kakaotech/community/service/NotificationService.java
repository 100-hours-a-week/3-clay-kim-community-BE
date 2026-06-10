package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.CourseSubscription;
import kr.kakaotech.community.entity.Notification;
import kr.kakaotech.community.repository.CourseSubscriptionRepository;
import kr.kakaotech.community.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final int TITLE_MAX_LENGTH = 100;
    private static final int CONTENT_MAX_LENGTH = 500;

    private final CourseSubscriptionRepository courseSubscriptionRepository;
    private final NotificationRepository notificationRepository;

    public void createCourseReportNotifications(CourseReport report, UUID eventId) {
        Set<UUID> notifiedUserIds = notificationRepository.findUserIdsByEventId(eventId);
        List<Notification> notifications = courseSubscriptionRepository.findByCourse_Id(report.getCourse().getId()).stream()
                .map(CourseSubscription::getUser)
                .filter(user -> !notifiedUserIds.contains(user.getId()))
                .map(user -> new Notification(
                        user,
                        report,
                        eventId,
                        createTitle(report),
                        createContent(report)
                ))
                .toList();

        if (notifications.isEmpty()) {
            return;
        }

        notificationRepository.saveAll(notifications);
    }

    private String createTitle(CourseReport report) {
        return truncate("코스 상태 제보: " + report.getCourse().getName(), TITLE_MAX_LENGTH);
    }

    private String createContent(CourseReport report) {
        return truncate(report.getType().getDescription() + " - " + report.getContent(), CONTENT_MAX_LENGTH);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }
}
