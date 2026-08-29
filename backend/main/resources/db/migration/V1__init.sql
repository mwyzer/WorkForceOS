CREATE TABLE departments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE teams (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments (id),
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE employees (
    id UUID PRIMARY KEY,
    employee_number VARCHAR(100) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    department_id UUID NOT NULL REFERENCES departments (id),
    team_id UUID NOT NULL REFERENCES teams (id),
    active BOOLEAN NOT NULL,
    CONSTRAINT uq_employees_employee_number UNIQUE (employee_number)
);

CREATE TABLE leave_requests (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE overtime_requests (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    request_date DATE NOT NULL,
    hours DOUBLE PRECISION NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE user_accounts (
    username VARCHAR(255) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE user_account_roles (
    username VARCHAR(255) NOT NULL REFERENCES user_accounts (username),
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (username, role)
);
