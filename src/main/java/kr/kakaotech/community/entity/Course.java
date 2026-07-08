package kr.kakaotech.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity(name = "courses")
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private CourseStatus currentStatus = CourseStatus.NORMAL;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Course(String name) {
        this.name = name;
    }

    public Course(String name, CourseStatus currentStatus) {
        this.name = name;
        this.currentStatus = currentStatus;
    }

    public void updateStatus(CourseStatus currentStatus) {
        this.currentStatus = currentStatus;
        this.updatedAt = LocalDateTime.now();
    }
}
