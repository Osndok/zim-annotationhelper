package meta.works.zim.annotationhelper;

import com.github.sheigutn.pushbullet.Pushbullet;
import com.github.sheigutn.pushbullet.ephemeral.*;
import com.github.sheigutn.pushbullet.items.device.Device;
import com.github.sheigutn.pushbullet.items.push.sendable.defaults.SendableNotePush;
import com.github.sheigutn.pushbullet.items.push.sent.Push;
import com.github.sheigutn.pushbullet.items.push.sent.defaults.FilePush;
import com.github.sheigutn.pushbullet.items.push.sent.defaults.NotePush;
import com.github.sheigutn.pushbullet.stream.PushbulletWebsocketClient;
import com.github.sheigutn.pushbullet.stream.PushbulletWebsocketListener;
import com.github.sheigutn.pushbullet.stream.message.NopStreamMessage;
import com.github.sheigutn.pushbullet.stream.message.PushStreamMessage;
import com.github.sheigutn.pushbullet.stream.message.StreamMessage;
import com.github.sheigutn.pushbullet.stream.message.TickleStreamMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meta.works.zim.annotationhelper.util.LossySet;
import org.apache.commons.codec.binary.StringUtils;
import org.buildobjects.process.ProcBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2018-12-08 11:46.
 */
public
class PushbulletListener implements PushbulletWebsocketListener, Runnable
{
	private static final
	Logger log = LoggerFactory.getLogger(PushbulletListener.class);

	private static final
	String ME = System.getProperty("google.account.name", "Robert H");

	private final
	Pushbullet pushbullet;

	private
	PushbulletWebsocketClient websocketClient;

	private final
	//ZimPageAppender zimPageAppender = new ZimPageAppenderDelayFilter(new ZimPageAppenderImpl());
	ZimPageAppender zimPageAppender = new ZimPageAppenderImpl();

	private final
	TasksNotificationsModificator tasks;

	private final
	Set<String> summariesToIgnore = Set.of(
			"F-Droid: Update ready to install"
	);

	private final
	PhoneNumberLinker phoneNumberLinker = new PhoneNumberLinker();

	public
	PushbulletListener()
	{
		this.pushbullet=new Pushbullet(getApiKey());
		this.tasks = new TasksNotificationsModificator(zimPageAppender);
	}

	@Override
	protected
	void finalize() throws Throwable
	{
		super.finalize();
		log.debug("finalize() - I'm going away...");
	}

	private
	enum Feature
	{
		note,
		todo
	}

	private final
	Map<Feature,Device> devicesByFeature=new HashMap<>();

	private final
	Map<String,Feature> featuresByDeviceId=new HashMap<>();

	private
	void replaceWebSocket()
	{
		if (this.websocketClient != null)
		{
			this.websocketClient.disconnect();
			this.websocketClient = null;
		}
		replaceWebSocketIfMissingOrDisconnected();
	}

	private
	void replaceWebSocketIfMissingOrDisconnected()
	{
		if (websocketClient != null)
		{
			if (websocketClient.isConnected())
			{
				return;
			}

			websocketClient = null;
		}

		log.debug("replacing websocket");

		try
		{
			PushbulletWebsocketClient newWebsocketClient=pushbullet.createWebsocketClient();
			newWebsocketClient.registerListener(this);
			newWebsocketClient.connect();
			this.websocketClient=newWebsocketClient;
		}
		catch (Exception e)
		{
			log.error("unable to replace web socket", e);
		}
	}

	private final
	Timer timer=new Timer();

	private final
	long webSocketReplacementPeriod = TimeUnit.HOURS.toMillis(1);

	private final
	long webSocketCheckupPeriod = TimeUnit.MINUTES.toMillis(5);

	public
	void activate()
	{
		if (this.websocketClient==null)
		{
			replaceWebSocket();

			final long initialDelay = webSocketReplacementPeriod;

			timer.schedule(new TimerTask()
			{
				@Override
				public
				void run()
				{
					if (true) return; // DISABLED
					log.debug("proactively replacing web socket");
					replaceWebSocket();
				}
			},
			initialDelay,
			webSocketReplacementPeriod
			);

			timer.schedule(new TimerTask()
						   {
							   @Override
							   public
							   void run()
							   {
								   replaceWebSocketIfMissingOrDisconnected();
							   }
						   },
					webSocketCheckupPeriod,
					webSocketCheckupPeriod
			);

		}
		else
		{
			throw new IllegalStateException("already activated");
		}

		//FAIL... this uses a LOT of the rate-limit b/c (1) the library does not initialize the modified-after time
		//Hopefully, this aweful kludge will make the following log always read "consuming 0 existing pushes"
		kludgeSetPrivateModifiedAfterField();

		//SIDE-EFFECT: We only want pushes that occur during out runtime, so prime the 'new'-ness.
		int wasted=pushbullet.getNewPushes().size();
		log.info("consuming {} existing pushes", wasted);

		for (Device device : pushbullet.getDevices())
		{
			try
			{
				final
				Feature feature = Feature.valueOf(device.getNickname());
				{
					log.debug("found {} device: {}", feature, device);
				}

				this.devicesByFeature.put(feature, device);
				this.featuresByDeviceId.put(device.getIdentity(), feature);
			}
			catch (IllegalArgumentException e)
			{
				log.trace("expected", e);
				log.debug("ignoring other device: {}", device);
			}
		}

		for (Feature feature : Feature.values())
		{
			if (!devicesByFeature.containsKey(feature))
			{
				final
				Device device=pushbullet.createDevice(feature.toString(), "stream");
				{
					log.info("created {} device: {}", feature, device);
				}

				devicesByFeature.put(feature, device);
				featuresByDeviceId.put(device.getIdentity(), feature);
			}
		}

		//To keep us around...
		new Thread(this).start();
	}

	@Override
	public
	void run()
	{
		try
		{
			while (true)
			{
				Thread.sleep(5000);
			}
		}
		catch (InterruptedException e)
		{
			log.info("interrupted", e);
		}
	}

	/**
	 * https://stackoverflow.com/questions/32716952/set-private-field-value-with-reflection
	 */
	private
	void kludgeSetPrivateModifiedAfterField()
	{
		Class<?> clazz = Pushbullet.class;
		Object cc = this.pushbullet;

		try
		{
			Field f1 = cc.getClass().getDeclaredField("newestModifiedAfter");
			f1.setAccessible(true);
			f1.set(cc, (System.currentTimeMillis() / 1000.0));
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
		catch (NoSuchFieldException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public
	void handle(
		Pushbullet pushbullet, StreamMessage streamMessage
	)
	{
		if (streamMessage instanceof NopStreamMessage)
		{
			log.trace("no-op");
			return;
		}

		muteIconField(streamMessage);

		try
		{
			log.info("handle: {} / {}", streamMessage.getClass(), streamMessage);

			switch (streamMessage.getType())
			{
				case NOP:
				{
					log.trace("no-op");
					break;
				}

				case PUSH:
				{
					handlePushStreamMessage((PushStreamMessage)streamMessage);
					break;
				}

				case TICKLE:
				{
					handleTickle((TickleStreamMessage) streamMessage);
					break;
				}

				default:
				{
					log.info("unhandled: {}", streamMessage);
				}
			}
		}
		catch (Exception e)
		{
			log.error("caught", e);
			bestEffortTryToNotice(e);
		}
	}

	// Mostly to decrease log noise.
	private
	void muteIconField(final StreamMessage streamMessage)
	{
		if (streamMessage instanceof PushStreamMessage a)
		{
			if (a.getPush() instanceof NotificationEphemeral b)
			{
				// TODO: Maybe we replace it with a super-compact hash? (CAS)
				b.setIcon("[...]");
			}
		}
	}

	private
	void bestEffortTryToNotice(final Exception e)
	{
		try
		{
			var hasMessage = e.getMessage() != null;
			var summary = hasMessage ? String.format("zah: %s", e.getClass().getSimpleName()) : "zah caught";
			var body = hasMessage ? e.getMessage() : e.getClass().getSimpleName();
			ProcBuilder.run("notify-send", "--expire-time", "3600000", summary, body);
		}
		catch (Exception e2)
		{
			log.error("additionally... could not alert user", e2);
		}
	}

	private
	void handlePushStreamMessage(PushStreamMessage pushStreamMessage) throws IOException, InterruptedException
	{
		final
		Ephemeral ephemeral = pushStreamMessage.getPush();

		if (ephemeral instanceof SmsChangedEphemeral)
		{
			final
			SmsChangedEphemeral smsChangedEphemeral=(SmsChangedEphemeral)ephemeral;

			final
			String sourceDeviceId=smsChangedEphemeral.getSourceDeviceIdentity();
			{
				log.debug("device that received the sms: {}", sourceDeviceId);
			}

			for (SmsNotification notification : smsChangedEphemeral.getNotifications())
			{
				handleSmsNotification(sourceDeviceId, notification);
			}
		}

		if (ephemeral instanceof NotificationEphemeral)
		{
			final
			NotificationEphemeral notification = (NotificationEphemeral)ephemeral;
			var id = notification.getNotificationId();
			var appPackage = notification.getPackageName();
			var app = notification.getApplicationName();
			var title = notification.getTitle();
			var body = notification.getBody();
			LogAndRememberNotification(id, appPackage, app, title, body);
		}

		if (ephemeral instanceof DismissalEphemeral)
		{
			final
			DismissalEphemeral dismissal = (DismissalEphemeral)ephemeral;
			var id = dismissal.getNotificationId();
			var appPackage = dismissal.getPackageName();
			PopAndLogDismissedNotification(id, appPackage);
		}
	}

	private final
	Map<String,String> notificationsById = new HashMap<>();

	private final
	Set<String> slackBodyBitsSeen = new LossySet<>(100);

	private
	void LogAndRememberNotification(
			final String id,
			final String appPackage,
			String app,
			String title,
			String body
	) throws IOException, InterruptedException
	{
		if (DontCareAbout(appPackage, app, title, body))
		{
			return;
		}

		body = applyTrimAndTruncation(body);

		if (app.equals("Phone"))
		{
			title = phoneNumberLinker.linkifyPhoneNumber(title);
		}

		app = filterAppName(app);

		String summary = title.startsWith(app) ? title : app + ": " + title;

		String summaryWithBody;
		{
			if (body == null || body.trim().isEmpty())
			{
				summaryWithBody = summary;
			}
			else
			if (body.length() > 800)
			{
				summaryWithBody = summary + ": (" + body.length() + " characters)";
			}
			else if (appPackage.equals("com.Slack"))
			{
				var bits = body.split("\n");
				var sb = new StringBuilder();
				// Remove leading body elements that we have seen before.
				for (String bit : bits)
				{
					// If we have accepted one bit, then accept all that follow.
					if (sb.length() != 0)
					{
						sb.append('\n');
						sb.append(bit);
						slackBodyBitsSeen.add(bit);
						continue;
					}

					if (slackBodyBitsSeen.add(bit))
					{
						sb.append(bit);
					}
					else
					{
						log.debug("Dropping seen slack body bit: {}", bit);
					}
				}
				body = sb.toString();
				log.debug("New slack body: {}", body);
				summaryWithBody = summary + ": " + body;
			}
			else
			{
				summaryWithBody = summary + ": " + body;
			}
		}

		//summaryWithBody = applyTrimAndTruncation(summaryWithBody);

		if (notificationsById.size() > 10_000)
		{
			notificationsById.clear();
		}

		if (appPackage.equals("org.tasks"))
		{
			notificationsById.put(id, title);
			tasks.OnNotificationDisplayed(id, title);
			return;
		}

		notificationsById.put(id, summary);

		if (summariesToIgnore.contains(summary))
		{
			log.debug("ignore: {}", summary);
			return;
		}

		if (shouldStandOut(summaryWithBody))
		{
			summaryWithBody += "\n";
		}

		zimPageAppender.journalNote(summaryWithBody);
	}

	private
	boolean shouldStandOut(final String summaryWithBody)
	{
		return summaryWithBody.startsWith("Weather: ");
	}

	private
	String filterAppName(final String app)
	{
		return appNameTranslations.getOrDefault(app, app);
	}

	private static final Map<String,String> appNameTranslations = Map.of(
			"Infinity", "Reddit"
	);

	private
	String applyTrimAndTruncation(String summaryWithBody)
	{
		if (summaryWithBody == null)
		{
			return null;
		}

		for (String prefix : prefixesToTrim)
		{
			if (summaryWithBody.startsWith(prefix))
			{
				summaryWithBody = summaryWithBody.substring(prefix.length());
			}
		}

		for (String truncationPoint : truncationPoints)
		{
			int i = summaryWithBody.indexOf(truncationPoint);

			if (i > 0)
			{
				summaryWithBody = summaryWithBody.substring(0, i);
			}
		}

		return summaryWithBody;
	}

	private final
	List<String> prefixesToTrim = List.of(
			"A driver needs help delivering your Amazon order. Reply STOP to stop receiving texts for Amazon deliveries.",
			"Slack: Thread in ",
			"Slack: "
	);

	private final
	List<String> truncationPoints = List.of(
			"About craigslist mail:",
			"view posting edit",
			"view all edit unsubscribe",
			"about search alerts",
			"View all the results.",
			"Check activity",
			"Manage family group",
			"Manage your Location History",
			"To check hours before heading to",
			"Rx info, price ",
			"See more highlights",
			"to unsubscribe",
			"To check hours before heading",
			"Or reply:",
			"Msg&Data",
			"how likely are you to recommend",
			"this mandatory email",
			" This text session will close ",
			"TxtHelp",
			"Txt STOP",
			"Text help",
			"Text STOP",
			"Text 0 for",
			" STOP to "
	);

	private
	boolean DontCareAbout(final String appPackage, final String app, final String title, final String body)
	{
		var care = false;
		var dont_care = true;

		if (appPackage.startsWith("org.fdroid") || app.equals("F-Droid"))
		{
			return dont_care;
		}

		if (appPackage.equals("com.google.android.gms") || app.equals("Google Play services"))
		{
			return dont_care;
		}

		if (app.equals("Eternity") && body.endsWith("New Messages"))
		{
			return dont_care;
		}

		if ((title.contains("pload") || title.contains("ownload")) && title.contains("%"))
		{
			return dont_care;
		}

		if (title.contains("LinkedIn Job Alerts"))
		{
			System.err.println("lija in title");
			return dont_care;
		}

		if (app.equals("K-9 Mail"))
		{
			return dont_care;
		}

		// --------------------------------------------------------

		if (body == null)
		{
			return care;
		}

		// ------------------- BODY BELOW THIS --------------------

		if (body.contains("LinkedIn Job Alerts"))
		{
			System.err.println("lija in body");
			return dont_care;
		}

		return care;
	}

	private
	void PopAndLogDismissedNotification(final String id, final String appPackage) throws
																				  IOException,
																				  InterruptedException
	{
		var summary = notificationsById.remove(id);

		if (summary == null)
		{
			if (appPackage.equals("sms") && id.equals("0"))
			{
				summary = "sms";
			}
			else
			{
				summary = "Unknown " + appPackage + " notification #" + id;
			}
		}

		// TODO: Save all the parameters, to better feed this.
		if (DontCareAbout(appPackage, "?", summary, "?")) {
			return;
		}

		// There are so many slack notifications, it's not worth logging their dismissal.
		if (appPackage.equals("com.Slack") || appPackage.startsWith("org.fdroid"))
		{
			return;
		}

		if (appPackage.equals("org.tasks"))
		{
			tasks.OnNotificationDismissed(id, summary);
			return;
		}

		if (summariesToIgnore.contains(summary))
		{
			log.debug("ignore dismissed: {}", summary);
		}
		else
		{
			zimPageAppender.journalNoteStruckOut("dismissed: " + summary);
		}
	}

	private
	void handleSmsNotification(String deviceId, SmsNotification sms) throws IOException, InterruptedException
	{
		if (looksLikeSpam(sms))
		{
			log.debug("ignoring spammy: {}", sms);
			return;
		}

		final
		String threadId = sms.getThreadId();

		final
		String name = sms.getTitle();

		final
		String body;

		// TODO: use this (the actual time received) instead of the local time for sms journal entries.
		final
		Date date = new Date(sms.getTimestamp()*1000);

		final
		String journalPage= ":Journal:"+colonSeparatedYearMonthDate.format(date);

		final
		String threadPage;
		{
			var trimmedSmsBody = applyTrimAndTruncation(sms.getBody());

			if (looksLikeGroupThread(name))
			{
				threadPage=String.format(":TXT:Thread:%s", threadId);

				final
				String author=guessAuthor(deviceId, threadId, sms.getTimestamp());

				if (author==null)
				{
					body=trimmedSmsBody;
				}
				else
				{
					body=String.format("**%s** - %s", author, trimmedSmsBody);
				}
			}
			else
			{
				threadPage=sanitizeForZimPageName(name);
				body=trimmedSmsBody;
			}
		}

		final
		String dateTimeMessage=String.format("\n%s - %s", linkedCompactTimeCode(journalPage, date), body);

		final
		String timeThreadMessage=String.format("\n%s - [[%s]] - %s", localTimeOnly(date), threadPage, body);

		zimPageAppender.pageNote(threadPage, dateTimeMessage);
		zimPageAppender.pageNote(journalPage, timeThreadMessage);
	}

	private
	boolean looksLikeSpam(SmsNotification sms)
	{
		final
		String body=sms.getBody();

		if (body!=null)
		{
			if (body.startsWith("Configuration Notification."))
			{
				return true;
			}
		}

		return false;
	}

	private
	String guessAuthor(String deviceId, String threadId, Long timestamp)
	{
		try
		{
			final
			PushbulletDeviceThreadRequest request = new PushbulletDeviceThreadRequest(
				deviceId,
				threadId
			);

			final
			JsonObject threadData = pushbullet.executeRequest(request);

			final
			JsonObject message = locateMatchingTimestamp(threadData, timestamp);

			final
			int who = message.get("recipient_index").getAsInt();

			return deviceThreadRecipient(deviceId, threadId, who);
		}
		catch (Exception e)
		{
			log.error("unable to guess author", e);
			return null;
		}
	}

	private final
	Map<String, List<JsonObject>> threadRecipientsByDeviceThreadId=new HashMap<>();

	private
	String deviceThreadRecipient(String deviceId, String threadId, int index)
	{
		final
		String deviceThreadId=String.format("%s:%s", deviceId, threadId);

		try
		{
			List<JsonObject> recipients=threadRecipientsByDeviceThreadId.get(deviceThreadId);

			if (recipients==null)
			{
				recipients=fetchDeviceThreadRecipients(deviceId, threadId);
				threadRecipientsByDeviceThreadId.put(deviceThreadId, recipients);
			}

			return recipientToString(recipients.get(index));
		}
		catch (Exception e)
		{
			log.error("unable to map device/thread/index to user", e);

			// NB: by clearing out the cache, we will (in the best case) pick up new receipients, and
			// (in the worst case) sent a thread request for each incoming message.
			threadRecipientsByDeviceThreadId.remove(deviceThreadId);

			return String.format("r%d", index);
		}
	}

	private
	List<JsonObject> fetchDeviceThreadRecipients(String deviceId, String threadId)
	{
		for (JsonElement _thread : pushbullet
			.executeRequest(new PushbulletDeviceThreadRequest(deviceId))
			.get("threads")
			.getAsJsonArray())
		{
			final
			JsonObject thread=_thread.getAsJsonObject();

			if (thread.get("id").getAsString().equals(threadId))
			{
				return recipientList(thread);
			}
		}

		throw new IllegalStateException("could not locate thread #"+threadId);
	}

	private
	List<JsonObject> recipientList(JsonObject thread)
	{
		final
		List<JsonObject> retval=new ArrayList<>(100);

		for (JsonElement recipient : thread.get("recipients").getAsJsonArray())
		{
			retval.add(recipient.getAsJsonObject());
		}

		return retval;
	}

	private
	String recipientToString(JsonObject jsonObject)
	{
		// fields: name, address, number
		final
		String base=jsonObject.get("name").getAsString();

		if (base.indexOf(',')>=0)
		{
			// e.g. "Jones, Bob" -> "Bob"
			return base.substring(base.lastIndexOf(',')+1).trim();
		}

		if (base.indexOf(' ')>=0)
		{
			// e.g. "Bob Jones" -> "Bob"
			return base.substring(0, base.indexOf(' '));
		}

		return base;
	}

	private
	JsonObject locateMatchingTimestamp(JsonObject threadData, long timestamp)
	{
		for (JsonElement _message : threadData.getAsJsonArray("thread"))
		{
			final
			JsonObject message=_message.getAsJsonObject();

			final
			long thisTimestamp = message.get("timestamp").getAsLong();

			if (timestamp == thisTimestamp)
			{
				return message;
			}
		}

		throw new RuntimeException("could not locate message by matching timestamp");
	}

	private
	String linkedCompactTimeCode(String journalPage, Date date)
	{
		final
		long seconds=date.getTime()/1000;

		final
		String timeCode=String.format("%08X", seconds);

		final
		String hexDay=timeCode.substring(0, 4);

		final
		String hexTime=timeCode.substring(4);

		return String.format("[[%s|%s-%s]]", journalPage, hexDay, hexTime);
	}

	private
	String localTimeOnly(Date date)
	{
		final
		String retval=localTime.format(date).toLowerCase();

		/*"1:23am".length()==6
		if (retval.length()==6)
		{
			return "0"+retval;
		}
		else
		{
			return retval;
		}
		*/
		return retval;
	}

	private final
	DateFormat localTime=new SimpleDateFormat("hh:mmaa");

	public static
	String sanitizeForZimPageName(String originator)
	{
		//e.g. for 'originator': "Bob Jones", "12345", "+12345"
		originator=originator.replaceAll("[^a-zA-Z0-9]", "");

		if (originator.startsWith("1"))
		{
			originator=originator.substring(1);
		}

		final
		char firstChar=originator.charAt(0);

		if (Character.isDigit(firstChar))
		{
			if (originator.length()==10)
			{
				final String areaCode = originator.substring(0, 3);

				final String remainder = originator.substring(3);

				return String.format(":Phone:Number:%s:%s", areaCode, remainder);
			}
			else
			{
				return String.format(":Phone:Number:%s", originator);
			}
		}
		else
		{
			return String.format(":%s:TextMessage", originator);
		}
	}

	private
	boolean looksLikeGroupThread(String userInput)
	{
		return userInput.indexOf(',')>=0;
	}

	private final
	DateFormat colonSeparatedYearMonthDate =new SimpleDateFormat("yyyy:MM:dd");

	private
	void handleTickle(TickleStreamMessage tickle) throws IOException, InterruptedException
	{
		final
		String subType = tickle.getSubType();

		if (subType.equals("push"))
		{
			for (Push push : pushbullet.getNewPushes())
			{
				handlePush(push);
			}

			return;
		}

		log.info("received a {} tickle", subType);
	}

	private
	void handlePush(Push push) throws IOException, InterruptedException
	{
		log.info("handlePush: {}", push);

		switch (push.getType())
		{
			case NOTE:
				handleNote((NotePush)push);
				break;
			case LINK:
				break;
			case FILE:
				handleFilePush((FilePush)push);
				break;
			case LIST:
				break;
			case ADDRESS:
				break;
			/*
			case SMS_CHANGED:
				log.info("sms");
				break;
			*/
		}
	}

	private
	void handleFilePush(final FilePush push) throws IOException, InterruptedException
	{
		var fileType = push.getFileType();
		var baseName = push.getFileName();

		if (fileType.startsWith("image/"))
		{
			handleImageFilePush(push);
		}
		else
		if (push.isDismissed())
		{
			zimPageAppender.journalNoteStruckOut("file push: " + baseName);
		}
		else
		{
			zimPageAppender.journalNote("file push: " + baseName);
		}
	}

	private
	void handleImageFilePush(final FilePush push) throws IOException, InterruptedException
	{
		var baseName = push.getFileName();
		var fetchUrl = push.getFileUrl();
		var imageWidth = push.getImageWidth();
		var imageHeight = push.getImageHeight();
		log.debug("handleImageFilePush: {}x{}: {}", imageWidth, imageHeight, fetchUrl);

		var who = whoSent(push);

		if (push.isDismissed())
		{
			zimPageAppender.journalNoteStruckOut(who + " pushed image: " + baseName);
			return;
		}


		// TODO: If the image is small, d/l it and stick it straight into the journal
		// TODO: If the image is large, d/l it, scale it down, and put the small version in the journal.
		// TODO: universal CAS refs?

		zimPageAppender.journalNote(who + " pushed image: " + baseName);
	}

	private
	String whoSent(final Push push)
	{
		var name = push.getSenderName();
		var space = name.indexOf(' ');

		if (space > 0)
		{
			return name.substring(0, space).trim();
		}
		else
		{
			return name.trim();
		}
	}

	private final
	DateFormat zimDateAndTimeLinkFormatter=new SimpleDateFormat("'[[:Journal:'yyyy:MM:dd|yyyy-MM-dd @ HH:mm:ss]]");

	private
	void handleNote(NotePush note) throws IOException, InterruptedException
	{
		final
		Feature feature = featuresByDeviceId.get(note.getTargetDeviceIdentity());

		if (feature!=null)
		{
			final
			String fullMessage;
			{
				if (isTrivial(note.getTitle()))
				{
					//no title
					if (isTrivial(note.getBody()))
					{
						//and no body...?
						fullMessage=note.toString();
					}
					else
					{
						//just the body
						fullMessage=note.getBody();
					}
				}
				else
				{
					switch (note.getTitle())
					{
						case "call-in":
						{
							try
							{
								final
								CallInRecord callInRecord = new CallInRecord(note.getBody());

								final
								String dateAndTimeLink = zimDateAndTimeLinkFormatter.format(callInRecord.getDate());

								zimPageAppender.pageNote(callInRecord.getZimPageName(), dateAndTimeLink+" -> Call In");
								zimPageAppender.journalNote(
									"**Incoming call** from " +
									callInRecord.getZimPageLink("CallIn")
								);
								return;
							}
							catch (Exception e)
							{
								log.error("call-in handling failure", e);
								//fall thru, log it like normal.
							}
						}
					}

					//have title
					if (isTrivial(note.getBody()))
					{
						//but no body
						fullMessage=note.getTitle();
					}
					else
					{
						//title and body
						fullMessage=String.format("%s: %s", note.getTitle(), note.getBody());
					}
				}
			}

			switch (feature)
			{
				case note:
				{
					zimPageAppender.journalNote(fullMessage.trim());
					return;
				}

				case todo:
				{
					zimPageAppender.newActionItem(fullMessage.trim());
					return;
				}
			}
		}

		var sender = note.getSenderName();

		if (sender != null && !sender.isEmpty())
		{
			var summary = sender+": "+summarize(note);

			if (sender.equals(ME))
			{
				// NB: outgoing replies (to normal conversations) are "immediately dismissed", and we don't want to
				// truncate them. This is also the path for CLI-generated messages (not just replies).
				// We do need this dismissed check, though, as otherwise it will trigger when we explicitly dismiss
				// the trash can notification too.
				// What we really need to know is if this is the first time we are seeing this message, in which case
				// we can ignore the dismissed flag.
				if (note.isDismissed())
				{
					if (summary.contains("Trash can act"))
					{
						summary = "dismissed: self: " + summarize(note);
						zimPageAppender.journalNoteStruckOut(summary);
					}
					else
					{
						summary = "self: " + summarize(note);
						zimPageAppender.journalNote(summary);
					}
				}
				else
				{
					summary = "self: " + summarize(note);
					zimPageAppender.journalNote(summary);
				}
			}
			else if (note.isDismissed())
			{
				// BUG? If the WUI is open, then responses might be instantly marked as read/dismissed.
				if (summary.length() > 47)
				{
					summary = summary.substring(0, 47) + "...";
				}
				summary = "dismissed: " + summary;
				zimPageAppender.journalNoteStruckOut(summary);
			}
			else
			{
				zimPageAppender.journalNote(summary);
			}
			return;
		}

		//NB: This logs even when the user dismisses an item.
		log.debug("ignoring note: '{}' / {}", note.getTitle(), note.getBody());
	}

	private
	String summarize(final NotePush note)
	{
		if (isTrivial(note.getBody()))
		{
			if (isTrivial(note.getTitle()))
			{
				return note.toString();
			}
			else
			{
				return note.getTitle();
			}
		}
		else
		if (isTrivial(note.getTitle()))
		{
			return note.getBody();
		}
		else
		{
			return String.format("%s: %s", note.getTitle(), note.getBody());
		}
	}

	private
	boolean isTrivial(String s)
	{
		return s==null || s.isEmpty() || s.trim().isEmpty();
	}

	private
	Device getFeatureDevice(String targetDeviceIdentity)
	{
		for (Device device : devicesByFeature.values())
		{
			if (targetDeviceIdentity.equals(device.getIdentity()))
			{
				return device;
			}
		}

		return null;
	}

	private static
	File getConfigFile()
	{
		// NB: Same configuration as: https://github.com/Red5d/pushbullet-bash
		return new File(System.getProperty("user.home"), ".config/pushbullet");
	}

	private static
	String getApiKey()
	{
		final
		String override=System.getenv("PB_API_KEY");
		{
			if (override!=null)
			{
				return override;
			}
		}

		final
		File file=getConfigFile();

		if (file.canRead())
		{
			try
			{
				final
				Properties properties=new Properties();
				{
					try (FileReader fileReader=new FileReader(file))
					{
						properties.load(fileReader);
					}
				}

				final
				String apiKey=properties.getProperty("PB_API_KEY");

				if (apiKey.startsWith("\""))
				{
					// Strip the enclosing quotation marks.
					return apiKey.substring(1, apiKey.length()-1);
				}
				else
				{
					return apiKey;
				}
			}
			catch (IOException e)
			{
				log.error("unable to read api key from {}", file, e);
				return null;
			}
		}
		else
		{
			log.warn("unreadable: {}", file);
			return null;
		}
	}

	public static
	void main(String[] args) throws IOException
	{
		//log.info("pushbullet api key: {}", getApiKey());

		final
		PushbulletListener self=new PushbulletListener();
		{
			self.activate();
		}

		final
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));

		int n=0;

		System.err.println("Messages typed here will appear in your pushbullet stream");

		while (true)
		{
			n++;
			self.pushbullet.push(new SendableNotePush("testing"+n, br.readLine()));
		}

		//System.in.read();
	}
}
