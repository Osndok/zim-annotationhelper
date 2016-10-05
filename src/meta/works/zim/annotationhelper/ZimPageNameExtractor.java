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

			for(int i=0; i<bits.length; i++)
			{
				log.debug("bit[{}]: {}", i, bits[i]);
			}
		}

		final
		Strategy strategy = getStrategy(noPathOrFileExt, bits);
		{
			log.debug("{} for {}", strategy, url);
		}

		switch(strategy)
		{
			case AGENDA31:
			{
				return refine("Agenda31", bits[5]);
			}

			case BIT_ONE_IS_EPISODE_NUMBER:
			{
				return refine(bits[0], bits[1]);
			}

			case BIT_TWO_IS_EPISODE_NUMBER:
			{
				return refine(bits[0]+upperFirst(bits[1]), bits[2]);
			}

			case BIT_THREE_IS_EPISODE_NUMBER:
			{
				return refine(bits[0], bits[3]);
			}

			case FUSE_TWO:
			{
				return refine(bits[0]+upperFirst(bits[1]), bits[2]);
			}

			case GWO:
			{
				return refine("GNUWorldOrder", bits[5]);
			}

			case SEASON_2_EPISODE_4:
			{
				return refine(bits[0], "s"+bits[2]+"e"+bits[4]);
			}

			case TTT:
			{
				return refine("TTT", bits[2]);
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
		if (showName.equals("lv"))
		{
			return "LinuxVoice";
		}
		else
		if (showName.equals("glp"))
		{
			return "GoingLinux";
		}
		else
		if (showName.length()<=4)
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
		if (showName.equals("tllts"))
		{
			return "LinuxLink";
		}

		if (showName.endsWith("podcast"))
		{
			showName=showName.substring(0, showName.length()-7)+"Podcast";
		}

		return upperFirst(removeExtension(showName));
	}

	private
	String removeExtension(String showName)
	{
		final
		int period=showName.lastIndexOf('.');

		if (period>0)
		{
			return showName.substring(0, period);
		}
		else
		{
			return showName;
		}
	}

	private
	String upperFirst(String showName)
	{
		if (showName!=null && showName.length()>1)
		{
			return Character.toUpperCase(showName.charAt(0)) + showName.substring(1);
		}
		else
		{
			return showName;
		}
	}

	private
	String refineEpisodeNumber(String episode)
	{
		log.debug("refine episode '{}'", episode);

		while (!episode.isEmpty() && badNumericPrefix(episode.charAt(0)))
		{
			episode=episode.substring(1);
		}

		log.debug("... to: '{}'", episode);
		return episode;
	}

	private
	boolean badNumericPrefix(char c)
	{
		return c=='-' || c=='0' || c=='_';
	}

	private
	Strategy getStrategy(String s, String[] bits)
	{
		if (s.startsWith("T3-"))
		{
			return Strategy.TTT;
		}

		if (s.startsWith("Agenda31"))
		{
			return Strategy.AGENDA31;
		}

		if (s.startsWith("gnuWorldOrder"))
		{
			return Strategy.GWO;
		}

		//if (firstBit.equals("Dudmanovi"))
		if (isNumeric(bits[1]) || bits.length<2)
		{
			return Strategy.BIT_ONE_IS_EPISODE_NUMBER;
		}

		if (bits[1].equals("_s"))
		{
			return Strategy.SEASON_2_EPISODE_4;
		}

		if (fuseMarker(bits[2]))
		{
			return Strategy.FUSE_TWO;
		}

		if (isNumeric(bits[2]))
		{
			return Strategy.BIT_TWO_IS_EPISODE_NUMBER;
		}

		if (isNumeric(bits[3]))
		{
			return Strategy.BIT_THREE_IS_EPISODE_NUMBER;
		}

		log.warn("no obvious strategy for: '{}'", s);
		return Strategy.BIT_ONE_IS_EPISODE_NUMBER;
	}

	private
	boolean fuseMarker(String bit)
	{
		return bit.equals("Luddites")
			|| bit.equals("Panic")
			|| bit.equals("Podcast")
			|| bit.equals("Show")
			;
	}

	private
	boolean isNumeric(String bit)
	{
		if (bit.length()==1)
		{
			//NB: don't want to match separators...
			return Character.isDigit(bit.charAt(0));
		}

		for (char c : bit.toCharArray())
		{
			if (!isNumericOrSeperator(c))
			{
				return false;
			}
		}

		return bit.length()>0;
	}

	private
	boolean isNumericOrSeperator(char c)
	{
		return Character.isDigit(c) || c=='-' || c=='_';
	}

	private
	enum Strategy
	{
		TTT, AGENDA31, BIT_TWO_IS_EPISODE_NUMBER, FUSE_TWO, GWO, SEASON_2_EPISODE_4, BIT_THREE_IS_EPISODE_NUMBER, BIT_ONE_IS_EPISODE_NUMBER
	}
}
