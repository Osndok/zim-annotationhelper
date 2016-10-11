package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final
	Logger log = LoggerFactory.getLogger(RhythmBoxMediaPlayer.class);

	public
	RhythmBoxMediaPlayer()
	{
		super("rhythmbox");
	}

	@Override
	String getDBusSenderSuffix()
	{
		return "rhythmbox";
	}

	@Override
	void onStateChange(StateSnapshot was, StateSnapshot now, long age) throws IOException, InterruptedException
	{
		if (now.getUrl()!=null && now.getPlayState() == Playing)
		{
			//Avoid "streaming" files.
			if (now.getUrl().startsWith("http") )
			{
				Runtime.getRuntime().exec("espeak undownloaded");
				stopPlayback();
				return;
			}

			//Avoid playing video files through RB.
			if (LooksLike.videoFile(now.getUrl()))
			{
				stopPlayback();

				if (vlcIsRunning())
				{
					tellVlcToPlay(now.getUrl());
				}
				else
				{
					launchVlcWithUrl(now.getUrl());
				}

				return;
			}
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
			if (was.getPlayState() == Stopped  || !was.sameShowAs(now))
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
	}

	private
	boolean vlcIsRunning() throws IOException, InterruptedException
	{
		final
		Process process=Runtime.getRuntime().exec("pgrep vlc");

		return process.waitFor()==0;
	}

	private
	void tellVlcToPlay(String url) throws IOException
	{
		Runtime.getRuntime().exec(new String[]{
			"qdbus",
			"org.mpris.MediaPlayer2.vlc",
			"/org/mpris/MediaPlayer2",
			"org.mpris.MediaPlayer2.Player.OpenUri",
			url
		});
	}

	private
	void launchVlcWithUrl(String url) throws IOException
	{
		Runtime.getRuntime().exec("vlc "+url);
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
		new RhythmBoxMediaPlayer().run();
	}
}
