package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.CourseReportType;
import kr.kakaotech.community.entity.CourseSubscription;
import kr.kakaotech.community.entity.Notification;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.CourseSubscriptionRepository;
import kr.kakaotech.community.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    CourseSubscriptionRepository courseSubscriptionRepository;
    @Mock
    NotificationRepository notificationRepository;

    @Test
    @DisplayName("코스 구독자마다 같은 이벤트 ID를 가진 알림을 생성한다")
    @SuppressWarnings("unchecked")
    void createCourseReportNotifications_success() {
        // given
        Course course = new Course("한강종주");
        ReflectionTestUtils.setField(course, "id", 1);
        User firstUser = new User("first@test.com", "password", "firstuser", "USER");
        User secondUser = new User("second@test.com", "password", "seconduser", "USER");
        ReflectionTestUtils.setField(firstUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondUser, "id", UUID.randomUUID());
        CourseReport report = new CourseReport(
                course,
                firstUser,
                CourseReportType.CLOSED,
                "진입로가 통제되었습니다."
        );
        UUID eventId = UUID.randomUUID();
        NotificationService notificationService = new NotificationService(
                courseSubscriptionRepository,
                notificationRepository
        );

        given(courseSubscriptionRepository.findByCourse_Id(course.getId()))
                .willReturn(List.of(
                        new CourseSubscription(firstUser, course),
                        new CourseSubscription(secondUser, course)
                ));
        given(notificationRepository.findUserIdsByEventId(eventId)).willReturn(Set.of());

        // when
        notificationService.createCourseReportNotifications(report, eventId);

        // then
        ArgumentCaptor<Iterable<Notification>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = (List<Notification>) captor.getValue();

        assertThat(notifications).hasSize(2);
        assertThat(notifications)
                .extracting(notification -> notification.getUser().getEmail())
                .containsExactly("first@test.com", "second@test.com");
        assertThat(notifications)
                .extracting(Notification::getEventId)
                .containsOnly(eventId);
        assertThat(notifications)
                .allSatisfy(notification -> {
                    assertThat(notification.getCourseReport()).isEqualTo(report);
                    assertThat(notification.getTitle()).contains("한강종주");
                    assertThat(notification.getContent()).contains("통제");
                    assertThat(notification.getRead()).isFalse();
                });
    }

    @Test
    @DisplayName("같은 이벤트로 이미 생성된 사용자 알림은 다시 생성하지 않는다")
    @SuppressWarnings("unchecked")
    void createCourseReportNotifications_skipAlreadyCreatedUsers() {
        // given
        Course course = new Course("한강종주");
        ReflectionTestUtils.setField(course, "id", 1);
        User firstUser = new User("first@test.com", "password", "firstuser", "USER");
        User secondUser = new User("second@test.com", "password", "seconduser", "USER");
        ReflectionTestUtils.setField(firstUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondUser, "id", UUID.randomUUID());
        CourseReport report = new CourseReport(
                course,
                firstUser,
                CourseReportType.CLOSED,
                "진입로가 통제되었습니다."
        );
        UUID eventId = UUID.randomUUID();
        NotificationService notificationService = new NotificationService(
                courseSubscriptionRepository,
                notificationRepository
        );

        given(courseSubscriptionRepository.findByCourse_Id(course.getId()))
                .willReturn(List.of(
                        new CourseSubscription(firstUser, course),
                        new CourseSubscription(secondUser, course)
                ));
        given(notificationRepository.findUserIdsByEventId(eventId)).willReturn(Set.of(firstUser.getId()));

        // when
        notificationService.createCourseReportNotifications(report, eventId);

        // then
        ArgumentCaptor<Iterable<Notification>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = (List<Notification>) captor.getValue();

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getUser()).isEqualTo(secondUser);
        assertThat(notifications.get(0).getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("같은 이벤트의 모든 사용자 알림이 이미 있으면 저장을 호출하지 않는다")
    void createCourseReportNotifications_allAlreadyCreated() {
        // given
        Course course = new Course("한강종주");
        ReflectionTestUtils.setField(course, "id", 1);
        User firstUser = new User("first@test.com", "password", "firstuser", "USER");
        User secondUser = new User("second@test.com", "password", "seconduser", "USER");
        ReflectionTestUtils.setField(firstUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondUser, "id", UUID.randomUUID());
        CourseReport report = new CourseReport(
                course,
                firstUser,
                CourseReportType.CLOSED,
                "진입로가 통제되었습니다."
        );
        UUID eventId = UUID.randomUUID();
        NotificationService notificationService = new NotificationService(
                courseSubscriptionRepository,
                notificationRepository
        );

        given(courseSubscriptionRepository.findByCourse_Id(course.getId()))
                .willReturn(List.of(
                        new CourseSubscription(firstUser, course),
                        new CourseSubscription(secondUser, course)
                ));
        given(notificationRepository.findUserIdsByEventId(eventId))
                .willReturn(Set.of(firstUser.getId(), secondUser.getId()));

        // when
        notificationService.createCourseReportNotifications(report, eventId);

        // then
        verify(notificationRepository, never()).saveAll(any());
    }
}
