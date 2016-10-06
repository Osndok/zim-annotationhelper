package meta.works.zim.annotationhelper;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.types.DBusMapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static sun.corba.Bridge.get;

/**
 * Created by robert on 2016-10-06 11:30.
 */
public abstract
class AbstractDBusMediaPlayer extends Thread
{
	abstract String getDBusSenderSuffix();
	/*
	abstract void onBegin();
	abstract void onPaused();
	abstract void onResume();
	abstract void onStopOrFinished();
	*/

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

	private
	void run2() throws Exception
	{
		final
		DBusConnection connection = DBusConnection.getConnection(DBusConnection.SESSION);

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
			playbackStatus = get(String.class, "PlaybackStatus");
			position = get(Long.class, "Position");
		}

		responsive=true;

		for (Map.Entry<String, Variant> me : propertiesByName.entrySet())
		{
			final
			Object value = me.getValue().getValue();

			log.info("'{}' = ({}) '{}'", me.getKey(), value.getClass(), value);
		}

		final
		Map<String,Variant> metadata=get(Map.class, "Metadata");

		if (metadata.isEmpty())
		{
			log.info("no file open, {}", playbackStatus);
		}
		else
		{
			final
			String url = (String) metadata.get("xesam:url").getValue();
			{
				log.info("URL: {}", url);
			}
		}
	}

	Map<String, Variant> propertiesByName=new HashMap<String, Variant>();

	private <T>
	T get(Class<T> c, String keyName)
	{
		return (T)propertiesByName.get(keyName).getValue();
	}

	boolean responsive;
	String playbackStatus;
	Long position;

}
