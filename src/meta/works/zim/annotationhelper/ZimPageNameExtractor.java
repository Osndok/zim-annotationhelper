package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a URL or file-name, return/compute the Zim page name therefor.
 */
public
class ZimPageNameExtractor
{
	private static final
	Logger log = LoggerFactory.getLogger(ZimPageNameExtractor.class);

	/*
	http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
	 */
	private static final
	String BREAK_MATCHING_REGEX=String.format(
		"%s|%s|%s|%s",
		"(?<=[A-Z])(?=[A-Z][a-z])",
		"(?<=[^A-Z])(?=[A-Z])",
		"(?<=[A-Za-z])(?=[^A-Za-z])",
		"(?<=[0-9])(?=[^0-9])"
	);

	public
	String getZimPageNameFor(String url)
	{
		final
		String noPathOrFileExt;
		{
			final
			int beginIndex;
			{
				final
				int slash=url.lastIndexOf('/');

				if (slash>=0)
				{
					beginIndex=slash+1;
				}
				else
				{
					beginIndex=0;
				}
			}

			final
			int endIndex;
			{
				final
				int period = url.lastIndexOf('.');

				if (period > 0)
				{
					endIndex=period;
				}
				else
				{
					endIndex=url.length();
				}
			}

			noPathOrFileExt=url.substring(beginIndex, endIndex);
		}

		final
		String[] bits = noPathOrFileExt.replaceAll(BREAK_MATCHING_REGEX, " ").split(" ");
		{
			log.debug("split into {} bits", bits.length);

			for (String bit : bits)
			{
				log.debug("bit: {}", bit);
			}
		}

		final
		Strategy strategy = getStrategy(bits);

		switch(strategy)
		{
			case BIT_ONE_IS_EPISODE_NUMBER:
			{
				return refine(bits[0], bits[1]);
			}
		}

		return "wip";
	}

	private
	String refine(String showName, String episode)
	{
		return String.format(
			":%s:%s",
			refineShowName(showName),
			refineEpisodeNumber(episode)
		);
	}

	private
	String refineShowName(String showName)
	{
		if (showName.length()<=3)
		{
			return showName.toUpperCase();
		}
		else
		if (showName.startsWith("linuxaction"))
		{
			return "LAS";
		}
		else
		if (showName.equals("techsnap"))
		{
			return "TechSNAP";
		}
		else
		{
			return upperFirst(showName);
		}
	}

	private
	String upperFirst(String showName)
	{
		return Character.toUpperCase(showName.charAt(0))+showName.substring(1);
	}

	private
	String refineEpisodeNumber(String episode)
	{
		while (badNumberChar(episode.charAt(0)))
		{
			episode=episode.substring(1);
		}

		return episode;
	}

	private
	boolean badNumberChar(char c)
	{
		return c=='-' || c=='0';
	}

	private
	Strategy getStrategy(String[] bits)
	{
		final
		String firstBit = bits[0];

		return Strategy.BIT_ONE_IS_EPISODE_NUMBER;
	}

	private
	enum Strategy
	{
		BIT_ONE_IS_EPISODE_NUMBER
	}
}
