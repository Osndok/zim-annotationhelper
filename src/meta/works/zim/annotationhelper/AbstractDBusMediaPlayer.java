package meta.works.zim.annotationhelper;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.types.DBusMapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robert on 2016-10-06 11:30.
 */
public abstract
class AbstractDBusMediaPlayer extends Thread
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
			catch (Throwable t)
			{
				responsive=false;
				connection=null;
				log.error("caught", t);
			}

			try
			{
				Thread.sleep(5000);
			}
			catch (InterruptedException e)
			{
				log.error("interrupted");
				return;
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
		PlayState playState = PlayState.valueOf(get(String.class, "PlaybackStatus"));

		final
		Long position = get(Long.class, "Position");

		final
		String zimPage;

		final
		Map<String,Variant> metadata=get(Map.class, "Metadata");

		if (metadata.isEmpty())
		{
			log.info("no file open, {}", playState);
			zimPage=null;
		}
		else
		{
			final
			String url = (String) metadata.get("xesam:url").getValue();
			{
				log.debug("url: {}", url);
			}

			zimPage=zimPageNameExtractor.getZimPageNameFor(url);
		}

		final
		StateSnapshot newState=new StateSnapshot(playState, position, zimPage);

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
		return (T)propertiesByName.get(keyName).getValue();
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
}
