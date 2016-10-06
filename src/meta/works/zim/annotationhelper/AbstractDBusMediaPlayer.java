package meta.works.zim.annotationhelper;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.DBusMapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.awt.PageAttributes.MediaType.D;

/**
 * Created by robert on 2016-10-06 11:30.
 */
public abstract
class AbstractDBusMediaPlayer extends Thread implements DBusSigHandler
{
	private long lastCallback;

	abstract String getDBusSenderSuffix();

	abstract void onStateChange(StateSnapshot was, StateSnapshot now, long age) throws IOException, InterruptedException;

	abstract void onPeriodicInterval(StateSnapshot state, String timeCode) throws IOException, InterruptedException;

	private static final
	long PERIODIC_INTERVAL_SECONDS = TimeUnit.MINUTES.toSeconds(2);

	public static final
	long NOTABLY_STALE_STATE_MILLIS = TimeUnit.MINUTES.toSeconds(5);

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

	private static final
	String OBJECT_PATH="/org/mpris/MediaPlayer2";

	private static final
	String INTERFACE_NAME="org.mpris.MediaPlayer2.Player";

	private
	void run2() throws Exception
	{
		if (connection==null)
		{
			connection = DBusConnection.getConnection(DBusConnection.SESSION);

			//connection.addSigHandler(DBus.Properties.class, getDBusSender(), (DBusSigHandler<?>) this);
		}

		final
		DBus.Properties properties = connection.getRemoteObject(
			getDBusSender(),
			OBJECT_PATH,
			DBus.Properties.class
		);

		propertiesByName = properties.GetAll(INTERFACE_NAME);
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
			log.debug("no file open, {}", playState);
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
	ZimPageAppender zimPageAppender = new ZimPageAppender();

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

	public
	void stopPlayback()
	{
		log.info("stop playback");

		if (connection!=null)
		{
			/* ATTEMPT 1
			final
			String source=getDBusSender();

			final
			String path=OBJECT_PATH;

			final
			String iface=INTERFACE_NAME;

			final
			String member;

			final
			String sig;

			final
			DBusSignal signal=new DBusSignal(source, path, iface, member, sig, args);
			connection.sendSignal(signal);
			*/

			/* ATTEMPT 2
			final
			String busName=getDBusSender();

			final
			String objectPath=OBJECT_PATH;

			final
			DBusInterface remoteObject;
			{
				try
				{
					remoteObject = connection.getRemoteObject(busName, objectPath);
				}
				catch (DBusException e)
				{
					log.error("caught", e);
					return;
				}

				log.debug("got: ({}) {}", remoteObject.getClass(), remoteObject);
			}

			final
			String methodName="Stop";

			final
			Object[] arguments=new Object[0];

			connection.callMethodAsync(remoteObject, INTERFACE_NAME+"."+methodName, arguments);
			*/

			//ATTEMPT 3
			try
			{
				Runtime.getRuntime().exec("qdbus "+getDBusSender()+" /org/mpris/MediaPlayer2 org.mpris.MediaPlayer2.Player.Stop");
			}
			catch (IOException e)
			{
				log.error("caught", e);
			}
		}
	}
}
