
# Analyzing loan approval decisions automated by IBM Operational Decision Manager
## Analyzing your decisions in Python with Panda and Brunel

This Python 2.10 notebook shows how to load a decision set produced by IBM ODM, and how to apply analytics with Brunel library to get insights on the decisions.
The decision set has been automated by running business rules on randomly generated loan applications. The decision set has been writtern in a CSV format. 

This notebook has been developed with a Panda dataframe and runs on Spark 2.0. 

The intent of applying data science on decisions is to check that decision automation works as expected. In other words, we want to check that the executed rules fit well with the segmentation of the data. From there we will potentialy find optimizations to better automate your decision making. You will be able to extend the notebook to create new views on your decisions by using Panda dataframes and Brunel visualization capabilities.
    
To get the most out of this notebook, you should have some familiarity with the Python programming language.

## Contents 
This notebook contains the following main sections:

1. [Load the loan validation decision set.](#overview)
2. [View an approval distribution pie chart.](#viewapprovaldistribution)
3. [View approvals in a chord chart.](#viewapprovaldistributionincordchart) 
4. [View the income on loan amount distribution.](#incomeoncreditscoredistribution)
5. [View the loan amount on credit score distribution.](#viewamountdistribution)
6. [Summary and next steps.](#next)    

<a id="overview"></a>
## 1. Load the Loan Validation decision set.
The loan validation dataset has been generated with Operational Decision Manager as a CSV file.
The following code accesses to this dataset file to construct a dataframe of simple processed loan applications.


```python
from io import StringIO

import requests
import json
import pandas as pd
import brunel

df = pd.read_csv("https://raw.githubusercontent.com/ODMDev/decisions-on-spark/master/data/miniloan/miniloan-decisions-ls-10K.csv")
df.head()
```




<div>
<style scoped>
    .dataframe tbody tr th:only-of-type {
        vertical-align: middle;
    }

    .dataframe tbody tr th {
        vertical-align: top;
    }

    .dataframe thead th {
        text-align: right;
    }
</style>
<table border="1" class="dataframe">
  <thead>
    <tr style="text-align: right;">
      <th></th>
      <th>name</th>
      <th>creditScore</th>
      <th>income</th>
      <th>loanAmount</th>
      <th>monthDuration</th>
      <th>approval</th>
      <th>rate</th>
      <th>yearlyReimbursement</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th>0</th>
      <td>John Doe</td>
      <td>436</td>
      <td>290532</td>
      <td>136331</td>
      <td>19</td>
      <td>true</td>
      <td>0.080</td>
      <td>13979.403261089992</td>
    </tr>
    <tr>
      <th>1</th>
      <td>John Doe</td>
      <td>333</td>
      <td>95440</td>
      <td>516245</td>
      <td>24</td>
      <td>false</td>
      <td>0.080</td>
      <td>48447.78364788128</td>
    </tr>
    <tr>
      <th>2</th>
      <td>John Doe</td>
      <td>805</td>
      <td>43242</td>
      <td>982572</td>
      <td>14</td>
      <td>false</td>
      <td>0.067</td>
      <td>108746.07131251291</td>
    </tr>
    <tr>
      <th>3</th>
      <td>John Doe</td>
      <td>313</td>
      <td>3773</td>
      <td>286564</td>
      <td>19</td>
      <td>false</td>
      <td>0.080</td>
      <td>30025.806398532393</td>
    </tr>
    <tr>
      <th>4</th>
      <td>John Doe</td>
      <td>639</td>
      <td>141075</td>
      <td>603802</td>
      <td>14</td>
      <td>false</td>
      <td>0.067</td>
      <td>69141.80143043594</td>
    </tr>
  </tbody>
</table>
</div>



A dataframe has been created to capture 10 000 loan application decisions automated with business rules. Business rules have been used to determine eligibility based credit score, loan amount, income to debt ratio. Decision outcomes are represented by the approval and yearlyReplayment columns.

Table above represents a decision set. Each row shows a decision with its input and output parameters:
   * inputs
      * name, 
      * creditScore, 
      * income, 
      * loanAmount, 
      * monthDuration
   * outputs
      * approval, 
      * the loan rate,
      * yearlyReimbursement.
     
By example on row 0 a loan application has been submited for John Doe, with a credit score of 436, a yearly income of USD 290532, a loan amount of USD 136331 and a loan duration set to 19 months. The automated decision gives an approval at a rate of 8% and a yearly reimbursement of USD 13979.4 .


```python
total_rows = df.shape[0]
print("The size of the decision set is " + str(total_rows))
```

    The size of the decision set is 10000


<a id="viewapprovaldistribution"></a>
## 2.View the loan approval distribution in a pie chart.
A simple pie chart that shows the approval distribution in the decision set.


```python
%brunel data('df') stack polar bar x("const") y(#count) color(approval) legends(none) label(approval) :: width=200, height=300
```


<!--
  ~ Copyright (c) 2015 IBM Corporation and others.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ You may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->


<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/brunel.2.3.css">
<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/sumoselect.css">

<style>
    
</style>

<div id="controlsid13ec1026-59b3-11e8-855e-002590fb7074" class="brunel"/>
<svg id="visid13ec066c-59b3-11e8-855e-002590fb7074" width="200" height="300"></svg>





    <IPython.core.display.Javascript object>



<a id="viewapprovaldistributionincordchart"></a>
## 3.View the loan approval distribution per credit score in a chord chart.
A chord chart that shows the approval count per credit score.


```python
%brunel data('df') chord x(approval) y(creditScore) color(#count) tooltip(#all)
```


<!--
  ~ Copyright (c) 2015 IBM Corporation and others.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ You may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->


<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/brunel.2.3.css">
<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/sumoselect.css">

<style>
    
</style>

<div id="controlsid1501393c-59b3-11e8-855e-002590fb7074" class="brunel"/>
<svg id="visid1501373e-59b3-11e8-855e-002590fb7074" width="500" height="400"></svg>





    <IPython.core.display.Javascript object>



<a id="incomeoncreditscoredistribution"></a>
## 4.View income on credit score distribution.
Do we see trends or limits in credit score or income for accepted loan applications? We can observe graphically that the larger are the credit score and income values the more accepted approval we get.


```python
%brunel data('df') x(income) y(creditScore) color(approval:yellow-green) tooltip(#all) :: width=800, height=300 
```


<!--
  ~ Copyright (c) 2015 IBM Corporation and others.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ You may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->


<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/brunel.2.3.css">
<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/sumoselect.css">

<style>
    
</style>

<div id="controlsid5c7f8500-59b5-11e8-855e-002590fb7074" class="brunel"/>
<svg id="visid5c7f8334-59b5-11e8-855e-002590fb7074" width="800" height="300"></svg>





    <IPython.core.display.Javascript object>



<a id="loanamountoncreditscoredistribution"></a>
## 5.View loan amount / credit score distribution-
Do we see limits in score or amount for accepted loan applications? We observe that as expected:
- the higher the loan amount, the higher the rejection rate.
- the lower credit score, the higher the rejection rate.

We observe the absence of green points identified for loan amount greater that USD 1 000 000. It is consistent with a rule that rejects the application for amounts greater than this threshold.


```python
%brunel data('df') x(loanAmount) y(creditScore) color(approval:yellow-green) tooltip(#all) :: width=800, height=300
```


<!--
  ~ Copyright (c) 2015 IBM Corporation and others.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ You may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->


<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/brunel.2.3.css">
<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/sumoselect.css">

<style>
    
</style>

<div id="controlsid185c2006-59b3-11e8-855e-002590fb7074" class="brunel"/>
<svg id="visid185c1e12-59b3-11e8-855e-002590fb7074" width="800" height="300"></svg>





    <IPython.core.display.Javascript object>



<a id="viewamountdistribution"></a>
## 5.Loan amount distribution.
The amount of loan applications visualized into a bar chart pie chart.
Bar chart shows a balanced distribution as input data have been ramdomly generated.


```python
%brunel data('df') bar x(loanAmount) y(#count) bin(loanAmount) style("size:100%") :: width=800, height=300
```


<!--
  ~ Copyright (c) 2015 IBM Corporation and others.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ You may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->


<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/brunel.2.3.css">
<link rel="stylesheet" type="text/css" href="/data/jupyter2/c7560d1f-8e2f-4189-8b1f-e2fb2aec8878/nbextensions/brunel_ext/sumoselect.css">

<style>
    #visid1e58e84a-59b3-11e8-855e-002590fb7074.brunel .chart1 .element1 .element {
	size: 100%;
}
</style>

<div id="controlsid1e58ea20-59b3-11e8-855e-002590fb7074" class="brunel"/>
<svg id="visid1e58e84a-59b3-11e8-855e-002590fb7074" width="800" height="300"></svg>





    <IPython.core.display.Javascript object>



<a id="next"></a>
# Summary and next steps
You have manipulated dataframes and views of a decision set powered by IBM ODM and captured in a CSV format. You can expand this notebook by adapting the views and adding new ones to get more insights about your decisions and make better decisions in the future.

Copyright © 2018 IBM. This notebook and its source code are released under the terms of the MIT License.

<a id="authors"></a>
## Authors

Pierre Feillet is engineer at the IBM Decision Lab. Pierre is architect in decision automation, and is passionate about data science and machine learning.

Copyright © 2018 IBM. This notebook and its source code are released under the terms of the MIT License.
