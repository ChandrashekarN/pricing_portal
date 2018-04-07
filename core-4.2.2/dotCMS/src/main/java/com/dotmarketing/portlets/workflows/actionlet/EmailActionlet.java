package com.dotmarketing.portlets.workflows.actionlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.DNSUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.util.JNDIUtil;

public class EmailActionlet extends WorkFlowActionlet {

	private static final long serialVersionUID = 1L;

	@Override
	public List<WorkflowActionletParameter> getParameters() {
		List<WorkflowActionletParameter> params = new ArrayList<WorkflowActionletParameter>();

		params.add(new WorkflowActionletParameter("fromEmail", "From Email", "", true));
		params.add(new WorkflowActionletParameter("fromName", "From Name", "", true));
		params.add(new WorkflowActionletParameter("toEmail", "To Email", "", true));
		params.add(new WorkflowActionletParameter("toName", "To Name", "", true));
		params.add(new WorkflowActionletParameter("cc", "Cc Email", "", false));
		params.add(new WorkflowActionletParameter("bcc", "Bcc Email", "", false));
		params.add(new WorkflowActionletParameter("emailSubject", "Email Subject", "", true));
		params.add(new WorkflowActionletParameter("emailBody", "Email Body (html)", "", true));
		params.add(new WorkflowActionletParameter("condition",
				"Condition - email will send unless<br>velocity prints 'false'", "", false));
		params.add(new WorkflowActionletParameter("attachment1",
				"Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
		params.add(new WorkflowActionletParameter("attachment2",
				"Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
		params.add(new WorkflowActionletParameter("attachment3",
				"Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
		params.add(new WorkflowActionletParameter("attachment4",
				"Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
		params.add(new WorkflowActionletParameter("attachment5",
				"Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));

		return params;
	}

	@Override
	public String getName() {
		return "Send an Email";
	}

	@Override
	public String getHowTo() {
		return "This actionlet will send an email that can be based on the submitted content. The value of every field here is parsed velocity.  So, to send a custom email to the email address stored in a field called userEmail, put $content.userEmail in the 'to email' field and the system will replace it with the variables from the content";
	}

	@Override
	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
			throws WorkflowActionFailureException {
		// changes by chandu

		String scheme = processor.getScheme().getName();
		String actionName = processor.getAction().getName();
		String nextStep = processor.getAction().getNextStep();
		String nextAssignId = processor.getNextAssign().getId();

		String nextAssignFQN = processor.getNextAssign().getFQN();

		RoleAPI roleAPI = APILocator.getRoleAPI();
		List<User> userList = new ArrayList<>();

		try {
			Role role = roleAPI.findRoleByFQN(nextAssignFQN);

			try {
				userList = roleAPI.findUsersForRole(role.getId());

			} catch (NoSuchUserException | DotSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (DotDataException e2) {
			e2.printStackTrace();
		}
		System.out.println("## User size ::" + userList.size());
		for (User user : userList) {
			System.out.println("## User " + user);
		}

		String toUserEmail = "";
		String assignName = "";
		String check = "";

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
		// start edited @chandu
		String port = s.getProperty("mail.smtp.port");
		String userName = s.getProperty("mail.smtp.user");
		String password = s.getProperty("mail.smtp.password");

		Properties propsNew = new Properties();
		propsNew.put("mail.smtp.auth", "true");
		propsNew.put("mail.smtp.starttls.enable", "true");
		propsNew.put("mail.smtp.host", smtpServer);
		propsNew.put("mail.smtp.port", port);

		for (User user : userList) {

			toUserEmail = user.getEmailAddress();
			assignName = user.getFirstName() + " " + user.getLastName();
			System.out.print(":: toUserEmail : " + toUserEmail);
			System.out.print(":: assignName : " + assignName);

			Boolean resultOFSentMail = customSendMsg(propsNew, toUserEmail, userName, password, assignName);

			if (resultOFSentMail) {
				check = null;
			}

		}

		// end changes by chandu

		if (check != null) {

			System.out.print("params size   " + params.size());
			Contentlet c = processor.getContentlet();

			String rev = c.getStringProperty("ipAddress");
			try {
				rev = DNSUtil.reverseDns(rev);
				c.setStringProperty("ipAddress", rev);
			} catch (Exception e) {
				Logger.error(this.getClass(), "error on reverse lookup" + e.getMessage());
			}

			try {
				// get the host of the content
				Host host = APILocator.getHostAPI().find(processor.getContentlet().getHost(),
						APILocator.getUserAPI().getSystemUser(), false);
				if (host.isSystemHost()) {
					host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
				}

				HttpServletRequest requestProxy = new MockHttpRequest(host.getHostname(), null).request();
				HttpServletResponse responseProxy = new BaseResponse().response();
				org.apache.velocity.context.Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
				ctx.put("host", host);
				ctx.put("host_id", host.getIdentifier());
				ctx.put("user", processor.getUser());
				ctx.put("workflow", processor);
				ctx.put("stepName", processor.getStep().getName());
				ctx.put("stepId", processor.getStep().getId());
				ctx.put("nextAssign", processor.getNextAssign().getName());
				ctx.put("workflowMessage", processor.getWorkflowMessage());
				ctx.put("nextStepResolved", processor.getNextStep().isResolved());
				ctx.put("nextStepId", processor.getNextStep().getId());
				ctx.put("nextStepName", processor.getNextStep().getName());
				ctx.put("workflowTaskTitle", UtilMethods.isSet(processor.getTask().getTitle())
						? processor.getTask().getTitle() : processor.getContentlet().getTitle());
				ctx.put("modDate", processor.getTask().getModDate());
				ctx.put("structureName", processor.getContentlet().getStructure().getName());

				ctx.put("contentlet", c);
				ctx.put("content", c);

				String toEmail = params.get("toEmail").getValue();
				String toName = params.get("toName").getValue();
				String fromEmail = params.get("fromEmail").getValue();
				String fromName = params.get("fromName").getValue();
				String emailSubject = params.get("emailSubject").getValue();
				String emailBody = params.get("emailBody").getValue();
				String condition = params.get("condition").getValue();
				String cc = params.get("cc").getValue();
				String bcc = params.get("bcc").getValue();

				if (UtilMethods.isSet(condition)) {
					condition = VelocityUtil.eval(condition, ctx);
					if (UtilMethods.isSet(condition) && condition.indexOf("false") > -1) {
						Logger.info(this.getClass(), processor.getAction().getName()
								+ " email condition contains false, skipping email send");
						return;
					}
				}

				if (UtilMethods.isSet(toEmail)) {
					toEmail = VelocityUtil.eval(toEmail, ctx);
				}
				if (UtilMethods.isSet(toName)) {
					toName = VelocityUtil.eval(toName, ctx);
				}
				if (UtilMethods.isSet(fromEmail)) {
					fromEmail = VelocityUtil.eval(fromEmail, ctx);
				}

				if (UtilMethods.isSet(fromName)) {
					fromName = VelocityUtil.eval(fromName, ctx);
				}
				if (UtilMethods.isSet(emailSubject)) {
					emailSubject = VelocityUtil.eval(emailSubject, ctx);
				}
				if (UtilMethods.isSet(emailBody)) {
					emailBody = VelocityUtil.eval(emailBody, ctx);
				}

				Mailer mail = new Mailer();

				InternetAddress to = new InternetAddress(toName + "<" + toEmail + ">");
				mail.setToEmail(to.toString());

				mail.setFromEmail(fromEmail);
				mail.setFromName(fromName);
				mail.setSubject(emailSubject);

				mail.setHTMLAndTextBody(emailBody);

				if (UtilMethods.isSet(cc)) {
					cc = VelocityUtil.eval(cc, ctx);
					mail.setCc(cc);
				}
				if (UtilMethods.isSet(bcc)) {
					bcc = VelocityUtil.eval(bcc, ctx);
					mail.setBcc(bcc);
				}

				for (int x = 1; x < 6; x++) {
					String attachment = params.get("attachment" + x).getValue();

					if (UtilMethods.isSet(attachment)) {
						Host fileHost = host;
						File f = null;
						try {
							if (attachment.indexOf("/") == -1
									&& processor.getContentlet().getBinary(attachment).exists()) {
								f = processor.getContentlet().getBinary(attachment);
							} else {
								String hostname = attachment;
								String filename = attachment;
								if (hostname.startsWith("//")) {
									hostname = hostname.substring(2, hostname.length());
									filename = hostname.substring(hostname.indexOf("/"), hostname.length());
									hostname = hostname.substring(0, hostname.indexOf("/"));
									fileHost = WebAPILocator.getHostWebAPI().resolveHostName(hostname,
											processor.getUser(), true);

								}

								Identifier id = APILocator.getIdentifierAPI().find(fileHost, filename);
								ContentletVersionInfo vinfo = APILocator.getVersionableAPI().getContentletVersionInfo(
										id.getId(), processor.getContentlet().getLanguageId());
								Contentlet cont = APILocator.getContentletAPI().find(vinfo.getLiveInode(),
										processor.getUser(), true);
								FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(cont);
								f = fileAsset.getFileAsset();
							}
							if (f != null && f.exists()) {
								mail.addAttachment(f);
							}
						} catch (Exception e) {
							Logger.error(this.getClass(), "Unable to get file attachment: " + e.getMessage());
						}
					}
				}

				mail.sendMessage();

			} catch (Exception e) {
				Logger.error(EmailActionlet.class, e.getMessage(), e);
			}

		}
	}

	// changes by chandu
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
			msg2.setSubject("Testing Subject");

			msg2.setText("Hello  " + name + "\n\n This is test mail No spam to my email, please!");

			Transport.send(msg2);
			result = true;

		} catch (Exception e) {
			Logger.error(this, "Error Sending Message", e);
			result = false;
		}
		return result;

	}

	// changes end -chandu

}