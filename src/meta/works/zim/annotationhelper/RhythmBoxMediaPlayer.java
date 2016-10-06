package meta.works.zim.annotationhelper;

import java.io.IOException;

import static meta.works.zim.annotationhelper.PlayState.Paused;
import static meta.works.zim.annotationhelper.PlayState.Playing;
import static meta.works.zim.annotationhelper.PlayState.Stopped;

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
	void onStateChange(StateSnapshot was, StateSnapshot now, long age) throws IOException, InterruptedException
	{
		//Avoid streaming files...
		if (now.getUrl()!=null && now.getUrl().startsWith("http") && now.getPlayState()==Playing)
		{
			Runtime.getRuntime().exec("espeak undownloaded");
			stopPlayback();
			return;
		}

		final
		String zimPage = now.getZimPage();

		if (zimPage==null)
		{
			//probably music...
			return;
		}

		if (now.getPlayState()==Playing)
		{
			if (was.getPlayState() == Stopped)
			{
				zimPageAppender.journalNote("Starting [["+zimPage+"]]");
			}
			else
			if (was.getPlayState() == Paused && age > NOTABLY_STALE_STATE_MILLIS)
			{
				zimPageAppender.journalNote("Resuming [["+zimPage+"]]");
			}
		}
	}

	@Override
	void onPeriodicInterval(StateSnapshot state, String timeCode) throws IOException, InterruptedException
	{
		zimPageAppender.pageNote(state.getZimPage(), timeCode);
	}

	public static final
	void main(String[] args)
	{
		new RhythmBoxMediaPlayer().run();
	}
}
