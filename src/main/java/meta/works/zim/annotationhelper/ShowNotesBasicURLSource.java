package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Given a digested Zim path to A PARTICULAR EPISODE, this class aims to provide a
 * url to the official/companion "show notes" if that url pattern is guessable.
 *
 * Created by robert on 2016-11-22 11:36.
 */
public
class ShowNotesBasicURLSource extends AbstractShowNotesSource
{
	private static final
	Logger log = LoggerFactory.getLogger(ShowNotesBasicURLSource.class);

	@Override
	public
	String unsafe_getShowNotesURL(String showName, String episodeName)
	{
		unsafeShowNotesUrlNumCalls++;

		if (showName.equals("DWW"))
		{
			return "https://distrowatch.com/weekly.php?issue="+episodeName;
		}
		else
		if (showName.equals("LNL"))
		{
			return "https://latenightlinux.com/late-night-linux-episode-"+episodeName+"/";
		}
		else
		if (showName.equals("LinuxAction:News"))
		{
			return "http://linuxactionnews.com/"+episodeName;
		}
		else
		if (showName.startsWith("FreeTalk:"))
		{
			return "https://www.freetalklive.com/podcast/"+reformatFreeTalkDate(episodeName);
		}
		else
		if (showName.equals("NA"))
		{
			final
			Scanner scanner = new Scanner(episodeName).useDelimiter("[^0-9]+");

			if (scanner.hasNext())
			{
				final
				int episodeNumber=scanner.nextInt();

				if (episodeNumber <= 300)
				{
					//system 1 - "http://noagenda.wikia.com/wiki/No_Agenda_123:_%22Thanksgiving_Turkeys%22"
					return null;
				}
				else
				if (episodeNumber <= 581)
				{
					//system 2 - freedom controller + blog - pre-data-loss?
					// e.g. "http://blog.curry.com/stories/2011/12/29/na36920111229.html" ????
					return null;
				}
				else
				{
					//system 3 - freedom controller only
					//return String.format("http://%d.nashownotes.com", episodeNumber);
					//system 4 - adam loves his domain names.
					return String.format("http://%d.noagendanotes.com", episodeNumber);
				}
			}
			else
			{
				log.debug("unable to parse NA show number from: '{}'", episodeName);
				return null;
			}
		}
		else
		if (showName.equals("SN"))
		{
			return String.format("https://www.grc.com/sn/sn-%s-notes.pdf", episodeName);
		}
		else
		if (showName.equals("TWIT"))
		{
			return String.format("https://twit.tv/shows/this-week-in-tech/episodes/%s?autostart=false", episodeName);
		}
		else
		if (showName.equals("UE"))
		{
			return String.format("https://error.show/%s", episodeName);
		}

		log.info("unhandled show: '{}' / episode: '{}'", showName, episodeName);
		return null;
	}

	private
	String reformatFreeTalkDate(String ymd)
	{
		return ymd.replace(':','-');
	}
}
