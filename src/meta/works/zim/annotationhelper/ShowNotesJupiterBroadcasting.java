package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 * Jupiter Broadcasting show notes are on a CMS that requires an ID.
 * Therefore, discovering the show notes link requires a URL call.
 *
 * Created by robert on 2017-01-06 11:45.
 */
public
class ShowNotesJupiterBroadcasting extends AbstractShowNotesSource
{
	private static final
	String INDIRECT_URL_FORMAT="http://www.jupiterbroadcasting.com/show/%s/";

	private static final
	String INDIRECT_URL_ALTERNATE_PAGE="http://www.jupiterbroadcasting.com/show/%s/page/%d/";

	private static final
	Logger log = LoggerFactory.getLogger(ShowNotesJupiterBroadcasting.class);

	private static final int MAX_PAGES_TO_SEARCH = 5;

	@Override
	public
	String unsafe_getShowNotesURL(String enclosingZimPage, String episodeIdentifier)
	{
		unsafeShowNotesUrlNumCalls++;

		final
		String jbWikiName = getJbWikiName(enclosingZimPage);
		{
			if (jbWikiName == null)
			{
				return null;
			}
		}

		return getShowNotesUrlFromPage(jbWikiName, episodeIdentifier, 1);
	}

	private
	String getShowNotesUrlFromPage(String jbWikiName, String episodeIdentifier, int pageNumber)
	{
		final
		String url;
		{
			url = String.format(INDIRECT_URL_ALTERNATE_PAGE, jbWikiName, pageNumber);
			log.debug("page {} url is: '{}'", pageNumber, url);
		}

		final
		String urlContent = fetchUrlContent(url);
		{
			log.trace("got content: {}", urlContent);
		}

		final
		String innerUrl = extractPrecedingHrefForShow(urlContent, episodeIdentifier);

		if (innerUrl==null && pageNumber < MAX_PAGES_TO_SEARCH)
		{
			return getShowNotesUrlFromPage(jbWikiName, episodeIdentifier, pageNumber+1);
		}
		else
		{
			return innerUrl;
		}
	}

	private
	String extractPrecedingHrefForShow(String urlContent, String episodeIdentifier)
	{
		final
		String spaceNumberQuote=String.format(" %s\"", episodeIdentifier);

		final
		int farLimit=urlContent.indexOf(spaceNumberQuote);
		{
			if (farLimit<0)
			{
				log.debug("{} does not appear to be on this page", episodeIdentifier);
				return null;
			}
			else
			{
				log.debug("found '{}' @ {}", spaceNumberQuote, farLimit);
			}
		}

		final
		int href=urlContent.lastIndexOf("href", farLimit);
		{
			log.debug("href @ {}", href);
		}

		final
		int quote2=urlContent.indexOf('"', href+10);
		{
			log.debug("quote2 @ {}", quote2);
		}

		final
		String innerUrl=urlContent.substring(href+6, quote2);
		{
			log.debug("extracted inner url: '{}'", innerUrl);
		}

		return innerUrl;
	}

	private
	String getJbWikiName(String enclosingZimPage)
	{
		final
		JBZimPageNameMapping zimPage;
		{
			try
			{
				zimPage = JBZimPageNameMapping.valueOf(enclosingZimPage);
			}
			catch (IllegalArgumentException e)
			{
				log.debug("not a jb show? {}", enclosingZimPage);
				log.trace("detail", e);
				return null;
			}
		}

		return zimPage.getJbWikiName();
	}

	private static
	enum JBZimPageNameMapping
	{
		BSD("bsdnow"),
		CR("coderradio"),
		LAS("linuxactionshow"),
		LUP("linuxun"),
		TechSNAP("techsnap"),
		Unfilter("unfilter"),
		UE("error")
		/*
		ARCHIVED SHOW WIKI NAMES
		--------------
		beeristasty
		cas
		fauxshow
		h2l
		indepthlook
		jointfailures
		nite
		legend-of-the-stoned-owl
		mmorgue
		planb
		rover-log
		scibyte
		stoked
		today
		torked
		wtr
		*/
		;

		private final
		String jbWikiName;

		JBZimPageNameMapping(String jbWikiName)
		{
			this.jbWikiName=jbWikiName;
		}

		public
		String getJbWikiName()
		{
			return jbWikiName;
		}
	}
}
