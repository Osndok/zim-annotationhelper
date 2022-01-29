package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public
class Main
{
	private static final
	Logger log = LoggerFactory.getLogger(Main.class);

	public static
	void main(String[] args)
	{
		if (args.length==0)
		{
			log.debug("activated; starting monitors");
			new VlcMediaPlayer().start();
			new RhythmBoxMediaPlayer().start();
			new SpotifyPlayer().start();
			new PushbulletListener().activate();
		}
		else
		if (args.length==1)
		{
			final
			String option=args[0];

			if (option.equals("vlc"))
			{
				VlcMediaPlayer.main(args);
			}
			else
			if (option.equals("rb"))
			{
				RhythmBoxMediaPlayer.main(args);
			}
			else
			{
				throw new UnsupportedOperationException("unknown option: "+option);
			}
		}
		else
		{
			throw new UnsupportedOperationException("expecting one arg (for now)");
		}
	}
}
