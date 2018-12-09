package meta.works.zim.annotationhelper;

import com.github.sheigutn.pushbullet.Pushbullet;
import com.github.sheigutn.pushbullet.items.device.Device;
import com.github.sheigutn.pushbullet.items.push.sendable.defaults.SendableNotePush;
import com.github.sheigutn.pushbullet.items.push.sent.Push;
import com.github.sheigutn.pushbullet.items.push.sent.defaults.NotePush;
import com.github.sheigutn.pushbullet.stream.PushbulletWebsocketClient;
import com.github.sheigutn.pushbullet.stream.PushbulletWebsocketListener;
import com.github.sheigutn.pushbullet.stream.message.NopStreamMessage;
import com.github.sheigutn.pushbullet.stream.message.StreamMessage;
import com.github.sheigutn.pushbullet.stream.message.TickleStreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2018-12-08 11:46.
 */
public
class PushbulletListener implements PushbulletWebsocketListener
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
		}

		this.websocketClient=pushbullet.createWebsocketClient();
		this.websocketClient.registerListener(this);
		this.websocketClient.connect();
	}

	private final
	Timer timer=new Timer();

	public
	void activate()
	{
		if (this.websocketClient==null)
		{
			replaceWebSocket();
			timer.schedule(new TimerTask()
			{
				@Override
				public
				void run()
				{
					log.debug("replacing web socket");
					replaceWebSocket();
				}
			}, TimeUnit.HOURS.toMillis(1));
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
		try
		{
			switch (streamMessage.getType())
			{
				case NOP:
				{
					log.trace("no-op");
					break;
				}

				case PUSH:
				{
					log.info("got push stream message?");
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
		}
	}

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
