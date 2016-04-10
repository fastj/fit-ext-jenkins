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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.fastj.fit.intf.PostProc;
import org.fastj.fit.intf.TCNode;
import org.fastj.fit.intf.TProject;
import org.fastj.fit.intf.TResult;
import org.fastj.fit.intf.TSuite;
import org.fastj.fit.log.LogUtil;

/**
 * Log&Result for Jenkins
 * 
 * @author zhouqingquan
 *
 */
public class JenkinsPostProc implements PostProc {

	TProject tproj;
	long start;
	long end;
	boolean filterSkipped = true;
	
	Map<String, XMLJUnitResult> emap = new HashMap<>();
	TSNode totalCnt = new TSNode();
	
	@Override
	public void end() {
		end = System.currentTimeMillis();
		
		for (XMLJUnitResult xjr : emap.values())
		{
			xjr.endTestSuite();
			xjr.count(totalCnt);
		}
		
		FileOutputStream glog = null;
		try {
			glog = new FileOutputStream(tproj.getLogFile("fit.log"));
			glog.write(LogUtil.getLog());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		finally
		{
			if (glog != null)
			{
				try {
					glog.close();
				} catch (IOException e) {
				}
			}
		}
		
		System.out.println("<=== End test at " + new Date());
		
		System.out.println("------------------------Report-------------------------\r\n");
		
		System.out.println("Total run: " + totalCnt.runCount());
		System.out.println("     PASS: " + (totalCnt.runCount() - totalCnt.failureCount()));
		System.out.println("     FAIL: " + totalCnt.failureCount());
		System.out.println("     SKIP: " + totalCnt.skipCount());
		
		System.out.println("\r\nAll test takes: " + (end - start)/1000. + " sec.");
		
	}

	@Override
	public void start(TProject tproj) {
		this.tproj = tproj;
		start = System.currentTimeMillis();
		filterSkipped = Boolean.valueOf(tproj.getSysVars().getPara("ignoreSkipped", "true"));
		System.out.println("===> Start test at " + new Date());
	}

	@Override
	public synchronized void start(TCNode tcn) {
		String tsname = tcn.getSuite().getName();
		synchronized (emap) {
			if (!emap.containsKey(tsname)) {
				emap.put(tsname, new XMLJUnitResult());
				emap.get(tsname).startTestSuite(tproj, tcn.getSuite(), tcn.getStartTime());
			}
		}
	}
	
	@Override
	public void finish(TCNode tcn) {
		
		if (tcn.getName().indexOf("${") > -1 || tcn.getTid().indexOf("${") > -1)
		{
			for (TResult tr : tcn.getResults())
			{
				TCRNode tcr = new TCRNode(tcn, tr);
				finish0(tcr);
			}
		}else
		{
			finish0(tcn);
		}
	}
	
	private synchronized void finish0(TCNode tcn)
	{
		if (tcn.getResult() == TCNode.REPLACED) return;
		
		if (tcn.getResult() == TCNode.SKIPPED)
		{
			totalCnt.setSkipCnt(totalCnt.skipCount() + 1);
		}
		
		if (filterSkipped && tcn.getResult() == TCNode.SKIPPED){
			return;
		}
		
		//ExtPoint: Set Result to 3rd Systems
		//ExtPoint: Write TestCase Log to 3rd Systems
		
		String tsname = tcn.getSuite().getName();
		emap.get(tsname).appendNode(tcn);
		
		TSNode tsn = emap.get(tsname).node;
		tsn.setEnd(tcn.getEndTime());
		tsn.setRunCnt(tsn.runCount() + 1);
		
		if (tcn.getResult() != TCNode.PASS && tcn.getResult() != TCNode.SKIPPED)
		{
			tsn.setFailureCnt(tsn.failureCount() + 1);
		}
	}

	@Override
	public void finish(TSuite suite) {
		//Can write log when TSuite done
	}

}
