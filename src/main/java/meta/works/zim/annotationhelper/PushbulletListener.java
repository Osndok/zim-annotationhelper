package meta.works.zim.annotationhelper;

import com.github.sheigutn.pushbullet.Pushbullet;
import com.github.sheigutn.pushbullet.ephemeral.*;
import com.github.sheigutn.pushbullet.items.device.Device;
import com.github.sheigutn.pushbullet.items.push.sendable.defaults.SendableNotePush;
import com.github.sheigutn.pushbullet.items.push.sent.Push;
import com.github.sheigutn.pushbullet.items.push.sent.defaults.NotePush;
import com.github.sheigutn.pushbullet.stream.PushbulletWebsocketClient;
import com.github.sheigutn.pushbullet.stream.PushbulletWebsocketListener;
import com.github.sheigutn.pushbullet.stream.message.NopStreamMessage;
import com.github.sheigutn.pushbullet.stream.message.PushStreamMessage;
import com.github.sheigutn.pushbullet.stream.message.StreamMessage;
import com.github.sheigutn.pushbullet.stream.message.TickleStreamMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2018-12-08 11:46.
 */
public
class PushbulletListener implements PushbulletWebsocketListener, Runnable
{
	private static final
	Logger log = LoggerFactory.getLogger(PushbulletListener.class);

	private final
	Pushbullet pushbullet;

	private
	PushbulletWebsocketClient websocketClient;

	private final
	ZimPageAppender zimPageAppender=new ZimPageAppender();

	public
	PushbulletListener()
	{
		this.pushbullet=new Pushbullet(getApiKey());
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
		if (this.websocketClient!=null)
		{
			this.websocketClient.disconnect();
			this.websocketClient=null;
		}

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
					log.debug("replacing web socket");
					replaceWebSocket();
				}
			},
			initialDelay,
			webSocketReplacementPeriod
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

		try
		{
			log.info("handle: {} / {}", streamMessage.getClass(), streamMessage.toString());

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
			LogAndRememberNotification(id, appPackage, app, title);
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

	private
	void LogAndRememberNotification(
			final String id,
			final String appPackage,
			final String app,
			final String title) throws	IOException, InterruptedException
	{
		var summary = app+": "+title;
		if (notificationsById.size() > 10_000)
		{
			notificationsById.clear();
		}
		notificationsById.put(id, summary);
		zimPageAppender.journalNote(summary);
	}

	private
	void PopAndLogDismissedNotification(final String id, final String appPackage) throws
																				  IOException,
																				  InterruptedException
	{
		var summary = notificationsById.remove(id);
		if (summary == null)
		{
			summary = "Unknown "+appPackage+" notification";
		}

		// There are so many slack notifications, it's not worth logging their dismissal.
		if (appPackage.equals("com.Slack"))
		{
			return;
		}

		zimPageAppender.journalNote("dismissed: "+summary);
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
			if (looksLikeGroupThread(name))
			{
				threadPage=String.format(":TXT:Thread:%s", threadId);

				final
				String author=guessAuthor(deviceId, threadId, sms.getTimestamp());

				if (author==null)
				{
					body=sms.getBody();
				}
				else
				{
					body=String.format("**%s** - %s", author, sms.getBody());
				}
			}
			else
			{
				threadPage=sanitizeForZimPageName(name);
				body=sms.getBody();
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

		//NB: This logs even when the user dismisses an item.
		log.debug("ignoring note: '{}' / {}", note.getTitle(), note.getBody());
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
