# decisions-on-spark/data
This folder contains decision data sets.

LoanValidation folder covers different sizes of serialized automated decisions to determine a loan validation.

Each CSV file is named with the number of decisions.
Each CSV file is structured with request and response parts. The request part is described by the borrower and loan columns.
The response is contained in the report columns.

Columns are:
* borrower.FirstName: the first name of the borrower,
* borrower.LastName: the last name of the borrower, 
* borrower.BirthDate: the birth date of the borrower, 
* borrower.SSN: the Security Serial Number of the borrower, 
* borrower.zipCode: the zipcode of the borrower, 
* borrower.CreditScore: the credit score of the borrower, 
* borrower.YearlyIncome: the yearly income of the borrower in dollars, 
* loan.Amount: the loan amount in dollars, 
* loan.StartDate: the start date of the loan, 
* loan.Duration: the duration in months of the loan, 
* loanToValue, 
* report.isApproved: the approval of the loan that is the key outcome in the decision, 
* report.isValidData: the validation of the input data, 
* report.isInsuranceRequired: specifies if an insurance is seen as required, 
* report.getMonthlyRepayment: the amount of monthly repayment, 
* report.getInsuranceRate: the insurance rate, 
* report.getMessages: the list of messages created during the decision making. 
