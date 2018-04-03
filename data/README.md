# decisions-on-spark/data
This folder contains decision data sets.

LoanValidation folder covers different sizes of serialized automated decisions to determine a loan validation.

Each CSV file is named with the number of decisions.
Each CSV file is structured with the following columns:
* borrower.FirstName, 
* borrower.LastName, 
* borrower.BirthDate, 
* borrower.SSN, 
* borrower.zipCode, 
* borrower.CreditScore, 
* borrower.YearlyIncome, 
* loan.Amount, 
* loan.StartDate, 
* loan.Duration, 
* loanToValue, 
* report.isApproved, 
* report.isValidData, 
* report.isInsuranceRequired, 
* report.getMonthlyRepayment, 
* report.getInsuranceRate, 
* report.getMessages 
