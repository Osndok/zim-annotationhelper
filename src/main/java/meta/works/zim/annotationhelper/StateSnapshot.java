package meta.works.zim.annotationhelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by robert on 2016-10-06 13:09.
 */
public
class StateSnapshot
{
	private final
	PlayState playState;

	private final
	Long position;

	private final
	String zimPage;

	private final
	long time;

	private final
	String url;

	private final
	String roughTimeCode;

	private final
	String album;

	private final
	String title;

	private final
	String trackId;

	public
	StateSnapshot(
			PlayState playState,
			Long position,
			String url,
			String zimPage,
			String roughTimeCode,
			String album,
			String title,
			String trackId
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
		this.url=url;
		this.roughTimeCode=roughTimeCode;
		this.album=album;
		this.title=title;
		this.trackId=trackId;
	}

	public
	PlayState getPlayState()
	{
		return playState;
	}

	/**
	 * @return the playback position in SECONDS, or null if it is unknown.
	 */
	public
	Long getPosition()
	{
		return position;
	}

	public
	String getZimPage()
	{
		return zimPage;
	}

	public
	String getUrl()
	{
		return url;
	}

	public
	String getRoughTimeCode()
	{
		return roughTimeCode;
	}

	public
	String getAlbum()
	{
		return album;
	}

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
}
