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

	public
	StateSnapshot(PlayState playState, Long position, String url, String zimPage)
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
}
