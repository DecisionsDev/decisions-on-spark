/*
* Copyright IBM Corp. 1987, 2018
* 
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
* 
**/

package loan;

import java.util.Date;

public class Bankruptcy implements java.io.Serializable {

	private static final long serialVersionUID = 3107842066700686170L;
	private Date date;
	private int chapter;
	private String reason;

	@SuppressWarnings("unused")
	private Bankruptcy() {
	}

	public Bankruptcy(Date date, int chapter, String reason) {
		this.date = date;
		this.chapter = chapter;
		this.reason = reason;
	}

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}

	public String getReason() {
		return reason;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public int getChapter() {
		return this.chapter;
	}
	
	public void setChapter(int chapter) {
		this.chapter = chapter;
	}
}
