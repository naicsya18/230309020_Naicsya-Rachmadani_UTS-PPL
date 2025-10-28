package com.siakad.service;

import com.siakad.exception.*;
import com.siakad.model.Course;
import com.siakad.model.Enrollment;
import com.siakad.model.Student;
import com.siakad.repository.CourseRepository;
import com.siakad.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test untuk class EnrollmentService
 * Menggunakan MOCK untuk method enrollCourse()
 * dan STUB untuk method validateCreditLimit() & dropCourse()
 */
public class EnrollmentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private GradeCalculator gradeCalculator;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ============================================================
    // TEST: enrollCourse() menggunakan MOCK
    // ============================================================

    @Test
    void testEnrollCourse_Success() {
        // Arrange
        Student student = new Student();
        student.setStudentId("STU001");
        student.setEmail("student@test.com");
        student.setAcademicStatus("ACTIVE");

        Course course = new Course();
        course.setCourseCode("CS101");
        course.setCourseName("Intro to Programming");
        course.setCapacity(30);
        course.setEnrolledCount(10);

        when(studentRepository.findById("STU001")).thenReturn(student);
        when(courseRepository.findByCourseCode("CS101")).thenReturn(course);
        when(courseRepository.isPrerequisiteMet("STU001", "CS101")).thenReturn(true);

        // Act
        Enrollment enrollment = enrollmentService.enrollCourse("STU001", "CS101");

        // Assert
        assertNotNull(enrollment);
        assertEquals("STU001", enrollment.getStudentId());
        assertEquals("CS101", enrollment.getCourseCode());
        assertEquals("APPROVED", enrollment.getStatus());
        verify(courseRepository).update(course);
        verify(notificationService).sendEmail(
                eq("student@test.com"),
                contains("Enrollment Confirmation"),
                contains("Intro to Programming")
        );
    }

    @Test
    void testEnrollCourse_StudentNotFound() {
        when(studentRepository.findById("STU001")).thenReturn(null);
        assertThrows(StudentNotFoundException.class, () ->
                enrollmentService.enrollCourse("STU001", "CS101"));
    }

    @Test
    void testEnrollCourse_StudentSuspended() {
        Student student = new Student();
        student.setStudentId("STU001");
        student.setAcademicStatus("SUSPENDED");
        when(studentRepository.findById("STU001")).thenReturn(student);

        assertThrows(EnrollmentException.class, () ->
                enrollmentService.enrollCourse("STU001", "CS101"));
    }

    @Test
    void testEnrollCourse_CourseNotFound() {
        Student student = new Student();
        student.setStudentId("STU001");
        student.setAcademicStatus("ACTIVE");
        when(studentRepository.findById("STU001")).thenReturn(student);
        when(courseRepository.findByCourseCode("CS101")).thenReturn(null);

        assertThrows(CourseNotFoundException.class, () ->
                enrollmentService.enrollCourse("STU001", "CS101"));
    }

    @Test
    void testEnrollCourse_CourseFull() {
        Student student = new Student();
        student.setStudentId("STU001");
        student.setAcademicStatus("ACTIVE");

        Course course = new Course();
        course.setCourseCode("CS101");
        course.setCapacity(10);
        course.setEnrolledCount(10);

        when(studentRepository.findById("STU001")).thenReturn(student);
        when(courseRepository.findByCourseCode("CS101")).thenReturn(course);

        assertThrows(CourseFullException.class, () ->
                enrollmentService.enrollCourse("STU001", "CS101"));
    }

    @Test
    void testEnrollCourse_PrerequisiteNotMet() {
        Student student = new Student();
        student.setStudentId("STU001");
        student.setAcademicStatus("ACTIVE");

        Course course = new Course();
        course.setCourseCode("CS101");
        course.setCapacity(30);
        course.setEnrolledCount(10);

        when(studentRepository.findById("STU001")).thenReturn(student);
        when(courseRepository.findByCourseCode("CS101")).thenReturn(course);
        when(courseRepository.isPrerequisiteMet("STU001", "CS101")).thenReturn(false);

        assertThrows(PrerequisiteNotMetException.class, () ->
                enrollmentService.enrollCourse("STU001", "CS101"));
    }

    // ============================================================
    // TEST: validateCreditLimit() menggunakan STUB
    // ============================================================

    @Test
    void testValidateCreditLimit_WithinLimit() {
        Student student = new Student();
        student.setStudentId("STU001");
        student.setGpa(3.5);

        when(studentRepository.findById("STU001")).thenReturn(student);
        when(gradeCalculator.calculateMaxCredits(3.5)).thenReturn(24);

        boolean result = enrollmentService.validateCreditLimit("STU001", 20);
        assertTrue(result);
    }

    @Test
    void testValidateCreditLimit_ExceedLimit() {
        Student student = new Student();
        student.setStudentId("STU001");
        student.setGpa(3.0);

        when(studentRepository.findById("STU001")).thenReturn(student);
        when(gradeCalculator.calculateMaxCredits(3.0)).thenReturn(20);

        boolean result = enrollmentService.validateCreditLimit("STU001", 25);
        assertFalse(result);
    }

    @Test
    void testValidateCreditLimit_StudentNotFound() {
        when(studentRepository.findById("STU001")).thenReturn(null);
        assertThrows(StudentNotFoundException.class, () ->
                enrollmentService.validateCreditLimit("STU001", 10));
    }

    // ============================================================
    // TEST: dropCourse() menggunakan STUB
    // ============================================================

    @Test
    void testDropCourse_Success() {
        Student student = new Student();
        student.setStudentId("STU001");
        student.setEmail("student@test.com");

        Course course = new Course();
        course.setCourseCode("CS101");
        course.setCourseName("Intro to Programming");
        course.setEnrolledCount(5);

        when(studentRepository.findById("STU001")).thenReturn(student);
        when(courseRepository.findByCourseCode("CS101")).thenReturn(course);

        enrollmentService.dropCourse("STU001", "CS101");

        verify(courseRepository).update(course);
        verify(notificationService).sendEmail(
                eq("student@test.com"),
                contains("Drop Confirmation"),
                contains("Intro to Programming")
        );
        assertEquals(4, course.getEnrolledCount());
    }

    @Test
    void testDropCourse_StudentNotFound() {
        when(studentRepository.findById("STU001")).thenReturn(null);
        assertThrows(StudentNotFoundException.class, () ->
                enrollmentService.dropCourse("STU001", "CS101"));
    }

    @Test
    void testDropCourse_CourseNotFound() {
        Student student = new Student();
        student.setStudentId("STU001");
        when(studentRepository.findById("STU001")).thenReturn(student);
        when(courseRepository.findByCourseCode("CS101")).thenReturn(null);

        assertThrows(CourseNotFoundException.class, () ->
                enrollmentService.dropCourse("STU001", "CS101"));
    }
}
