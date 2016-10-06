package meta.works.zim.annotationhelper;

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

	public static final
	void main(String[] args)
	{
		new VlcMediaPlayer().run();
	}
}
