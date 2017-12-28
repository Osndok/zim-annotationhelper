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
	StateChangeReturn onStateChange(StateSnapshot was, StateSnapshot now, long age) throws IOException, InterruptedException
	{
		//NB: we often get a 'paused' state before 'playing'.
		if (now.getUrl()!=null && now.getPlayState() != Stopped)
		{
			if (kludge_automaticHalt)
			{
				log.debug("kludge_automaticHalt");
				return null;
			}

			//Avoid "streaming" those files which are intended to be downloaded.
			if (now.getUrl().startsWith("http") && !acceptableStreamingSource(now.getUrl()))
			{
				log.warn("url triggers undownloaded catch: '{}'", now.getUrl());
				Runtime.getRuntime().exec("espeak undownloaded");
				stopPlayback();
				//NB: The following can stick (if it never actually starts playing), so for now we don't:
				//kludge_automaticHalt=true;
				return new StateChangeReturn().withInitialTimeCodeSuppressed();
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

				return new StateChangeReturn().withInitialTimeCodeSuppressed();
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
					log.debug("intercepted 'finished' message (zim page became null)");
					kludge_automaticHalt=false;
				}
				else
				{
					finishedPlaying(was);
				}
			}

			return null;
		}

		kludge_automaticHalt=false;

		final
		boolean changedShows=!was.sameShowAs(now);

		if (changedShows && was.getZimPage()!=null)
		{
			log.debug("changedShows");

			//TODO: there is another possibility... it could have been playing & hit the last timecode!
			if (was.getPlayState() == Paused)
			{
				zimPageAppender.journalNote("Left [[" + was.getZimPage() + "]]");
			}
			else
			if (isRecent(was))
			{
				finishedPlaying(was);
			}
			else
			{
				log.debug("...ignoring stale changedShow transition: {} -> {}", was, now);
			}
		}

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

		return null;
	}

	private
	boolean acceptableStreamingSource(String url)
	{
		return url.endsWith(".m3u")
			|| url.contains("soundcloud.com")
			;
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

			/* tends to add extra blank lines
			if (didSomething)
			{
				//extra newline to separate the coming time codes.
				log.debug("no top-level metadata...");
				zimPageAppender.pageNote(zimPage, "");
			}
			*/
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
			log.debug("onPeriodicInterval");
			zimPageAppender.pageNote(state.getZimPage(), state.getRoughTimeCode());
		}
	}

	public static final
	void main(String[] args)
	{
		new RhythmBoxMediaPlayer().run();
	}
}
