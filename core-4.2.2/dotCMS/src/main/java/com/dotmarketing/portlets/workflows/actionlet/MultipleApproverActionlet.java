package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.workflows.model.MultiEmailParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;

public class MultipleApproverActionlet extends WorkFlowActionlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {

		return "Require Multiple Approvers";
	}

	public String getHowTo() {

		return "This actionlet takes a comma separated list of userIds or "
				+ "user email addresses of users that need to approve this workflow task before it can progress. "
				+ "If eveyone in the list has not approved, this actionlet will send a notification email out to " +
						"users who have not approved and STOP all further subaction processing.";
	}

	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
			throws WorkflowActionFailureException {
		System.out.println("MultipleApproverActionlet");
	}

	@Override
	public boolean stopProcessing(){
		return shouldStop;
	}
	private boolean shouldStop = false;
	
	private static ArrayList<WorkflowActionletParameter> paramList = null; 
	@Override
	public List<WorkflowActionletParameter> getParameters() {
		if (paramList == null) {
			synchronized (this.getClass()) {
				if (paramList == null) {
					paramList = new ArrayList<WorkflowActionletParameter>();
					paramList.add(new MultiEmailParameter("approvers", "User IDs or Emails", null, true));
					paramList.add(new WorkflowActionletParameter("emailSubject", "Email Subject", "Multiple Approval Required", false));
					paramList.add(new WorkflowActionletParameter("emailBody", "Email Message", null, false));

				}
			}
		}
		return paramList;
	}

}
