package org.fastj.fit.jenkins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.fastj.fit.intf.TCNode;
import org.fastj.fit.intf.TProject;
import org.fastj.fit.intf.TSuite;
import org.fastj.fit.log.LogUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLJUnitResult implements Constants{

    private static final double ONE_SECOND = 1000.0;
    private static final String UNKNOWN = "unknown";

    private Document doc;
    private Element rootElement;
    private OutputStream out;
    protected TSNode node;
    private File tsRlt = null;

    public void startTestSuite(TProject tproj, TSuite suite, long time) {
    	node = new TSNode();
    	node.setName(suite.getName());
    	node.setStart(time);
    	
        doc = domBuilder().newDocument();
        rootElement = doc.createElement(TESTSUITE);
        String n = suite.getName();
        rootElement.setAttribute(ATTR_NAME, n == null ? UNKNOWN : n);

        final String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date(node.getStartTime()));
        rootElement.setAttribute(TIMESTAMP, timestamp);
        rootElement.setAttribute(HOSTNAME, getHostname());
        
        try {
        	tsRlt = tproj.getLogFile(suite.getName().replace('.','_') + "_rlt.xml");
			out = new FileOutputStream(tsRlt);
		} catch (Throwable e) {
			LogUtil.error("Create TSLog fail: e={}", e.getMessage());
		}
    }

    private String getHostname()  {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    public void count(TSNode cnter)
    {
    	cnter.setRunCnt(cnter.runCount() + node.runCount());
    	cnter.setFailureCnt(cnter.failureCount() + node.failureCount());
    }
    
    public void endTestSuite() {
    	TSNode suite = node;
    	
        rootElement.setAttribute(ATTR_TESTS, "" + suite.runCount());
        rootElement.setAttribute(ATTR_FAILURES, "" + suite.failureCount());
        rootElement.setAttribute(ATTR_ERRORS, "" + suite.errorCount());
        rootElement.setAttribute(ATTR_SKIPPED, "" + suite.skipCount());
        rootElement.setAttribute(ATTR_TIME, "" + (suite.getRunTime() / ONE_SECOND));
        if (out != null) {
            Writer wri = null;
            try {
                wri = new BufferedWriter(new OutputStreamWriter(out, "UTF8"));
                wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                (new DOMElementWriter()).write(rootElement, wri, 0, "  ");
            } catch (IOException exc) {
            	LogUtil.error("Write fit xml result fail: e={}", exc.getMessage());
            } finally {
                if (wri != null) {
                    try {
                        wri.flush();
                    } catch (IOException ex) {
                    	LogUtil.error("Close writer fail: e={}", ex.getMessage());
                    }
                    
					try {
						out.close();
					} catch (IOException e) {
						LogUtil.error("Close writer fail: e={}", e.getMessage());
					}
                    
                }
            }
        }
    }

	public void appendNode(TCNode test) {
		Element currentTest = doc.createElement(TESTCASE);
		String n = test.getName() == null ? UNKNOWN : test.getName();
		String id = test.getTid() == null ? UNKNOWN : test.getTid();
		currentTest.setAttribute(ATTR_NAME, n);
		currentTest.setAttribute(ATTR_ID, id);
		
		String tsn = test.getSuite().getName();
		tsn = tsn == null ? "envSetup" : tsn.contains("__L") ? tsn.substring(0, tsn.indexOf("__L")) : tsn;
		tsn = tsn.replaceAll("\\.", "_");
		currentTest.setAttribute(ATTR_CLASSNAME, tsn + "." + id);
		currentTest.setAttribute(ATTR_RESULT, String.valueOf(test.getResult()));
		
		rootElement.appendChild(currentTest);
		formatOutput(currentTest, test.getResult() == TCNode.PASS ? SYSTEM_OUT : SYSTEM_ERR, test.getLog());

		if (test.getResult() != TCNode.PASS)
		{
			Element fe = doc.createElement(FAILURE);
			fe.setAttribute(ATTR_TYPE, "ERROR");
			currentTest.appendChild(fe);
		}
		
		currentTest.setAttribute(ATTR_TIME, "" + ((test.getEndTime() - test.getStartTime()) / ONE_SECOND));
	}

    private void formatOutput(Element p, String type, String output) {
        Element nested = doc.createElement(type);
        p.appendChild(nested);
        nested.appendChild(doc.createCDATASection(output));
    }
    
    private DocumentBuilder domBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

}

interface Constants {
    String TESTSUITES = "testsuites";
    String TESTSUITE = "testsuite";
    String TESTCASE = "testcase";
    String ERROR = "error";
    String FAILURE = "failure";
    String SYSTEM_ERR = "system-err";
    String SYSTEM_OUT = "system-out";
    String ATTR_PACKAGE = "package";
    String ATTR_NAME = "name";
    String ATTR_TIME = "time";
    String ATTR_ERRORS = "errors";
    String ATTR_FAILURES = "failures";
    String ATTR_TESTS = "tests";
    String ATTR_SKIPPED = "skipped";
    String ATTR_TYPE = "type";
    String ATTR_MESSAGE = "message";
    String PROPERTIES = "properties";
    String PROPERTY = "property";
    String ATTR_VALUE = "value";
    String ATTR_CLASSNAME = "classname";
    String ATTR_ID = "id";
    String ATTR_RESULT = "result";
    String TIMESTAMP = "timestamp";
    String HOSTNAME = "hostname";
}

