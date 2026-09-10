# IIT Management System (IITMS)

JavaFX desktop application for managing IIT courses, attendance, continuous evaluation, resources, and final results.

## Prerequisites

- JDK 21
- Maven 3.9 or newer
- Git

SQLite is embedded through JDBC; no separate database installation is required.

## Run

```bash
git clone https://github.com/Ashik590/IIT-Management-System.git
cd IIT-Management-System
mvn clean test
mvn javafx:run
```

Use `mvn clean package` to build without opening the application.

## Guidelines

- Launch with `mvn javafx:run`, not by running the main class directly, so JavaFX modules are configured correctly.
- Runtime data is stored in `data/iit-course-management.db`; uploaded resources are copied to `data/resources/`.
- Seed data is created only when the users table is empty.
- To start with fresh demo data, close the application and delete `data/iit-course-management.db`.
- Develop significant changes on feature branches, run `mvn clean test`, and merge through pull requests.
- See [PROJECT_SPECIFICATION.md](PROJECT_SPECIFICATION.md) for features and [TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md) for design details.

## Initial Login Credentials

| Role | Username | Password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Teacher | `teacher1` | `teacher123` |
| Teacher | `teacher2` | `teacher123` |
| Student | `student1` | `student123` |
| Student | `student2` | `student123` |
| Student | `student3` | `student123` |
