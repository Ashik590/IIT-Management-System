# Project Specification

## 1. Overview

The IIT Management System (IITMS) is a desktop application for administering a course from initial setup to final result publication. It serves Administrators, Teachers, and Students through separate role-based dashboards. Data remains available between sessions in a local SQLite database.

## 2. Authentication and Shared Directories

- Users sign in with a username and password; inactive accounts cannot sign in.
- The application opens the dashboard that matches the authenticated role.
- Every role can browse separate Students and Teachers screens and search detailed profile cards.
- Student search supports name, username, roll number, session, and blood group.
- Teacher search supports name, username, employee ID, and designation.
- Administrator accounts never appear in either directory.
- Directory access is read-only for Teachers and Students. Administrators can create, activate, and deactivate Student or Teacher accounts.

## 3. Administrator Features

### Account management

- Create Student accounts with name, email, username, password, roll number, session, and blood group.
- Create Teacher accounts with name, email, username, password, employee ID, and designation.
- Activate or deactivate accounts without deleting historical academic information.

### Course setup

- Create Theory or Lab courses with code, title, credit, academic session, and semester.
- New courses start in Draft state and are visible only to Administrators.
- Edit or delete a Draft course.
- Assign active Teachers and enroll active Students while the course is Draft.
- Theory courses require exactly one Teacher; Lab courses require exactly two.
- Activate a course only after the Teacher requirement and a non-empty Student roster are satisfied.

### Final results and lifecycle

- Enter final-exam marks for active courses: up to 60 for Theory and 30 for Lab.
- Finish a course only when its CE structure is finalized and all required marks exist.
- View a result sheet containing CE, final-exam mark, total, and pass/fail outcome.
- Reset a Finished course for a new batch. Reset returns it to Draft, clears the academic session and Student-specific history, and retains course identity, Teacher assignments, CE components, and resources.

## 4. Teacher Features

- View only assigned Active and Finished courses; Draft courses are hidden.
- Create attendance sessions for an assigned Active course and mark every enrolled Student Present or Absent.
- Correct attendance while the course remains Active and view attendance percentages.
- Configure CE components such as quizzes, assignments, presentations, and midterms.
- Every course has an Attendance component with a default weight of 15% of CE. Teachers may edit its weight but cannot delete it or enter its marks manually.
- Finalize CE only when all component weights total 100%.
- Enter and update manual component marks within their maximum values.
- View Student academic summaries.
- Upload course resources of supported types up to 20 MB.

## 5. Student Features

- View only enrolled Active and Finished courses; Draft courses are hidden.
- View attendance sessions, individual status, and attendance percentage.
- View CE components, available marks, and calculated CE.
- View final CE, final-exam mark, total, and outcome after a course is Finished.
- View and open resources belonging to enrolled courses.

## 6. Academic Rules

| Rule | Theory | Lab |
|---|---:|---:|
| CE contribution | 40 | 70 |
| Final-exam contribution | 60 | 30 |
| Total | 100 | 100 |
| Required Teachers | 1 | 2 |

Attendance contributes to CE using its configured percentage weight. For any component:

```text
contribution = (obtained mark / maximum mark) x component weight x CE maximum / 100
```

The Attendance component uses the Student's attendance percentage as its obtained value. A total mark of at least 40 produces a Completed outcome; otherwise the outcome is Incomplete.

## 7. Main Workflows

1. **Course activation:** Administrator creates Draft course -> assigns required Teacher(s) -> enrolls Students -> activates course.
2. **Academic delivery:** Teacher records attendance -> configures and finalizes CE -> enters marks -> uploads resources.
3. **Completion:** Administrator enters final marks -> system validates all prerequisites -> calculates results -> marks course Finished.
4. **New batch:** Administrator resets Finished course -> Student data and session are cleared -> course returns to Draft for reallocation.

## 8. Validation and Error Handling

- Usernames, roll numbers, employee IDs, and course codes must be unique.
- Marks, component weights, credits, dates, account roles, and course states are validated.
- Unauthorized course access and invalid lifecycle operations are rejected.
- Duplicate enrollment, Teacher allocation, attendance records, and component marks are prevented.
- Multi-step database writes use transactions so partial changes are rolled back.
- Missing or inaccessible resource files produce a user-readable error.

## 9. Scope Limits

The application does not provide messaging, email, fees, payroll, room scheduling, online examinations, Teacher ratings, anonymous reports, or Student-uploaded resources.
