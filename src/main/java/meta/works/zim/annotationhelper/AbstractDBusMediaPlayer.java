package meta.works.zim.annotationhelper;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    long SLEEP_MILLIS = TimeUnit.SECONDS.toMillis(2);

	private static final
	long RECENT_ACTIVITY_THRESHOLD = TimeUnit.MINUTES.toMillis(5);

	private long lastStateChangeCallback=System.currentTimeMillis();

	abstract String getDBusSenderSuffix();

	abstract
	StateChangeReturn onStateChange(StateSnapshot was, StateSnapshot now, long age) throws IOException, InterruptedException;

	abstract void onPeriodicInterval(StateSnapshot state) throws IOException, InterruptedException;

	public static final
	long NOTABLY_STALE_STATE_MILLIS = TimeUnit.MINUTES.toMillis(5);

	private static final
	Logger log = LoggerFactory.getLogger(AbstractDBusMediaPlayer.class);

	public
	AbstractDBusMediaPlayer(
			String name
	)
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

	protected
	boolean ignoringDocumentedReplay;

	private final
	PositionApproximator positionApproximator = new PositionApproximator();

	private
	void run2() throws Exception
	{
		final StateSnapshot newState = getNewState();

		final StateSnapshot previousState;
		{
			previousState = this.stateSnapshot;
			stateSnapshot = newState;
		}

		final String newZimPageName = newState.getZimPage();

		if (previousState == null)
		{
			return;
		}

		if (getName().equals("spotify"))
		{
			final var approximatedPosition = positionApproximator.onStateChange(previousState, newState) * 1000;

			if (newState.position == null || newState.position == 0L)
			{
				newState.position = approximatedPosition;
				newState.roughTimeCode = StateSnapshot.getRoughTimeCode(approximatedPosition);
				log.trace("approximated position: {}", newState.roughTimeCode);
			}
			else
			{
				// Please let me know if you ever get this message spammed at you... :)
				log.debug("spotify mpris position seems to be working");
			}
		}

		if (warrantsFiringCallback(previousState, newState))
		{
			log.debug("onStateChange: {} -> {}", previousState, newState);

			if (newState.getPlayState() == PlayState.Playing)
			{
				if (newState.refersToSameContentAs(previousState) && previousState.playState== PlayState.Paused)
				{
					log.debug("Suppressing notebook entry for play/pause pair");
				}
				else
				{
					zimPageAppender.nowPlaying(newState);
				}

				if (newZimPageName == null)
				{
					ignoringDocumentedReplay = false;
				}
				else
				{
					final File zimPageFile = zimPageAppender.getPageFile(newZimPageName);

					ignoringDocumentedReplay = EndMarker.isPresentIn(zimPageFile);
				}
			}

			if (ignoringDocumentedReplay)
			{
				log.info("zim page already has an end marker, so ignoring this replay: {}", newZimPageName);

				if (newZimPageName != null)
				{
					zimPageAppender.journalNote("Replaying [[" + newZimPageName + "]]");
				}

				return;
			}

			final long now = System.currentTimeMillis();

			final StateChangeReturn scr = onStateChange(previousState, newState, now - lastStateChangeCallback);

			lastStateChangeCallback = now;

			if (firstTimeCode(newState) && newState.getPlayState() == PlayState.Playing &&
				(scr == null || !scr.isInitialTimeCodeSuppressed()))
			{
				log.debug("adding initial 0:00 interval @ {}", newState);
				onStartingNewContentSpecificPage(newZimPageName, newState);
				zimPageAppender.pageNote(newZimPageName, "");
				onPeriodicInterval(newState);
			}
		}
		else if (!previousState.getRoughTimeCode().equals(newState.getRoughTimeCode()))
		{
			log.trace("periodic");

			if (ignoringDocumentedReplay) return;

			onPeriodicInterval(newState);
		}
		else
		{
			log.trace("not notable, and same rough time code: {} == {}", previousState.roughTimeCode, newState.roughTimeCode);
		}
	}

	protected
	void onStartingNewContentSpecificPage(final String zimPageName, final StateSnapshot state) throws
																							   IOException,
																							   InterruptedException
	{
		zimPageAppender.pageNote(zimPageName, state.title);
	}

	protected
	boolean firstTimeCode(StateSnapshot state)
	{
		final
		String roughTimeCode = state.getRoughTimeCode();

		return roughTimeCode!=null && roughTimeCode.equals("00:00");
	}

	private
	Boolean appRunning;

	private
	StateSnapshot extractDBusMetadata()
	{
		final
		DBus.Properties properties;
		{
			try
			{
				if (connection == null)
				{
					connection = DBusConnection.getConnection(DBusConnection.SESSION);
				}

				properties = connection.getRemoteObject(
						getDBusSender(),
						OBJECT_PATH,
						DBus.Properties.class
				);

				propertiesByName = properties.GetAll(INTERFACE_NAME);

				if (appRunning==Boolean.TRUE)
				{
					log.trace("{} is running", getDBusSenderSuffix());
				}
				else
				{
					appRunning=Boolean.TRUE;
					log.debug("{} is running", getDBusSenderSuffix());
				}
			}
			catch (DBus.Error.ServiceUnknown e)
			{
				if (appRunning==Boolean.FALSE)
				{
					log.trace("{} is NOT running", getDBusSenderSuffix());
				}
				else
				{
					appRunning=Boolean.FALSE;
					log.debug("{} is NOT running", getDBusSenderSuffix());
				}

				connection = null;
				return new StateSnapshot(PlayState.Stopped, null, null, null);
			}
			catch (DBusException e)
			{
				throw new RuntimeException(e);
			}
		}

		if (log.isTraceEnabled())
		{
			for (Map.Entry<String, Variant<?>> me : propertiesByName.entrySet())
			{
				final
				Object value = me.getValue().getValue();

				log.trace("'{}' = ({}) '{}'", me.getKey(), value.getClass(), value);
			}
		}
		return null;
	}

	private
	StateSnapshot getNewState()
	{
		StateSnapshot dbusError = extractDBusMetadata();

		if (dbusError!=null)
		{
			return dbusError;
		}

		final
		String playStateString = get(String.class, "PlaybackStatus");

		final
		PlayState playState = PlayState.valueOf(playStateString);

		final
		Map<String,Variant> metadata=get(Map.class, "Metadata");

		final
		Long position = get(Long.class, "Position");

		final
		String zimPage = getZimPage(metadata);

		return new StateSnapshot(playState, position, zimPage, metadata);
	}

	protected
	String getZimPage(final Map<String, Variant> metadata)
	{
		final
		String url = getString(metadata, "xesam:url");

		if (!isLocalPodcastUrl(url))
		{
			return null;
		}

		return zimPageNameExtractor.getZimPageNameFor(url);
	}

	public static
	String getString(Map<String, Variant> metadata, String fieldName)
	{
		if (metadata==null)
		{
			return null;
		}

		final
		Variant variant = metadata.get(fieldName);

		if (variant==null)
		{
			log.trace("no entry for: '{}'", fieldName);
			return null;
		}
		else
		{
			String stringValue = (String)variant.getValue();
			log.trace("{} = {}", fieldName, stringValue);
			return stringValue;
		}
	}

	private
	boolean isLocalPodcastUrl(String url)
	{
		//return url.startsWith("file:///mnt/shared/Podcasts/");
        //return url.startsWith("/mnt/media/Podcasts");
        return url != null && url.contains("/Podcasts/");
    }

	private
	boolean warrantsFiringCallback(StateSnapshot previousState, StateSnapshot newState)
	{
		if (!previousState.getPlayState().equals(newState.getPlayState()))
		{
			return true;
		}

		return !previousState.refersToSameContentAs(newState);
	}

	public static final
	ZimPageAppender zimPageAppender = new ZimPageAppender();

	public static final
	ZimPageNameExtractor zimPageNameExtractor=new ZimPageNameExtractor(false);

	Map<String, Variant<?>> propertiesByName=new HashMap<>();

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
	StateSnapshot stateSnapshot;

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

	protected
	void finishedPlaying(StateSnapshot was) throws IOException, InterruptedException
	{
		zimPageAppender.pageNote(was.getZimPage(), EndMarker.STRING); // writes ".end"
		zimPageAppender.journalNote("Finished [[" + was.getZimPage() + "]]");
	}

	protected static
	boolean isRecent(StateSnapshot stateSnapshot)
	{
		final
		long now = System.currentTimeMillis();

		final
		long snapshotTime = stateSnapshot.getSnapshotTime();

		if ( now-snapshotTime > RECENT_ACTIVITY_THRESHOLD)
		{
			log.debug("isRecent() identifies stale state: {} - {} = {} > {}", now, snapshotTime, now-snapshotTime, RECENT_ACTIVITY_THRESHOLD);
			return false; // NOT recent
		}
		else
		{
			return true; // *IS* recent
		}
	}
}
