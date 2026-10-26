-- H2 (MySQL mode) version of the production loan_info table, used only by tests
DROP TABLE IF EXISTS loan_info;
CREATE TABLE loan_info (
  pk_loan_id varchar(50) NOT NULL PRIMARY KEY,
  loan_org_id varchar(50) NOT NULL,
  loan_user_id varchar(50) NOT NULL,
  loan_amount decimal(15,2) NOT NULL,
  interest_rate decimal(5,2) NOT NULL DEFAULT 0.00,
  tenure_months int NOT NULL,
  purpose varchar(255) DEFAULT NULL,
  application_date date NOT NULL DEFAULT CURRENT_DATE,
  approval_date date DEFAULT NULL,
  disbursement_date date DEFAULT NULL,
  due_date date DEFAULT NULL,
  outstanding_amount decimal(15,2) DEFAULT NULL,
  received tinyint(1) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  created_datetime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_user varchar(50) DEFAULT NULL,
  updated_datetime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_user varchar(50) DEFAULT NULL
);
