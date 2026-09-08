package edu.du.iit.cms;

import edu.du.iit.cms.db.Database;
import edu.du.iit.cms.db.DatabaseSeeder;
import edu.du.iit.cms.pattern.chain.CompletionValidationChain;
import edu.du.iit.cms.pattern.strategy.TeacherAllocationPolicies;
import edu.du.iit.cms.repository.AttendanceRepository;
import edu.du.iit.cms.repository.CourseRepository;
import edu.du.iit.cms.repository.EvaluationRepository;
import edu.du.iit.cms.repository.ResourceRepository;
import edu.du.iit.cms.repository.UserRepository;
import edu.du.iit.cms.security.PasswordHasher;
import edu.du.iit.cms.service.AttendanceService;
import edu.du.iit.cms.service.AuthService;
import edu.du.iit.cms.service.CourseCompletionService;
import edu.du.iit.cms.service.CourseService;
import edu.du.iit.cms.service.EvaluationService;
import edu.du.iit.cms.service.ReportingService;
import edu.du.iit.cms.service.ResourceService;
import edu.du.iit.cms.service.UserService;

import java.nio.file.Path;

public final class AppServices {
    private final Database database;
    private final AuthService authService;
    private final UserService userService;
    private final CourseService courseService;
    private final AttendanceService attendanceService;
    private final EvaluationService evaluationService;
    private final ResourceService resourceService;
    private final ReportingService reportingService;
    private final CourseCompletionService completionService;

    public AppServices(Path dataDirectory, boolean seedData) {
        database = new Database(dataDirectory);
        database.initialize();

        PasswordHasher passwordHasher = new PasswordHasher();
        if (seedData) {
            new DatabaseSeeder(database, passwordHasher).seedIfEmpty();
        }

        UserRepository users = new UserRepository(database);
        CourseRepository courses = new CourseRepository(database);
        EvaluationRepository evaluations = new EvaluationRepository(database);
        AttendanceRepository attendance = new AttendanceRepository(database);
        ResourceRepository resources = new ResourceRepository(database);
        TeacherAllocationPolicies policies = new TeacherAllocationPolicies();

        authService = new AuthService(users, passwordHasher);
        userService = new UserService(users, passwordHasher);
        courseService = new CourseService(courses, policies);
        attendanceService = new AttendanceService(courses, attendance);
        evaluationService = new EvaluationService(courses, evaluations);
        resourceService = new ResourceService(database, courses, resources);
        reportingService = new ReportingService(courses, attendance, evaluations);
        completionService = new CourseCompletionService(courses, evaluations, policies,
                new CompletionValidationChain());
    }

    public Database database() { return database; }
    public AuthService auth() { return authService; }
    public UserService users() { return userService; }
    public CourseService courses() { return courseService; }
    public AttendanceService attendance() { return attendanceService; }
    public EvaluationService evaluation() { return evaluationService; }
    public ResourceService resources() { return resourceService; }
    public ReportingService reporting() { return reportingService; }
    public CourseCompletionService completion() { return completionService; }
}
