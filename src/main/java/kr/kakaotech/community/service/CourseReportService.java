package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.ReportRegisterRequest;
import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.CourseReportRepository;
import kr.kakaotech.community.repository.CourseRepository;
import kr.kakaotech.community.repository.EventOutboxRepository;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseReportService {
    private static final String COURSE_REPORT_CREATED_EVENT_TYPE = "COURSE_REPORT_CREATED";
    private static final String COURSE_REPORT_AGGREGATE_TYPE = "COURSE_REPORT";
    private static final String EMPTY_PAYLOAD = "{}";

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseReportRepository courseReportRepository;
    private final EventOutboxRepository eventOutBoxRepository;

    @Transactional
    public CourseReport registerReport(Integer courseId, ReportRegisterRequest request, UUID userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COURSE));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        CourseReport report = courseReportRepository.save(new CourseReport(
                course,
                user,
                request.getType(),
                request.getContent()
        ));
        course.updateStatus(request.getType().toCourseStatus());

        eventOutBoxRepository.save(new EventOutbox(
                COURSE_REPORT_CREATED_EVENT_TYPE,
                COURSE_REPORT_AGGREGATE_TYPE,
                report.getId(),
                EMPTY_PAYLOAD
        ));

        return report;
    }
}
