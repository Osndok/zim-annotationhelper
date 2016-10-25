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
		String withoutPathOrFileExt;
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

			withoutPathOrFileExt=url.substring(beginIndex, endIndex);
		}

		final
		String[] bits = withoutPathOrFileExt.replaceAll(BREAK_MATCHING_REGEX, " ").split(" ");
		{
			log.debug("split into {} bits", bits.length);

			for(int i=0; i<bits.length; i++)
			{
				log.debug("bit[{}]: {}", i, bits[i]);
			}
		}

		final
		Strategy strategy = getStrategy(withoutPathOrFileExt, bits);
		{
			log.debug("{} for {}", strategy, url);
			this.lastStrategy=strategy;
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

			case USER_ERROR_DIAMOND_COLLECTION:
			{
				return refine("UE", "DC"+refineEpisodeNumber(bits[5]));
			}

			//CAREFUL! No unit test coverage ATM!
			case BEST_EFFORT:
			{
				final
				int firstWithNumber=firstWithNumber(bits);

				final
				int lastWithNumber=lastWithNumber(bits);

				if (firstWithNumber>1 && lastWithNumber >1)
				{
					final
					String show = join(bits, 0, firstWithNumber-1);

					final
					String episode = join(bits, firstWithNumber, lastWithNumber);

					return refine(show, episode);
				}
				else
				{
					return withoutPathOrFileExt;
				}
			}
		}

		return withoutPathOrFileExt;
	}

	private
	int firstWithNumber(String[] bits)
	{
		for (int i=0; i<bits.length; i++)
		{
			final
			String bit=bits[i];

			if (stringContainsDigit(bit))
			{
				return i;
			}
		}

		return -1;
	}

	private
	int lastWithNumber(String[] bits)
	{
		for (int i=bits.length-1; i>0; i--)
		{
			final
			String bit=bits[i];

			if (stringContainsDigit(bit))
			{
				return i;
			}
		}

		return -1;
	}

	private
	boolean stringContainsDigit(String bit)
	{
		for (char c : bit.toCharArray())
		{
			if (Character.isDigit(c))
			{
				return true;
			}
		}

		return false;
	}

	private
	String join(String[] bits, int start, int end)
	{
		final
		StringBuilder sb = new StringBuilder();
		{
			for (int i=start; i<=end; i++)
			{
				final
				String bit=bits[i];

				if (bit.charAt(0)=='-')
				{
					sb.append(bit.substring(1));
				}
				else
				{
					sb.append(bit);
				}

				if (sb.charAt(sb.length()-1)!='-')
				{
					sb.append('-');
				}
			}

			sb.deleteCharAt(sb.length()-1);
		}

		return sb.toString();
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

		if (s.startsWith("ue-Diamond-Collection"))
		{
			return Strategy.USER_ERROR_DIAMOND_COLLECTION;
		}

		//----------- begin generic (wide-net) reasoning -------------

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
		return Strategy.BEST_EFFORT;
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
	Strategy lastStrategy;

	public
	boolean lastStrategyWasBestEffort()
	{
		return lastStrategy== Strategy.BEST_EFFORT;
	}

	private
	enum Strategy
	{
		TTT,
		AGENDA31,
		BIT_ONE_IS_EPISODE_NUMBER,
		BIT_TWO_IS_EPISODE_NUMBER,
		BIT_THREE_IS_EPISODE_NUMBER,
		FUSE_TWO,
		GWO,
		SEASON_2_EPISODE_4,
		USER_ERROR_DIAMOND_COLLECTION,
		BEST_EFFORT,
	}
}
