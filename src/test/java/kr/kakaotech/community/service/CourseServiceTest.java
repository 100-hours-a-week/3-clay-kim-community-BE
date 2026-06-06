package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseSubscription;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.CourseRepository;
import kr.kakaotech.community.repository.CourseSubscriptionRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    CourseRepository courseRepository;
    @Mock
    CourseSubscriptionRepository subscriptionRepository;
    @Mock
    UserRepository userRepository;

    @InjectMocks
    CourseService courseService;

    @Nested
    @DisplayName("코스 알림받기 목록 조회 (getSubscribedCourses)")
    class GetSubscribedCourses {

        @Test
        @DisplayName("성공 - 사용자가 구독한 코스 목록을 반환한다")
        void success() {
            // given
            UUID userId = UUID.randomUUID();
            User user = mock(User.class);
            Course firstCourse = new Course("한강종주");
            Course secondCourse = new Course("북한강종주");
            List<CourseSubscription> subscriptions = List.of(
                    new CourseSubscription(user, firstCourse),
                    new CourseSubscription(user, secondCourse)
            );

            given(userRepository.existsById(userId)).willReturn(true);
            given(subscriptionRepository.findByUser_Id(userId)).willReturn(subscriptions);

            // when
            var responses = courseService.getSubscribedCourses(userId);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses)
                    .extracting("name")
                    .containsExactly("한강종주", "북한강종주");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void fail_userNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            given(userRepository.existsById(userId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> courseService.getSubscribedCourses(userId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_USER));
            verify(subscriptionRepository, never()).findByUser_Id(any());
        }
    }

    @Nested
    @DisplayName("코스 알림받기 등록 (registerCourseSubscription)")
    class RegisterCourseSubscription {

        @Test
        @DisplayName("성공 - 새 구독이면 저장하고 true를 반환한다")
        void success_newSubscription() {
            // given
            UUID userId = UUID.randomUUID();
            Integer courseId = 1;
            User user = mock(User.class);
            Course course = new Course("한강종주");

            given(subscriptionRepository.existsByUser_IdAndCourse_Id(userId, courseId)).willReturn(false);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));

            // when
            boolean created = courseService.registerCourseSubscription(courseId, userId);

            // then
            assertThat(created).isTrue();
            verify(subscriptionRepository).saveAndFlush(any(CourseSubscription.class));
        }

        @Test
        @DisplayName("성공 - 이미 구독 중이면 저장하지 않고 false를 반환한다")
        void success_alreadySubscribed() {
            // given
            UUID userId = UUID.randomUUID();
            Integer courseId = 1;

            given(subscriptionRepository.existsByUser_IdAndCourse_Id(userId, courseId)).willReturn(true);

            // when
            boolean created = courseService.registerCourseSubscription(courseId, userId);

            // then
            assertThat(created).isFalse();
            verify(userRepository, never()).findById(any());
            verify(courseRepository, never()).findById(any());
            verify(subscriptionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("성공 - 동시 중복 요청으로 unique 제약에 걸리면 false를 반환한다")
        void success_duplicateByConcurrentRequest() {
            // given
            UUID userId = UUID.randomUUID();
            Integer courseId = 1;
            User user = mock(User.class);
            Course course = new Course("한강종주");

            given(subscriptionRepository.existsByUser_IdAndCourse_Id(userId, courseId)).willReturn(false);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
            given(subscriptionRepository.saveAndFlush(any(CourseSubscription.class)))
                    .willThrow(new DataIntegrityViolationException("duplicated subscription"));

            // when
            boolean created = courseService.registerCourseSubscription(courseId, userId);

            // then
            assertThat(created).isFalse();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 코스")
        void fail_courseNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            Integer courseId = 1;
            User user = mock(User.class);

            given(subscriptionRepository.existsByUser_IdAndCourse_Id(userId, courseId)).willReturn(false);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(courseRepository.findById(courseId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> courseService.registerCourseSubscription(courseId, userId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_COURSE));
            verify(subscriptionRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("코스 알림받기 취소 (deleteCourseSubscription)")
    class DeleteCourseSubscription {

        @Test
        @DisplayName("성공 - 구독이 있으면 삭제하고 true를 반환한다")
        void success_deleteSubscription() {
            // given
            UUID userId = UUID.randomUUID();
            Integer courseId = 1;

            given(userRepository.existsById(userId)).willReturn(true);
            given(courseRepository.existsById(courseId)).willReturn(true);
            given(subscriptionRepository.deleteByUserIdAndCourseId(userId, courseId)).willReturn(1);

            // when
            boolean deleted = courseService.deleteCourseSubscription(courseId, userId);

            // then
            assertThat(deleted).isTrue();
        }

        @Test
        @DisplayName("성공 - 구독이 없어도 false를 반환하고 예외를 던지지 않는다")
        void success_noSubscription() {
            // given
            UUID userId = UUID.randomUUID();
            Integer courseId = 1;

            given(userRepository.existsById(userId)).willReturn(true);
            given(courseRepository.existsById(courseId)).willReturn(true);
            given(subscriptionRepository.deleteByUserIdAndCourseId(userId, courseId)).willReturn(0);

            // when
            boolean deleted = courseService.deleteCourseSubscription(courseId, userId);

            // then
            assertThat(deleted).isFalse();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 코스")
        void fail_courseNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            Integer courseId = 1;

            given(userRepository.existsById(userId)).willReturn(true);
            given(courseRepository.existsById(courseId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> courseService.deleteCourseSubscription(courseId, userId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_COURSE));
            verify(subscriptionRepository, never()).deleteByUserIdAndCourseId(any(), any());
        }
    }
}
