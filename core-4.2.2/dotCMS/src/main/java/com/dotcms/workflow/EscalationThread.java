package com.dotcms.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIImpl;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.JNDIUtil;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class EscalationThread extends DotStatefulJob {

	private String roleNameTemp = "Multiple Approval";

	private String actionClassName = "Require Multiple Approvers";

	private final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();

	public void run(JobExecutionContext jobContext) throws JobExecutionException {
		System.out.println("-------------------------------------------------");
		System.out.println("## EscalationThread  run :::: " + LocalDateTime.now());
		System.out.println("-------------------------------------------------");

		Logger.info(this, "## EscalationThread  run :::: " + LocalDateTime.now());

		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		final String wfActionAssign = Config.getStringProperty("ESCALATION_DEFAULT_ASSIGN", "");
		final String wfActionComments = Config.getStringProperty("ESCALATION_DEFAULT_COMMENT", "Task time out");

		/* if(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level) { */
		try {
			HibernateUtil.startTransaction();
			List<WorkflowTask> tasks = wapi.findExpiredTasks();

			System.out.println("## ec ##expired Tasks " + tasks.size());
			Logger.info(this, "## ec ##expired Tasks :::: " + tasks.size());

			for (WorkflowTask task : tasks) {
				String stepId = task.getStatus();
				WorkflowStep step = wapi.findStep(stepId);
				String actionId = step.getEscalationAction();

				Integer stepOrder = step.getMyOrder();
				System.out.println("step Order ::: " + stepOrder);
				Logger.info(this, "step Order ::: :::: " + stepOrder);

				WorkflowAction action = wapi.findAction(actionId, APILocator.getUserAPI().getSystemUser());

				// test for next users manager
				System.out.println("next Assign" + task.getWebasset());

				// end test

				Logger.info(this,
						"Task '" + task.getTitle() + "' " + "on contentlet id '" + task.getWebasset() + "' "
								+ "timeout on step '" + step.getName() + "' " + "excecuting escalation action '"
								+ action.getName() + "'");

				// find contentlet for default language
				Contentlet def = APILocator.getContentletAPI().findContentletByIdentifier(task.getWebasset(), false,
						APILocator.getLanguageAPI().getDefaultLanguage().getId(),
						APILocator.getUserAPI().getSystemUser(), false);

				System.out.println(
						"action id --> " + actionId + "  :wfActionId  -->  " + def.getStringProperty("wfActionId"));

				// changes made by chandrashekar for identify task is expired

				System.out.println("Task Modify Date :: " + task.getModDate());

				int escaltionSeconds = step.getEscalationTime();

				Boolean taskEculate = getComapreOfDate(task.getModDate(), escaltionSeconds);

				System.out.println("task eculate --> " + taskEculate);
				Logger.info(this, "task eculate --> " + taskEculate);

				String inode = def.getInode();

				// end changes

				// No need to escalate if the contentlet already is in the
				// Action Escalated.
				if (taskEculate) {

					/*
					 * if (UtilMethods.isSet(actionId) &&
					 * !actionId.equals(def.getStringProperty("wfActionId"))) {
					 */

					// if the worflow requires a checkin
					if (action.requiresCheckout()) {
						System.out.println("## ec ## requires CheckOUt");
						Contentlet c = APILocator.getContentletAPI().checkout(inode,
								APILocator.getUserAPI().getSystemUser(), false);
						c.setStringProperty("wfActionId", action.getId());
						c.setStringProperty("wfActionComments", wfActionComments);
						c.setStringProperty("wfActionAssign", wfActionAssign);

						APILocator.getContentletAPI().checkin(c, APILocator.getUserAPI().getSystemUser(), false);
					}

					// if the worflow requires a checkin
					else {
						System.out.println("## ec ## requires no CheckOUt fireWorkflowNoCheckin");
						Contentlet c = APILocator.getContentletAPI().find(inode,
								APILocator.getUserAPI().getSystemUser(), false);

						WorkflowProcessor processor = new WorkflowProcessor(c, APILocator.getUserAPI().getSystemUser());

						// First Level required Approval's step
						if (stepOrder.equals(1)) {

							c.setStringProperty("wfActionId", actionId);
							c.setStringProperty("wfActionComments", "Eculated");
							c.setStringProperty("wfActionAssign", action.getNextAssign());
							c.setBoolProperty("escalationThread", true);

							System.out.println("Task  eculated ::: " + task.getTitle());
							Logger.info(this, "Task  eculated   :::: " + task.getTitle());

						}

						/*
						 * Boolean isNotifyUser;
						 * 
						 * try { isNotifyUser = c.getBoolProperty("notifyuser");
						 * System.out.println("notifyuser  " + isNotifyUser); }
						 * 
						 * catch (Exception e) { isNotifyUser = false;
						 * System.out.println("notifyuser property not found " +
						 * isNotifyUser);
						 * 
						 * }
						 */

						// Second Level Manager step
						/*
						 * if (stepOrder.equals(2) &&
						 * isNotifyUser.equals(false)) {
						 */

						if (stepOrder.equals(2)) {

							String role = action.getNextAssign();

							List<User> users = notifyToNextAssign(role);
							System.out.println(users);

							sendMail(users);

							c.setBoolProperty("notifyuser", true);

							System.out.println("Task  notify ::: " + task.getTitle());
							Logger.info(this, "Task  notify    :::: " + task.getTitle());

						}

						// Second Level required Approval's step
						if (stepOrder.equals(3)) {

							c.setStringProperty("wfActionId", actionId);
							c.setStringProperty("wfActionComments", "Eculated");
							c.setStringProperty("wfActionAssign", action.getNextAssign());
							c.setBoolProperty("escalationThread", true);

							System.out.println("Task  eculated ::: " + task.getTitle());
							Logger.info(this, "Task  eculated    :::: " + task.getTitle());

						}

						// Second Level Manager step
						if (stepOrder.equals(4)) {

							/*
							 * String role = action.get
							 * 
							 * List<User> users = notifyToNextAssign(role);
							 * 
							 * System.out.println(users);
							 * 
							 * sendMail(users);
							 */

						}

						if (!processor.inProcess()) {
						} else {
							fireWorkflowPostCheckin(processor, c);
						}
					}
					/* } */
				}
				HibernateUtil.closeAndCommitTransaction();

			}
		} catch (Exception ex) {
			Logger.warn(this, ex.getMessage(), ex);

			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e) {
			}
		} finally {

			HibernateUtil.closeSessionSilently();
		}
		/* } */

		System.out.println("::: EscalationThread  end :::: ");
		Logger.info(this, "## EscalationThread  end :::: ");
	}

	// New Method for sending mail
	private void sendMail(List<User> users) {

		Session s = null;
		try {
			Context ctx = (Context) new InitialContext();
			s = (javax.mail.Session) JNDIUtil.lookup(ctx, "mail/MailSession");
		} catch (NamingException e1) {
			Logger.error(this, e1.getMessage(), e1);
		}

		if (s == null) {
			Logger.debug(this, "No Mail Session Available.");

		}

		String smtpServer = s.getProperty("mail.smtp.host");

		String port = s.getProperty("mail.smtp.port");
		String userName = s.getProperty("mail.smtp.user");
		String password = s.getProperty("mail.smtp.password");

		Properties propsNew = new Properties();
		propsNew.put("mail.smtp.auth", "true");
		propsNew.put("mail.smtp.starttls.enable", "true");
		propsNew.put("mail.smtp.host", smtpServer);
		propsNew.put("mail.smtp.port", port);

		for (User user : users) {

			String toUserEmail = user.getEmailAddress();
			String assignName = user.getFirstName() + " " + user.getLastName();
			System.out.print(":: toUserEmail : " + toUserEmail);
			System.out.print(":: assignName : " + assignName);
			try {
				customSendMsg(propsNew, toUserEmail, userName, password, assignName);
			}

			catch (Exception e) {
				Logger.error(this, "Error Sending Mail to " + toUserEmail);
			}
		}

	}

	public Boolean customSendMsg(Properties props, String toUserEmail, String from, String password, String name) {

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(from, password);
			}
		});

		Boolean result = false;
		try {
			Message msg2 = new MimeMessage(session);
			msg2.setFrom(new InternetAddress(from));
			msg2.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toUserEmail));
			msg2.setSubject("Notify ");

			msg2.setText("Hello  " + name + "\n\n This is test mail No spam to my email, please!");

			Transport.send(msg2);
			result = true;

		} catch (Exception e) {
			Logger.error(this, "Error Sending Message", e);
			result = false;
		}
		return result;
	}

	private List<User> notifyToNextAssign(String role)
			throws NoSuchUserException, DotDataException, DotSecurityException {
		List<User> users = APILocator.getRoleAPI().findUsersForRole(role);

		System.out.println("Users :: " + users);

		return users;
	}

	// added new method by chandrashekar to validate the task
	private static Boolean getComapreOfDate(Date taskModifyDate, int expireSeconds) {

		Date currentDate = new Date();

		taskModifyDate.setSeconds(expireSeconds);

		System.out.println("expireDate , currentDate " + taskModifyDate + ": " + currentDate);

		if (taskModifyDate.compareTo(currentDate) == 0) {
			System.out.println("expireDate is equal to currentDate");
			return true;
		} else if (taskModifyDate.compareTo(currentDate) > 0) {
			System.out.println("expireDate is after currentDate");
			return false;
		} else if (taskModifyDate.compareTo(currentDate) < 0) {
			System.out.println("expireDate is before currentDate");
			return true;
		} else {
			System.out.println("error ");
		}
		return false;
	}

	// end validate task

	// added to esculate
	public void fireWorkflowPostCheckin(WorkflowProcessor processor, Contentlet contentlet)
			throws DotDataException, DotWorkflowException {

		boolean local = false;

		try {
			if (!processor.inProcess()) {
				return;
			}

			if (!processor.getActionClasses().isEmpty()) {

				// Set<User> requiredApprovers =
				// getRequiredApprovers(processor);

				local = HibernateUtil.startLocalTransactionIfNeeded();

				processor.getContentlet().setStringProperty("wfActionId", processor.getAction().getId());

				String comments = contentlet.getStringProperty("wfActionComments");

				processor.setWorkflowMessage(comments);

				WorkflowTask task = processor.getTask();
				if (task != null) {
					Role r = APILocator.getRoleAPI().getUserRole(processor.getUser());
					if (task.isNew()) {

						task.setCreatedBy(r.getId());
						task.setWebasset(processor.getContentlet().getIdentifier());
						if (processor.getWorkflowMessage() != null) {
							task.setDescription(processor.getWorkflowMessage());
						}
					}
					task.setTitle(processor.getContentlet().getTitle());
					task.setModDate(new java.util.Date());
					if (processor.getNextAssign() != null)
						task.setAssignedTo(processor.getNextAssign().getId());
					task.setStatus(processor.getNextStep().getId());

					saveWorkflowTask(task, processor);
					if (processor.getWorkflowMessage() != null) {
						WorkflowComment comment = new WorkflowComment();
						comment.setComment(processor.getWorkflowMessage());

						comment.setWorkflowtaskId(task.getId());
						comment.setCreationDate(new Date());
						comment.setPostedBy(r.getId());
						saveComment(comment);
					}
				}

				List<WorkflowActionClass> actionClasses = processor.getActionClasses();
				if (actionClasses != null) {
					for (WorkflowActionClass actionClass : actionClasses) {
						WorkFlowActionlet actionlet = actionClass.getActionlet();
						Map<String, WorkflowActionClassParameter> paramss = findParamsForActionClass(actionClass);
						// actionlet.executeAction(processor, paramss);

						// if we should stop processing further actionlets
						if (actionlet.stopProcessing()) {
							break;
						}
					}
				}
				if (UtilMethods.isSet(processor.getContentlet())) {
					APILocator.getContentletAPI().refresh(processor.getContentlet());
				}
				if (local) {
					HibernateUtil.closeAndCommitTransaction();
				}

			}
		} catch (Exception e) {
			if (local) {
				HibernateUtil.rollbackTransaction();
			}
			/* Show a more descriptive error of what caused an issue here */
			Logger.error(WorkflowAPIImpl.class, "There was an unexpected error: " + e.getMessage(), e);

			System.out.println("Multiple Required sub action missing");
			throw new DotWorkflowException(e.getMessage(), e);
		} finally {
			if (local) {

				HibernateUtil.closeSessionSilently();
			}
		}

	}

	private Set<User> getRequiredApprovers(WorkflowProcessor processor) throws DotDataException {

		Set<User> requiredApprovers = new HashSet<>();

		Map<String, WorkflowActionClassParameter> params = findParamsForActionClass(
				processor.getActionClasses().stream().filter(a -> a.getName().equals(actionClassName)).findAny().get());

		String userIds = (params.get("approvers") == null) ? "" : params.get("approvers").getValue();

		List<String> myList = new ArrayList<String>(Arrays.asList(userIds.split(",")));

		for (String userIdOrMail : myList) {
			List<User> users = APILocator.getUserAPI().getUsersByNameOrEmailOrUserID(userIdOrMail, 0, 1);

			requiredApprovers.addAll(users);
		}

		return requiredApprovers;
	}

	@WrapInTransaction
	public void saveWorkflowTask(WorkflowTask task, WorkflowProcessor processor) throws DotDataException {
		saveWorkflowTask(task);
		WorkflowHistory history = new WorkflowHistory();
		history.setWorkflowtaskId(task.getId());
		history.setActionId(processor.getAction().getId());
		history.setCreationDate(new Date());
		history.setMadeBy(processor.getUser().getUserId());
		history.setStepId(processor.getNextStep().getId());

		String comment = (UtilMethods.isSet(processor.getWorkflowMessage())) ? processor.getWorkflowMessage() : "";
		String nextAssignName = (UtilMethods.isSet(processor.getNextAssign())) ? processor.getNextAssign().getName()
				: "";

		try {
			history.setChangeDescription(
					LanguageUtil.format(processor.getUser().getLocale(), "workflow.history.description",
							new String[] { processor.getUser().getFullName(), processor.getAction().getName(),
									processor.getNextStep().getName(), nextAssignName, comment },
							false));
		} catch (LanguageException e) {
			Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
		}
		saveWorkflowHistory(history);
	}

	@WrapInTransaction
	public void saveWorkflowTask(WorkflowTask task) throws DotDataException {
		workFlowFactory.saveWorkflowTask(task);
	}

	@WrapInTransaction
	public void saveWorkflowHistory(WorkflowHistory history) throws DotDataException {
		workFlowFactory.saveWorkflowHistory(history);
	}

	@WrapInTransaction
	public void saveComment(WorkflowComment comment) throws DotDataException {
		if (UtilMethods.isSet(comment.getComment())) {
			workFlowFactory.saveComment(comment);
		}
	}

	@CloseDBIfOpened
	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass)
			throws DotDataException {

		System.out.print("## ## ");
		return workFlowFactory.findParamsForActionClass(actionClass);
	}

	private Role getTempRoleEculate(User user) throws DotStateException, DotDataException {

		RoleAPI roleAPI = APILocator.getRoleAPI();

		List<Role> roles = roleAPI.findRolesByNameFilter(roleNameTemp, 0, 1);

		if (!roles.isEmpty()) {

			roleAPI.addRoleToUser(roleNameTemp, user);

			return roleAPI.loadRoleById(roleNameTemp);
		}
		return new Role();

	}

	public Set<User> getHasApproved(WorkflowProcessor processor, Set<User> requiredApprovers) {

		List<WorkflowHistory> histories = processor.getHistory();

		Set<User> hasApproved = new HashSet<User>();

		// add this approval to the history
		WorkflowHistory h = new WorkflowHistory();
		h.setActionId(processor.getAction().getId());
		h.setMadeBy(processor.getUser().getUserId());
		if (histories == null) {
			histories = new ArrayList<WorkflowHistory>();
			histories.add(h);
		} else
			histories.add(h);

		for (User u : requiredApprovers) {

			for (WorkflowHistory history : histories) {
				if (history.getActionId().equals(processor.getAction().getId())) {
					if (u.getUserId().equals(history.getMadeBy())) {
						hasApproved.add(u);
					}

				}

			}

		}
		return hasApproved;
	}

}
