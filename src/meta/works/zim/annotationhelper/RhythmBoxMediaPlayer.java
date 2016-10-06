package meta.works.zim.annotationhelper;

/**
 * Created by robert on 2016-10-06 11:32.
 */
public
class RhythmBoxMediaPlayer extends AbstractDBusMediaPlayer
{
	@Override
	String getDBusSenderSuffix()
	{
		return "rhythmbox";
	}

	@Override
	void onStateChange(StateSnapshot was, StateSnapshot now, long age)
	{

	}

	public static final
	void main(String[] args)
	{
		new RhythmBoxMediaPlayer().run();
	}
}
