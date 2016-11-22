package meta.works.zim.annotationhelper;

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

	public
	StateSnapshot(
		PlayState playState,
		Long position,
		String url,
		String zimPage,
		String roughTimeCode,
		String album,
		String title
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
			//", time=" + time +
			'}';
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
}
