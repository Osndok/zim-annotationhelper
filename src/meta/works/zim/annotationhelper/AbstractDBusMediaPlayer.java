package meta.works.zim.annotationhelper;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2016-10-06 11:30.
 */
public abstract
class AbstractDBusMediaPlayer extends Thread implements DBusSigHandler
{
    private static final
    long SLEEP_MILLIS = TimeUnit.SECONDS.toMillis(3);

	private long lastStateChangeCallback=System.currentTimeMillis();

	abstract String getDBusSenderSuffix();

	abstract void onStateChange(StateSnapshot was, StateSnapshot now, long age) throws IOException, InterruptedException;

	abstract void onPeriodicInterval(StateSnapshot state) throws IOException, InterruptedException;

	private static final
	long PERIODIC_INTERVAL_SECONDS = TimeUnit.MINUTES.toSeconds(2);

	public static final
	long NOTABLY_STALE_STATE_MILLIS = TimeUnit.MINUTES.toMillis(5);

	private static final
	Logger log = LoggerFactory.getLogger(AbstractDBusMediaPlayer.class);

	private final
	ShowNotesURLSource showNotesURLSource=new ShowNotesURLSource();

	public
	AbstractDBusMediaPlayer(String name)
	{
		super(name);
	}

	public final
	void run()
	{
		while(true)
		{
			try
			{
				run2();
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
					this.wait(SLEEP_MILLIS);
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
		final
		StateSnapshot newState=getNewState();

		final
		StateSnapshot previousState;
		{
			previousState = this.stateSnapshot;
			stateSnapshot = newState;
		}

		if (previousState!=null)
		{
			if (warrantsFiringCallback(previousState, newState))
			{
				log.debug("onStateChange: {} -> {}", previousState, newState);

				final
				long now = System.currentTimeMillis();

				onStateChange(previousState, newState, now - lastStateChangeCallback);

				lastStateChangeCallback = now;

				if (firstTimeCode(newState))
				{
					zimPageAppender.pageNote(newState.getZimPage(), "\n");
					onPeriodicInterval(newState);
				}
			}
			else
			if (!previousState.getRoughTimeCode().equals(newState.getRoughTimeCode()))
			{
				onPeriodicInterval(newState);
			}
		}
	}

	private
	boolean firstTimeCode(StateSnapshot state)
	{
		final
		String roughTimeCode = state.getRoughTimeCode();

		return roughTimeCode!=null && roughTimeCode.equals("00:00");
	}

	private
	StateSnapshot getNewState()
	{
		final
		DBus.Properties properties;
		{
			try
			{
				if (connection == null)
				{
					connection = DBusConnection.getConnection(DBusConnection.SESSION);

					//connection.addSigHandler(DBus.Properties.class, getDBusSender(), (DBusSigHandler<?>) this);
				}

				properties = connection.getRemoteObject(
					getDBusSender(),
					OBJECT_PATH,
					DBus.Properties.class
				);

				propertiesByName = properties.GetAll(INTERFACE_NAME);
			}
			catch (DBus.Error.ServiceUnknown e)
			{
				log.debug("{} is not running", getDBusSenderSuffix());
				responsive = false;
				connection = null;
				return new StateSnapshot(PlayState.Stopped, null, null, null, NO_TIME_CODE, null, null);
			}
			catch (DBusException e)
			{
				throw new RuntimeException(e);
			}
		}

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

		final
		String album;

		final
		String title;

		if (metadata.isEmpty())
		{
			log.trace("no file open, {}", playState);
			url=null;
			zimPage=null;
			album=null;
			title=null;
		}
		else
		{
			album = optionalString(metadata, "xesam:album");
			title = optionalString(metadata, "xesam:title");

			url = (String) metadata.get("xesam:url").getValue();
			{
				log.trace("url: {}", url);
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
		String roughTimeCode=getRoughTimeCode(position);

		return new StateSnapshot(playState, position, url, zimPage, roughTimeCode, album, title);
	}

	private
	String optionalString(Map<String, Variant> metadata, String fieldName)
	{
		final
		Variant variant = metadata.get(fieldName);

		if (variant==null)
		{
			return null;
		}
		else
		{
			return (String)variant.getValue();
		}
	}

	private static final
	String NO_TIME_CODE = "X";

	/**
	 * @param positionMicroSeconds
	 * @return a human-readable time code with
	 */
	private
	String getRoughTimeCode(Long positionMicroSeconds)
	{
		if (positionMicroSeconds==null)
		{
			return "00:00";
		}
		else
		{
			long minutes = TimeUnit.MICROSECONDS.toMinutes(positionMicroSeconds);

			final
			long hours=minutes/60;

			minutes=minutes%60;

			if (hours>0)
			{
				return String.format("%d:%02d:00", hours, minutes);
			}
			else
			{
				return String.format("%02d:00", minutes);
			}
		}
	}

	private
	boolean isLocalPodcastUrl(String url)
	{
		//return url.startsWith("file:///mnt/shared/Podcasts/");
        //return url.startsWith("/mnt/media/Podcasts");
        return url.contains("/Podcasts/");
    }

	private
	boolean warrantsFiringCallback(StateSnapshot previousState, StateSnapshot newState)
	{
		if (!previousState.getPlayState().equals(newState.getPlayState()))
		{
			return true;
		}

		final
		String oldSubject=previousState.getZimPage();

		final
		String newSubject=newState.getZimPage();

		if (newSubject==null)
		{
			return oldSubject!=null;
		}
		else
		{
			return !newSubject.equals(oldSubject);
		}

		/*
		NB: eventually, we will probably want to add a file/uri check too. In which case,
		we might as well push the logic into the 'equals()' function, methinks.
		 */
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
