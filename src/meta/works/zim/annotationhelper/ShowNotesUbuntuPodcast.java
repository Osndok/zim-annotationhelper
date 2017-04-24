package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by robert on 2017-04-06 17:04.
 */
public
class ShowNotesUbuntuPodcast extends AbstractShowNotesSource
{
	private static final
	Logger log = LoggerFactory.getLogger(ShowNotesUbuntuPodcast.class);

	@Override
	protected
	boolean acceptUnparsedZimPageName(String zimPageName)
	{
		return zimPageName.startsWith(":UbuntuPodcast:");
	}

	@Override
	public
	String unsafe_getShowNotesURL(String seasonWithPrefix, String episodeWithPrefix)
	{
		return getShowNotesUrlFromPage(seasonWithPrefix.substring(1), episodeWithPrefix, 1);
	}

	private static final
	String INDIRECT_URL_ALTERNATE_PAGE="http://ubuntupodcast.org/category/season-%s/page/%d/";

	private static final
	int MAX_PAGES_TO_SEARCH = 10;

	private
	String getShowNotesUrlFromPage(String twoDigitSeason, String episodeIdentifier, int pageNumber)
	{
		final
		String url;
		{
			url = String.format(INDIRECT_URL_ALTERNATE_PAGE, twoDigitSeason, pageNumber);
			log.debug("page {} url is: '{}'", pageNumber, url);
		}

		final
		String urlContent = fetchUrlContent(url);
		{
			log.trace("got content: {}", urlContent);
		}

		final
		String innerUrl = extractPrecedingHrefForShow(urlContent, twoDigitSeason, episodeIdentifier);

		if (innerUrl==null && pageNumber < MAX_PAGES_TO_SEARCH)
		{
			return getShowNotesUrlFromPage(twoDigitSeason, episodeIdentifier, pageNumber+1);
		}
		else
		{
			return innerUrl;
		}
	}

	private
	String extractPrecedingHrefForShow(String urlContent, String twoDigitSeason, String episodeIdentifier)
	{
		final
		String slashSeasonEpisode=String.format("/s%s%s", twoDigitSeason, episodeIdentifier);

		final
		int center=urlContent.indexOf(slashSeasonEpisode);
		{
			if (center<0)
			{
				log.debug("could not find '{}' on season-{} index page", slashSeasonEpisode, twoDigitSeason);
				return null;
			}
			else
			{
				log.debug("found '{}' @ {}", slashSeasonEpisode, center);
			}
		}

		final
		int highLimit=urlContent.indexOf('"', center);
		{
			if (highLimit<0)
			{
				log.debug("{} does not appear to be on this page", episodeIdentifier);
				return null;
			}
			else
			{
				log.debug("end = {}", highLimit);
			}
		}

		final
		int lowLimit=urlContent.lastIndexOf('"', center)+1;
		{
			log.debug("start = {}", lowLimit);
		}

		final
		String href=urlContent.substring(lowLimit, highLimit);
		{
			log.debug("href @ {}", href);
		}

		return href;
	}
}
