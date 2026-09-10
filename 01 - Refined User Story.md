# IIT Course Management System

## Refined User Story

### 1. Project overview

The IIT Course Management System is a desktop application for managing the academic lifecycle of courses at the Institute of Information Technology. It focuses on the responsibilities shared by Administrators, Teachers, and Students from course creation and enrollment through attendance, Continuous Evaluation (CE), final examination, and course completion.

The system is intentionally limited to essential course-management activities. Its purpose is to provide clear academic workflows and business rules while remaining manageable for a two-member Design Patterns Lab project.

The application has three user roles:

- **Administrator** — manages user accounts, courses, teacher allocation, student enrollment, final-exam marks, course completion, user searches, and result reports.
- **Teacher** — manages attendance, CE structures, assessment marks, learning resources, and academic summaries for assigned courses.
- **Student** — views personal courses, attendance, assessment marks, CE results, final results, and learning resources.

### 2. Authentication and access

All users access the system through a common login screen using credentials created by an Administrator.

- A user can log in only when the account is active and the credentials are correct.
- After login, the system opens the dashboard appropriate to the user's role.
- A user can access only the operations and information authorized for that role.
- Deactivating an account prevents future login but does not remove historical academic data.

### 3. Administrator user stories

#### 3.1 Manage users

As an Administrator, I want to create and maintain Student and Teacher accounts so that authorized users can access their academic functions.

When creating any account, the Administrator provides a unique username, an initial password, full name, email address, and role-specific information.

Student information includes:

- Roll number
- Academic session
- Blood group

Teacher information includes:

- Employee ID
- Designation

The Administrator can:

- Create Student and Teacher accounts.
- View and update permitted profile information.
- Activate or deactivate accounts.
- Search users by name or username.
- Search Students by roll number or blood group.
- Search Teachers by employee ID.

Usernames, Student roll numbers, and Teacher employee IDs must be unique.

#### 3.1.1 Browse Student and Teacher directories

As any authenticated user, I want separate Student and Teacher directories so that I can find institutional profile information without mixing the two account types.

- Each dashboard provides a Students screen and a Teachers screen.
- Each screen initially displays detailed profile cards for every account of that role.
- Search runs when the user submits a query and displays only matching cards from the selected role.
- Student search supports name, username, roll number, session, and blood group.
- Teacher search supports name, username, employee ID, and designation.
- Teachers and Students can only view directory information.
- Administrators can additionally create accounts and activate or deactivate any Student or Teacher from the corresponding screen.
- Administrator accounts never appear in either directory.

#### 3.2 Create and configure courses

As an Administrator, I want to create and configure courses so that Teachers and Students can participate in the correct academic activities.

A course contains:

- Course code
- Course title
- Course type: Theory or Lab
- Credit value
- Academic session
- Semester
- Course status

A newly created course begins in the **Draft** state.

The Administrator assigns Teachers according to course type:

- A Theory course requires exactly one Teacher.
- A Lab course requires exactly two Teachers.

Teacher assignments may be corrected while the course is Draft. After the course becomes Active, its Teacher assignments are locked. The assignments remain stored after completion as historical academic information.

#### 3.3 Enroll students and activate courses

As an Administrator, I want to enroll Students in a course so that they can participate in its academic activities.

- A Student with no enrollment record for a course is treated as **Not Enrolled**.
- When the Administrator allocates a Student, an enrollment is created with **Enrolled** status.
- Duplicate enrollment of the same Student in the same course is not allowed.
- Only active Student accounts may be newly enrolled.

The Administrator can activate a Draft course after:

- The required number of Teachers has been assigned.
- At least one Student has been enrolled.

Once Active, the course becomes available to its assigned Teachers and enrolled Students.

#### 3.4 Enter final-exam marks

As an Administrator, I want to enter each enrolled Student's final-exam mark so that the system can calculate the final course result.

- For Theory courses, the final examination is worth 60 marks.
- For Lab courses, the final examination is worth 30 marks.
- A final-exam mark must be between 0 and the maximum for the course type.
- Marks can be entered or corrected while the course is Active.
- Every enrolled Student must have a final-exam mark before the course can be finished.

#### 3.5 Finish a course

As an Administrator, I want to finish a course only after its academic requirements are complete so that every Student receives a valid final result.

Before finishing, the system verifies that:

- The course is Active.
- The correct number of Teachers is assigned.
- At least one Student is enrolled.
- The CE structure is finalized.
- CE component weights total exactly 100%.
- Every enrolled Student has the required assessment and final-exam marks.

For each Student, the system calculates:

```text
Theory total = CE mark out of 40 + final-exam mark out of 60
Lab total = CE mark out of 70 + final-exam mark out of 30
```

The enrollment outcome is:

```text
Total mark >= 40  -> Completed
Total mark < 40   -> Incomplete
```

After successful completion:

- The course becomes **Finished**.
- Every enrollment becomes Completed or Incomplete.
- Attendance, CE structures, assessment marks, final-exam marks, enrollment, and Teacher allocation become read-only.
- Historical results and course resources remain available to authorized users.

The entire completion operation must either succeed completely or leave the course unchanged if any validation or database operation fails.

#### 3.6 Reset a course for a new batch

As an Administrator, I want to reset a Finished course so that its configuration can be reused for another batch.

- The course returns to Draft.
- The academic session is cleared and must be entered before reactivation.
- Previous enrollments, results, assessment marks, and attendance are removed.
- Course details, Teacher assignments, CE components and weights, and resources remain unchanged.
- Only a Finished course can be reset.

#### 3.7 View reports

As an Administrator, I want to view academic summaries so that I can monitor course outcomes.

The Administrator can view:

- A course roster.
- Individual Student attendance percentages.
- Individual Student CE marks.
- Final-exam marks.
- Total marks and completion outcomes.
- A course result sheet containing all enrolled Students.

### 4. Teacher user stories

#### 4.1 Access assigned courses

As a Teacher, I want to view my assigned Active and Finished courses so that I can manage current courses and review historical ones.

- Academic records may be modified only while a course is Active.
- Finished courses and their records are read-only.
- A Teacher cannot manage a course to which they are not assigned.

#### 4.2 Manage attendance

As a Teacher, I want to create attendance sessions for an assigned Active course so that Student participation can be recorded.

When a new attendance session is created:

- The system loads all currently enrolled Students.
- Every Student is initially marked Present.
- The Teacher changes the status of absent Students.
- The Teacher reviews and submits the session.

An attendance session contains a course, class date, optional session title, and attendance status for each enrolled Student.

The Teacher may correct a submitted attendance record while the course remains Active. The system calculates attendance as:

```text
Attendance percentage = Present sessions / Total submitted sessions * 100
```

If no attendance session exists, the system displays that attendance has not yet been recorded instead of showing a misleading percentage.

#### 4.3 Define the CE structure

As a Teacher, I want to define a flexible CE structure so that different courses can use appropriate assessment components.

The total CE is worth 40 marks for Theory courses and 70 marks for Lab courses. The Teacher can add assessment components such as quizzes, assignments, presentations, midterms, or attendance.

Every course includes an Attendance assessment component with a default weight of 15% of CE. Its obtained value is calculated automatically from the Student's attendance percentage, so Teachers do not enter a separate Attendance mark. A Teacher may change the Attendance component's weight, but cannot delete it.

Each component contains:

- Assessment title
- Weight percentage within the CE
- Maximum mark

Rules include:

- Weight must be greater than 0 and at most 100.
- Maximum mark must be greater than 0.
- The combined component weights must equal exactly 100% before finalization.
- Existing component titles and maximum marks do not change when a weight is edited.
- The Teacher may add components or change weights while the course is Active.

The CE structure begins as **Draft**. It becomes **Finalized** only when its total weight is exactly 100%.

If a Teacher adds a component or changes a weight after finalization:

- The CE structure returns to Draft.
- Existing Student marks remain stored.
- Further mark entry and course completion are blocked until the structure is finalized again.
- Calculated CE values are treated as provisional until re-finalization.

#### 4.4 Enter assessment marks

As a Teacher, I want to enter assessment marks for enrolled Students so that their CE can be calculated accurately.

- Marks may be entered only for a finalized CE structure in an Active course.
- The system displays all enrolled Students for the selected component.
- An obtained mark must be between 0 and the component's maximum mark.
- The Teacher can update marks while the course remains Active.
- A Student may have at most one mark record for a given component.

The contribution of a component is calculated as:

```text
Component contribution =
    (obtained mark / maximum mark)
    * (weight percentage / 100)
    * CE maximum for the course type
```

For the Attendance component, the attendance percentage replaces `obtained mark / maximum mark` in this calculation.

The Student's CE mark is the sum of all component contributions and cannot exceed 40 for Theory or 70 for Lab.

#### 4.5 Manage course resources

As a Teacher, I want to upload learning resources for an assigned Active course so that enrolled Students can access relevant materials.

A resource contains:

- Course
- Original filename
- Stored filename or path
- File type
- File size
- Uploader
- Upload date and time

The application copies an accepted file into its managed resource directory and stores its metadata in SQLite.

- Only an assigned Teacher can upload a resource.
- Students cannot upload resources in the current project scope.
- Enrolled and Completed/Incomplete Students may view resources belonging to their courses.
- Removing a database resource record must be coordinated with removal of its managed file.

### 5. Student user stories

#### 5.1 View courses

As a Student, I want to view my current and completed courses so that I can follow my academic progress.

The Student dashboard separates:

- Active courses with Enrolled status.
- Finished courses with Completed or Incomplete status.

A Student cannot access another Student's academic records.

#### 5.2 View attendance

As a Student, I want to view my attendance records and percentage for an enrolled course so that I can monitor participation.

The Student can view:

- Attendance session date and title.
- Present or Absent status for each session.
- Total sessions.
- Total Present sessions.
- Calculated attendance percentage.

#### 5.3 View CE and final results

As a Student, I want to view my assessment and result information so that I understand my academic performance.

For an Active course, the Student can view:

- Assessment components.
- Obtained marks already entered by the Teacher.
- Maximum marks.
- Component weights.
- Current CE mark, labeled provisional when the CE structure is not finalized.

For a Finished course, the Student can additionally view:

- Final CE mark out of 40 for Theory or 70 for Lab.
- Final-exam mark out of 60 for Theory or 30 for Lab.
- Total mark out of 100.
- Completed or Incomplete outcome.

#### 5.4 Access resources

As a Student, I want to view and open resources from my courses so that I can use the materials supplied by Teachers.

- The Student may access resources only for courses in which they have an enrollment record.
- Resource access remains available after the course is Finished.
- The application reports missing or inaccessible files without losing the associated academic data.

### 6. Core lifecycle summary

```text
Administrator creates Draft course
        -> assigns required Teacher(s)
        -> enrolls Students
        -> activates course

Teacher records attendance
        -> creates and finalizes CE structure
        -> enters assessment marks
        -> uploads resources

Administrator enters final-exam marks
        -> requests course completion
        -> system validates all requirements
        -> calculates final results
        -> marks enrollments Completed/Incomplete
        -> marks course Finished and read-only
```

### 7. Search, analysis, and reporting operations

The project includes the following non-CRUD operations:

- Search Students and Teachers using supported profile attributes.
- Calculate attendance percentages from attendance records.
- Calculate weighted CE marks from assessment structures and obtained marks.
- Generate a complete course result sheet.
- Separate a Student's current and historical courses by lifecycle status.

### 8. Validation and error expectations

The system must handle at least these cases clearly:

- Incorrect or inactive login.
- Duplicate username, roll number, employee ID, course code, or enrollment.
- Incorrect Teacher count for a course type.
- Attempting an operation on an unauthorized or Finished course.
- CE weights that do not total 100%.
- Assessment marks outside the allowed range.
- Final-exam marks outside the course-type limit (0–60 for Theory or 0–30 for Lab).
- Missing marks during course completion.
- Empty attendance history.
- Missing or inaccessible resource files.
- Database errors during multi-step operations.

### 9. Explicitly excluded from the current scope

The following features are not part of the initial project:

- Teacher ratings or feedback.
- Anonymous Student reports.
- Alumni management.
- General departmental-information management.
- Student-uploaded resources.
- Notifications, email, or messaging.
- Mid-course Teacher reassignment.
- Online classes or examination delivery.
- Fees, payroll, room scheduling, and institutional administration.

These may be discussed as possible future extensions but should not be implemented for the final submission unless the core system is already complete and verified.

### 10. Project success criteria

The system is successful when:

- Each role can complete its authorized workflows through JavaFX.
- Course and academic data persist in SQLite after restart.
- Theory and Lab Teacher-allocation rules are enforced.
- Attendance and CE calculations are correct.
- Invalid marks and incomplete CE structures are rejected.
- A course cannot finish until every completion rule passes.
- Course completion updates all related records consistently.
- Finished academic records remain available but cannot be modified.
- Important business rules are covered by automated tests.
