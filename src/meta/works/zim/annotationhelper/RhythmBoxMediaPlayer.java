package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static meta.works.zim.annotationhelper.PlayState.Paused;
import static meta.works.zim.annotationhelper.PlayState.Playing;
import static meta.works.zim.annotationhelper.PlayState.Stopped;

/**
 * Created by robert on 2016-10-06 11:32.
 *
 * [Variant: [Argument: a{sv} {
 * 	"mpris:trackid" = [Variant(QString): "/org/mpris/MediaPlayer2/Track/9925"],
 * 	"xesam:url" = [Variant(QString): "file:///mnt/shared/Podcasts/Distrowatch%20Weekly%20Podcast/dww20161121.mp3"],
 * 	"xesam:title" = [Variant(QString): "DistroWatch Weekly, Issue 688 21 November 2016"],
 * 	"xesam:artist" = [Variant(QStringList): {""menckenmadness@gmail.com" (Bruce Patterson)"}],
 * 	"xesam:album" = [Variant(QString): "Distrowatch Weekly Podcast"],
 * 	"xesam:genre" = [Variant(QStringList): {"Podcast"}],
 * 	"xesam:audioBitrate" = [Variant(int): 128000],
 * 	"xesam:lastUsed" = [Variant(QString): "2016-11-22T17:49:46Z"],
 * 	"mpris:length" = [Variant(qlonglong): 2597000000],
 * 	"xesam:trackNumber" = [Variant(int): 0],
 * 	"xesam:useCount" = [Variant(int): 1],
 * 	"xesam:userRating" = [Variant(double): 0]
 * }]]
 */
public
class RhythmBoxMediaPlayer extends AbstractDBusMediaPlayer
{
	private static final
	Logger log = LoggerFactory.getLogger(RhythmBoxMediaPlayer.class);

	private final
	ShowNotesSource showNotesSource =new ShowNotesMultiSource();

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

	boolean kludge_automaticHalt;

	@Override
	void onStateChange(StateSnapshot was, StateSnapshot now, long age) throws IOException, InterruptedException
	{
		if (now.getUrl()!=null && now.getPlayState() == Playing)
		{
			if (kludge_automaticHalt)
			{
				log.debug("kludge_automaticHalt");
				return;
			}

			//Avoid "streaming" files.
			if (now.getUrl().startsWith("http") )
			{
				Runtime.getRuntime().exec("espeak undownloaded");
				stopPlayback();
				kludge_automaticHalt=true;
				return;
			}

			//Avoid playing video files through RB.
			if (LooksLike.videoFile(now.getUrl()))
			{
				stopPlayback();
				kludge_automaticHalt=true;

				//Do this first so that vlc won't run away making logging statements & mess up the order.
				noteTitleAndShowNotes(now);

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
			if (was.getZimPage()!=null)
			{
				if (kludge_automaticHalt)
				{
					//mute meaningless 'finished', as vlc is playing... or it otherwise never really started.
					log.debug("intercepted 'finished' message");
					kludge_automaticHalt=false;
				}
				else
				{
					zimPageAppender.journalNote("Finished [[" + was.getZimPage() + "]] ?");
				}
			}

			return;
		}

		kludge_automaticHalt=false;

		final
		boolean changedShows=!was.sameShowAs(now);

		if (now.getPlayState()==Playing)
		{
			if (was.getPlayState() == Stopped  || changedShows || firstTimeCode(now))
			{
				zimPageAppender.journalNote("Starting [["+zimPage+"]]");
				noteTitleAndShowNotes(now);
			}
			else
			if (was.getPlayState() == Paused && age > NOTABLY_STALE_STATE_MILLIS)
			{
				log.debug("pause -> play @ {}ms age", age);

				if (now.getRoughTimeCode()==null)
				{
					zimPageAppender.journalNote("Resuming [[" + zimPage + "]]");
				}
				else
				{
					zimPageAppender.journalNote("Resuming [[" + zimPage + "]] @ "+now.getRoughTimeCode());
				}
			}
		}
		else
		if (changedShows)
		{
			zimPageAppender.journalNote("[["+zimPage+"]] "+now.getPlayState());
			noteTitleAndShowNotes(now);
		}
	}

	private
	void noteTitleAndShowNotes(StateSnapshot stateSnapshot) throws IOException, InterruptedException
	{
		final
		String zimPage = stateSnapshot.getZimPage();

		if (zimPage!=null)
		{
			boolean didSomething=false;

			final
			String title=stateSnapshot.getTitle();
			{
				if (title!=null)
				{
					if (title.indexOf('"')>=0)
					{
						zimPageAppender.pageNote(zimPage, String.format("%s", title));
					}
					else
					{
						zimPageAppender.pageNote(zimPage, String.format("\"%s\"", title));
					}

					didSomething=true;
				}
			}

			final
			String album=stateSnapshot.getAlbum();

			final
			String showNotesUrl= showNotesSource.getShowNotesURL(zimPage);
			{
				if (showNotesUrl!=null)
				{
					zimPageAppender.pageNote(zimPage, String.format("* [[%s|Show Notes]]", showNotesUrl));
					didSomething=true;
				}
			}

			if (didSomething)
			{
				//extra newline to separate the coming time codes.
				zimPageAppender.pageNote(zimPage, "");
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
