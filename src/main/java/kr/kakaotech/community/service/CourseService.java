package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.response.CourseResponse;
import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseSubscription;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.CourseRepository;
import kr.kakaotech.community.repository.CourseSubscriptionRepository;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CourseResponse> getCourses() {
        return courseRepository.findAll().stream()
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getSubscribedCourses(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }

        return subscriptionRepository.findByUser_Id(userId).stream()
                .map(CourseSubscription::getCourse)
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional
    public boolean registerCourseSubscription(Integer courseId, UUID userId) {
        if (subscriptionRepository.existsByUser_IdAndCourse_Id(userId, courseId)) {
            return false;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COURSE));
        CourseSubscription courseSubscription = new CourseSubscription(user, course);

        try {
            subscriptionRepository.saveAndFlush(courseSubscription);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional
    public boolean deleteCourseSubscription(Integer courseId, UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }
        if (!courseRepository.existsById(courseId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_COURSE);
        }

        return subscriptionRepository.deleteByUserIdAndCourseId(userId, courseId) > 0;
    }
}
