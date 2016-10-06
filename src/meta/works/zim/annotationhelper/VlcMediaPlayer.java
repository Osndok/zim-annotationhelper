package meta.works.zim.annotationhelper;

import java.io.IOException;

/**
 * Created by robert on 2016-10-06 11:31.
 */
public
class VlcMediaPlayer extends AbstractDBusMediaPlayer
{
	@Override
	String getDBusSenderSuffix()
	{
		return "vlc";
	}

	@Override
	void onStateChange(StateSnapshot was, StateSnapshot now, long age)
	{

	}

	@Override
	void onPeriodicInterval(StateSnapshot state, String timeCode) throws IOException, InterruptedException
	{

	}

	public static final
	void main(String[] args)
	{
		new VlcMediaPlayer().run();
	}
}
