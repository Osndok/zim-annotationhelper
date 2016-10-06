package meta.works.zim.annotationhelper;

/**
 * Created by robert on 2016-10-06 13:09.
 */
public
class StateSnapshot
{
	private final
	PlayState playState;

	private final
	long position;

	private final
	String zimPage;

	private final
	long time;

	public
	StateSnapshot(PlayState playState, long position, String zimPage)
	{
		this.playState = playState;
		this.position = position;
		this.zimPage = zimPage;

		this.time = System.currentTimeMillis();
	}

	public
	PlayState getPlayState()
	{
		return playState;
	}

	public
	long getPosition()
	{
		return position;
	}

	public
	String getZimPage()
	{
		return zimPage;
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
