/*
 * Copyright 2015  FastJ
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fastj.fit.jenkins;

import java.util.ArrayList;
import java.util.List;

import org.fastj.fit.intf.DataInvalidException;
import org.fastj.fit.intf.ParamIncertitudeException;
import org.fastj.fit.intf.TCNode;
import org.fastj.fit.intf.TResult;
import org.fastj.fit.intf.TSuite;
import org.fastj.fit.tool.StringUtil;

/**
 * @author zhouqingquan
 *
 */
public class TCRNode implements TCNode{

	long startTime = 0L;
	long endTime = 0L;
	String log = "";
	String name = "";
	String tid = "";
	int result = PASS;
	List<TResult> results = new ArrayList<>();
	TSuite suite = null;
	
	public TCRNode(TCNode tcase, TResult tr) {
		
		startTime = tr.getStart();
		endTime = tr.getEnd();
		log = tr.getLog();
		result = tr.getResult();
		results.add(tr);
		suite = tcase.getSuite();
		try {
			name = StringUtil.expend(tcase.getName(), tr.getLoopData());
		} catch (ParamIncertitudeException | DataInvalidException e) {
			name = tcase.getName();
		}
		try {
			tid = StringUtil.expend(tcase.getTid(), tr.getLoopData());
		} catch (ParamIncertitudeException | DataInvalidException e) {
			tid = tcase.getTid();
		}
	}
	
	@Override
	public long getEndTime() {
		return endTime;
	}

	@Override
	public String getLog() {
		return log;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getResult() {
		return result;
	}

	@Override
	public List<TResult> getResults() {
		return results;
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public TSuite getSuite() {
		return suite;
	}

	@Override
	public String getTid() {
		return tid;
	}

}
