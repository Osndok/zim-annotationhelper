package meta.works.zim.annotationhelper;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.types.DBusMapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.awt.PageAttributes.MediaType.D;

/**
 * Created by robert on 2016-10-06 11:30.
 */
public abstract
class AbstractDBusMediaPlayer extends Thread implements DBusSigHandler
{
	private long lastCallback;

	abstract String getDBusSenderSuffix();

	abstract void onStateChange(StateSnapshot was, StateSnapshot now, long age);

	private static final
	Logger log = LoggerFactory.getLogger(AbstractDBusMediaPlayer.class);

	public final
	void run()
	{
		while(true)
		{
			try
			{
				run2();
			}
			catch (DBus.Error.ServiceUnknown e)
			{
				log.debug("{} is not running", getDBusSenderSuffix());
				responsive = false;
				connection = null;
			}
			catch (Throwable t)
			{
				responsive=false;
				connection=null;
				log.error("caught", t);
			}

			synchronized (this)
			{
				try
				{
					this.wait(5000);
				}
				catch (InterruptedException e)
				{
					log.error("interrupted");
					return;
				}
			}
		}
	}

	private
	String getDBusSender()
	{
		return "org.mpris.MediaPlayer2."+getDBusSenderSuffix();
	}

	DBusConnection connection;

	private
	void run2() throws Exception
	{
		if (connection==null)
		{
			connection = DBusConnection.getConnection(DBusConnection.SESSION);

			//connection.addSigHandler(DBus.Properties.class, getDBusSender(), (DBusSigHandler<?>) this);
		}

		final
		String objectPath="/org/mpris/MediaPlayer2";

		final
		String interfaceName="org.mpris.MediaPlayer2.Player";

		final
		DBus.Properties properties = connection.getRemoteObject(
			getDBusSender(),
			objectPath,
			DBus.Properties.class
		);

		propertiesByName = properties.GetAll(interfaceName);
		{
			responsive=true;

			if (log.isTraceEnabled())
			{
				for (Map.Entry<String, Variant> me : propertiesByName.entrySet())
				{
					final
					Object value = me.getValue().getValue();

					log.trace("'{}' = ({}) '{}'", me.getKey(), value.getClass(), value);
				}
			}
		}

		final
		String playStateString = get(String.class, "PlaybackStatus");

		final
		PlayState playState = PlayState.valueOf(playStateString);

		final
		Long position = get(Long.class, "Position");

		final
		String zimPage;

		final
		Map<String,Variant> metadata=get(Map.class, "Metadata");

		final
		String url;

		if (metadata.isEmpty())
		{
			log.info("no file open, {}", playState);
			url=null;
			zimPage=null;
		}
		else
		{
			url = (String) metadata.get("xesam:url").getValue();
			{
				log.debug("url: {}", url);
			}

			if (isLocalPodcastUrl(url))
			{
				zimPage = zimPageNameExtractor.getZimPageNameFor(url);
			}
			else
			{
				zimPage = null;
			}
		}

		final
		StateSnapshot newState=new StateSnapshot(playState, position, url, zimPage);

		final
		StateSnapshot previousState;
		{
			previousState = this.stateSnapshot;
			stateSnapshot = newState;
		}

		if (previousState!=null && warrantsFiringCallback(previousState, newState))
		{
			log.debug("onStateChange: {} -> {}", previousState, newState);

			final
			long now=System.currentTimeMillis();

			onStateChange(previousState, newState, now-lastCallback);

			lastCallback=now;
		}
	}

	private
	boolean isLocalPodcastUrl(String url)
	{
		return url.startsWith("file:///mnt/shared/Podcasts/");
	}

	private
	boolean warrantsFiringCallback(StateSnapshot previousState, StateSnapshot newState)
	{
		return !previousState.getPlayState().equals(newState.getPlayState());
	}

	public static final
	ZimPageNameExtractor zimPageNameExtractor=new ZimPageNameExtractor();

	Map<String, Variant> propertiesByName=new HashMap<String, Variant>();

	private <T>
	T get(Class<T> c, String keyName)
	{
		final
		Variant variant = propertiesByName.get(keyName);

		if (variant==null)
		{
			return null;
		}
		else
		{
			return (T) variant.getValue();
		}
	}

	private
	boolean responsive;

	public
	boolean isResponsive()
	{
		return responsive;
	}

	private
	StateSnapshot stateSnapshot;

	public
	StateSnapshot getStateSnapshot()
	{
		return stateSnapshot;
	}

	@Override
	public
	void handle(DBusSignal dBusSignal)
	{
		log.debug("received: {}", dBusSignal);

		synchronized (this)
		{
			this.notifyAll();
		}
	}
}
