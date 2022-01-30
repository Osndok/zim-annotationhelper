package meta.works.zim.annotationhelper;

import org.freedesktop.dbus.Variant;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import static meta.works.zim.annotationhelper.AbstractDBusMediaPlayer.getString;

/**
 * Created by robert on 2016-10-06 13:09.
 */
public
class StateSnapshot
{
	public final
	PlayState playState;

	public
	Long position;

	public final
	String zimPage;

	public final
	long time;

	public final
	String url;

	public
	String roughTimeCode;

	public final
	String album;

	public final
	String title;

	public final
	String trackId;

	public final
	String artist;

	public
	StateSnapshot(
			PlayState playState,
			Long position,
			String zimPage,
			final Map<String, Variant> metadata
	)
	{
		this.playState = playState;

		if (position==null)
		{
			this.position = null;
		}
		else
		{
			this.position = TimeUnit.MICROSECONDS.toSeconds(position);
		}

		this.zimPage = zimPage;

		this.time = System.currentTimeMillis();

		this.url = getString(metadata, "xesam:url");
		this.roughTimeCode = getRoughTimeCode(position);
		this.album = getString(metadata, "xesam:album");
		this.artist = getArtist(metadata);
		this.title = getString(metadata, "xesam:title");
		this.trackId = getString(metadata, "mpris:trackid");
	}

	private
	String getArtist(final Map<String, Variant> metadata)
	{
		final
		String fieldName = "xesam:artist";

		if (metadata==null)
		{
			return null;
		}

		final
		Variant variant = metadata.get(fieldName);

		if (variant==null)
		{
			return null;
		}

		Object value = variant.getValue();

		if (value instanceof Vector)
		{
			value = ((Vector)value).get(0);
		}

		return value.toString();
	}

	/**
	 * @param positionMicroSeconds
	 * @return a human-readable time code with
	 */
	public static
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

	@Deprecated
	public
	PlayState getPlayState()
	{
		return playState;
	}

	@Deprecated
	public
	String getZimPage()
	{
		return zimPage;
	}

	@Deprecated
	public
	String getUrl()
	{
		return url;
	}

	@Deprecated
	public
	String getRoughTimeCode()
	{
		return roughTimeCode;
	}

	@Deprecated
	public
	String getAlbum()
	{
		return album;
	}

	@Deprecated
	public
	String getTitle()
	{
		return title;
	}

	@Override
	public
	String toString()
	{
		return "{" +
			playState +
			" @ " + position +
			", zimPage='" + zimPage + '\'' +
			", time=" + format(time) +
			'}';
	}

	private static final
	DateFormat DATE_FORMAT=new SimpleDateFormat("yyyy-MM-dd @ HH:mm:ss z");

	private static
	String format(long time)
	{
		synchronized (DATE_FORMAT)
		{
			return DATE_FORMAT.format(new Date(time));
		}
	}

	public
	boolean sameShowAs(StateSnapshot other)
	{
		if (this.url==null)
		{
			return other.url==null;
		}
		else
		{
			return this.url.equals(other.getUrl());
		}
	}

	public
	long getSnapshotTime()
	{
		return time;
	}

	public
	String getTrackId()
	{
		return trackId;
	}

	public
	boolean refersToSameContentAs(final StateSnapshot that)
	{
		if (this.url != null && this.url.equals(that.url))
		{
			return true;
		}

		if (differs(this.zimPage, that.zimPage))
		{
			return false;
		}

		if (differs(this.album, that.album))
		{
			return false;
		}

		if (differs(this.artist, that.artist))
		{
			return false;
		}

		if (differs(this.title, that.title))
		{
			return false;
		}

		return true;
	}

	private
	boolean differs(final String a, final String b)
	{
		if (a == null)
		{
			return b!=null;
		}
		else
		{
			return !a.equals(b);
		}
	}
}
