package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.ReportRegisterRequest;
import kr.kakaotech.community.entity.CourseReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseReportCommandService {
    private final CourseReportService courseReportService;
    private final NotificationService notificationService;

    @Transactional
    public void registerReport(Integer courseId, ReportRegisterRequest request, UUID userId) {
        CourseReport report = courseReportService.registerReport(courseId, request, userId);
        notificationService.createCourseReportNotifications(report);
    }
}
