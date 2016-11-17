package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static meta.works.zim.annotationhelper.PlayState.Paused;
import static meta.works.zim.annotationhelper.PlayState.Playing;
import static meta.works.zim.annotationhelper.PlayState.Stopped;

/**
 * Created by robert on 2016-10-06 11:31.
 */
public
class VlcMediaPlayer extends AbstractDBusMediaPlayer
{
	private static final
	Logger log = LoggerFactory.getLogger(VlcMediaPlayer.class);

	public
	VlcMediaPlayer()
	{
		super("vlc");
	}

	@Override
	String getDBusSenderSuffix()
	{
		return "vlc";
	}

	@Override
	void onStateChange(StateSnapshot was, StateSnapshot now, long age) throws IOException, InterruptedException
	{
		final
		String zimPage = now.getZimPage();

		if (zimPage==null)
		{
			//probably music...
			return;
		}

		final
		boolean changedShows = !was.sameShowAs(now);

		if (now.getPlayState()==Playing)
		{
			if (was.getPlayState() == Stopped || changedShows)
			{
				zimPageAppender.journalNote("Starting [["+zimPage+"]]");
			}
			else
			if (was.getPlayState() == Paused && age > NOTABLY_STALE_STATE_MILLIS)
			{
				log.debug("pause -> play @ {}ms age", age);
				zimPageAppender.journalNote("Resuming [["+zimPage+"]]");
			}
		}
		else
		if (changedShows)
		{
			zimPageAppender.journalNote("[["+zimPage+"]] "+now.getPlayState());
		}
	}

	@Override
	void onPeriodicInterval(StateSnapshot state) throws IOException, InterruptedException
	{
		if (state.getZimPage()!=null)
		{
			zimPageAppender.pageNote(state.getZimPage(), state.getRoughTimeCode());
		}
	}

	public static final
	void main(String[] args)
	{
		new VlcMediaPlayer().run();
	}
}
