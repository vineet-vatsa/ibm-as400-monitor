/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uptimesoftware.uptime.plugin;

import java.io.*;
import java.util.*;
import java.math.*;
import java.lang.Thread.*;
import com.ibm.as400.access.*;
import com.ibm.as400.resource.*;
import ro.fortsoft.pf4j.PluginWrapper;
import com.uptimesoftware.uptime.plugin.api.Extension;
import com.uptimesoftware.uptime.plugin.api.Plugin;
import com.uptimesoftware.uptime.plugin.api.PluginMonitor;
import com.uptimesoftware.uptime.plugin.monitor.MonitorState;
import com.uptimesoftware.uptime.plugin.monitor.Parameters;
/*
import com.uptimesoftware.uptime.erdc.baseclass.*;
import com.uptimesoftware.uptime.base.util.Parameters;
*/
/**
 *
 * @author chris
 */
public class MonitorAS400msgq extends Plugin {

    public MonitorAS400msgq(PluginWrapper wrapper) {
        super(wrapper);
    }
	
	@Extension
    public static class UptimeMonitorAS400 extends PluginMonitor {

		static int metricValue = new Integer(0);
		private String username = "";
		private String password = "";
		private String hostname = "";
		private String message = "";
		private String messageQueueFile = "";

		@Override
		public void setParameters(Parameters params) {
			username = params.getString("username");
			password = params.getString("password");
			messageQueueFile = params.getString("queueFile");
			hostname = params.getString("hostname");
		}

		public void monitor() {
			int checkPos = 0;
			String strMsgQIn = "";
			String strMsgQLib = "";
			String strMsgQ = "";
			String strMsgID = "";
			String strCompleteMsgQ = "";
			String strSys = "";

			try {
				AS400 as400 = new AS400(hostname);
				as400.setUserId(username);
				as400.setPassword(password);


				strMsgQ = messageQueueFile.substring(messageQueueFile.lastIndexOf("_") + 1, messageQueueFile.lastIndexOf("."));
				strMsgQLib = messageQueueFile.substring(messageQueueFile.indexOf("_") + 1, messageQueueFile.lastIndexOf("_"));
				strSys = messageQueueFile.substring(messageQueueFile.lastIndexOf("/") + 1, messageQueueFile.indexOf("_"));

				List<String> strMsgs = getQueueList(messageQueueFile);

				if (strMsgQLib.length() < 1) {
					strCompleteMsgQ = ("/QSYS.LIB/" + strMsgQ + ".MSGQ");
				} else {
					strCompleteMsgQ = ("/QSYS.LIB/" + strMsgQLib + ".LIB/" + strMsgQ + ".MSGQ");
				}

				RMessageQueue messageQueue = new RMessageQueue(as400, strCompleteMsgQ);
				Integer intSeverity = new Integer(40);
				messageQueue.setSelectionValue(RMessageQueue.SEVERITY_CRITERIA, intSeverity);

				messageQueue.open();
				messageQueue.waitForComplete();
				String msgText = "";
				long numberOfMessages = messageQueue.getListLength();
				Integer returnStatus = 0;

				for (long i = 0; i < numberOfMessages; ++i) {
					RQueuedMessage queuedMessage = (RQueuedMessage) messageQueue.resourceAt(i);

					for (String messageId : strMsgs) {
						int msgInString = queuedMessage.toString().indexOf(messageId);
						int msgInMsgTxt = msgText.toString().indexOf(queuedMessage.getAttributeValue(RQueuedMessage.MESSAGE_TEXT).toString());
						if (msgInString > -1 && msgInMsgTxt == -1) {
							if (msgInString > -1) {
								msgText = queuedMessage.getAttributeValue(RQueuedMessage.MESSAGE_ID) + " " + queuedMessage.getAttributeValue(RQueuedMessage.MESSAGE_TEXT);
								returnStatus = 1;
							}
						}
					}
				}

				messageQueue.close();

				if (returnStatus == 1) {
					setMessage("Messages in " + strMsgQ + " found: " + msgText);
					setState(MonitorState.WARN);

				} else if (returnStatus == 0) {
					setMessage("No monitored messages found in " + strMsgQ);
					setState(MonitorState.OK);
				}

			} catch (Exception e) {
				setState(MonitorState.UNKNOWN);
				setMessage("An exception occured running monitor. \r" + e.getMessage());
			}
		}

		public List<String> getQueueList(String fileName) {
			File file = new File(fileName);
			List<String> queueList = new ArrayList<String>();

			try {
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String line;
				while ((line = br.readLine()) != null) {
					queueList.add(line);
				}
			} catch (IOException e) {
				System.err.println("Problem, class fileReader: " + e);
			}
			return queueList;
		}
	}
}
