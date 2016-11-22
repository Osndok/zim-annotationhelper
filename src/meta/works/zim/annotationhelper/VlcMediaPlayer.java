package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static meta.works.zim.annotationhelper.PlayState.Paused;
import static meta.works.zim.annotationhelper.PlayState.Playing;
import static meta.works.zim.annotationhelper.PlayState.Stopped;

/**
 * Created by robert on 2016-10-06 11:31.
 *
 * NB: gets a lot less data than rhythmbox (which has/controls the podcatcher):
 *
 * (Metadata entry)
 * [Variant: [Argument: a{sv} {
 * 	"mpris:trackid" = [Variant: [ObjectPath: /org/videolan/vlc/playlist/17]],
 * 	"xesam:url" = [Variant(QString): "file:///mnt/shared/Podcasts/All%20Jupiter%20Broadcasting%20Videos/cr-0232-432p.mp4"],
 * 	"vlc:time" = [Variant(uint): 3726],
 * 	"mpris:length" = [Variant(qlonglong): 3726976000],
 * 	"vlc:encodedby" = [Variant(QString): "Lavf57.25.100"],
 * 	"vlc:length" = [Variant(qlonglong): 3726976],
 * 	"vlc:publisher" = [Variant(int): 5]
 * }]]
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
			if (was.getZimPage()!=null)
			{
				zimPageAppender.journalNote("Finished [["+was.getZimPage()+"]] ?");
			}

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
