package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static meta.works.zim.annotationhelper.ZimPageNameStrategy.*;

/**
 * Given a URL or file-name, return/compute the Zim page name therefor.
 */
public
class ZimPageNameExtractor
{
	private static final
	Logger log = LoggerFactory.getLogger(ZimPageNameExtractor.class);

	private final
	boolean debug;

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

	private
	String kludge_lastSeenUrl;

	public
	ZimPageNameExtractor(boolean debug)
	{
		this.debug = debug;
	}

	public
	String getZimPageNameFor(String url)
	{
		try
		{
			url = URLDecoder.decode(url, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			log.error("bad url?", e);
		}

		final
		String superBasename;

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

					final
					int secondToLastSlash=url.lastIndexOf('/', slash-1);

					if (secondToLastSlash>=0)
					{
						superBasename=url.substring(secondToLastSlash+1, slash);
					}
					else
					{
						superBasename=null;
					}
				}
				else
				{
					beginIndex=0;
					superBasename=null;
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
		List<String> bitsList = removeHazards(withoutPathOrFileExt.replaceAll(BREAK_MATCHING_REGEX, " ").split(" "));
		{
			log.debug("split into {} bits", bitsList.size());

			if (!withoutPathOrFileExt.equals(kludge_lastSeenUrl))
			{
				if (debug)
				{
					for (int i = 0; i < bitsList.size(); i++)
					{
						log.info("bit[{}]: {}", i, bitsList.get(i));
					}
				}

				kludge_lastSeenUrl=withoutPathOrFileExt;
			}
		}

		final
		String[] bits=bitsList.toArray(new String[bitsList.size()]);

		final
		int last=bits.length-1;

		final
		ZimPageNameStrategy strategy = getStrategy(url, withoutPathOrFileExt, bits);
		{
			if (debug)
			{
				log.info("'{}' strategy for {}", strategy, url);
			}
			else
			{
				log.debug("'{}' strategy for {}", strategy, url);
			}
			this.lastStrategy=strategy;
		}

		switch(strategy)
		{
			case AGENDA31:
			{
				return refine("Agenda31", bits[5]);
			}

			case BillBurr:
			{
				final
				String minusYear=bits[3];

				final
				String minusMonth=bits[1];

				final
				String minusDate=bits[2];

				final
				String year="20"+minusYear.substring(1);

				final
				String date;
				{
					final
					StringBuilder sb=new StringBuilder();
					{
						sb.append(year);

						if (minusMonth.length()==2)
						{
							sb.append(':');
							sb.append(minusMonth);
							sb.replace(5, 6, "0");
						}
						else
						{
							sb.append(minusMonth);
							sb.replace(4, 5, ":");
						}

						if (minusDate.length()==2)
						{
							sb.append(':');
							sb.append(minusDate);
							sb.replace(8, 9, "0");
						}
						else
						{
							sb.append(minusDate);
							sb.replace(7, 8, ":");
						}
					}

					date=sb.toString();
				}

				return refine("BillBurr:"+bits[0], date);
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
				if (bits[2].equals("E"))
				{
					//"E" is for... "EPISODE!"
					return refine(bits[0], bits[3]);
				}
				else
				{
					return refine(bits[0] + bits[1] + ":" + bits[2], bits[3]);
				}
			}

			case FTL:
			{
				if (withoutPathOrFileExt.contains("Digest"))
				{
					final
					String year;

					final
					String minusMonth;

					final
					String minusDate;

					if (bits[1].equals("Digest"))
					{
						//no spaces
						if (bits.length>=5)
						{
							//the common case
							year = bits[2];
							minusMonth = bits[3];
							minusDate = bits[4];
						}
						else
						if (bits.length == 3)
						{
							//No separators?
							String yyyymmdd=bits[2];
							year=yyyymmdd.substring(0,4);
							minusMonth="-"+yyyymmdd.substring(4,6);
							minusDate="-"+yyyymmdd.substring(6,8);

						}
						else
						{
							//they tried to type a '-' and hit a '0'...
							year = bits[2];
							minusMonth=bits[3].substring(0,3);
							minusDate=bits[3].substring(3);
						}
					}
					else
					{
						//with spaces (generates empty segments)
						year=bits[5];
						minusMonth=bits[6];
						minusDate=bits[7];
					}

					final
					String date;
					{
						final
						StringBuilder sb=new StringBuilder();
						{
							sb.append(year);
							sb.append(minusMonth);
							sb.append(minusDate);
							sb.replace(4, 5, ":");
							if (minusMonth.length()==2)
							{
								sb.insert(5, '0');
							}
							sb.replace(7, 8, ":");
							if (minusDate.length()==2)
							{
								sb.insert(8, '0');
							}
						}

						date=sb.toString();
					}
					return refine("FreeTalk:Digest", date);
				}
				else
				{
					final
					String year=bits[1];

					final
					String minusMonth=bits[2];

					final
					String minusDate=bits[3];

					final
					String date;
					{
						final
						StringBuilder sb=new StringBuilder();
						{
							sb.append(year);
							sb.append(minusMonth);
							sb.append(minusDate);
							sb.replace(4, 5, ":");
							sb.replace(7, 8, ":");
						}

						date=sb.toString();
					}

					return refine("FreeTalk:Live", date);
				}
			}

			case FUSE_TWO:
			{
				return refine(bits[0]+upperFirst(bits[1]), bits[2]);
			}

			case GWO:
			{
				final
				String season;
				{
					if (bits[3].equals("_"))
					{
						season=bits[4].substring(0, bits[4].length()-1);
					}
					else
					{
						season = bits[3];
					}
				}

				return refine("Podcast:GnuWorldOrder:"+season, bits[5]);
			}

			case JORDAN_PETERSON:
			{
				return refine("JordanPeterson:Podcast", firstNumeric(bits));
			}

			case LIBERTARIAN_CHRISTIAN:
			{
				return refine("Podcast:LibertarianChristian", refineEpisodeNumber(bits[1]));
			}

			case LIBERTY_WEEKLY:
			{
				final
				int ep=firstMatching(bits, "Ep");

				if (ep>=0)
				{
					return refine("Podcast:LibertyWeekly:Episode", refineEpisodeNumber(bits[ep+1]));
				}

				if (bits[0].equals("LL"))
				{
					return refine("Podcast:LibertyWeekly:LiberationLibrary", refineEpisodeNumber(bits[1]));
				}

				final
				int library=firstMatching(bits, "Library");

				if (library>0)
				{
					return refine("Podcast:LibertyWeekly:LiberationLibrary", refineEpisodeNumber(bits[library+1]));
				}

				return null;
			}

			case MartinHash:
			{
				return refine("MartinHash", bits[3]);
			}

			case SEASON_2_EPISODE_4:
			{
				return refine(bits[0], "s"+bits[2]+":e"+bits[4]);
			}

			case TTT:
			{
				return refine("TTT", bits[2]);
			}

			case USER_ERROR_DIAMOND_COLLECTION:
			{
				return refine("UE", "DC"+refineEpisodeNumber(bits[5]));
			}

			case CSWDC:
			{
				return refine("CommonSenseWithDanCarlin", getCswdcEpisodeNumber(withoutPathOrFileExt));
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

			case UUID:
			{
				String podcast=removeSpacesAddPodcastPrefix(superBasename);
				String bit0=withoutPathOrFileExt.substring(0, 2);
				String bit1=withoutPathOrFileExt.substring(2, 4);
				return String.format(":%s:%s:%s", podcast, bit0, bit1);
			}

			case ANARCHOCHRISTIAN:
			{
				return refine("Podcast:AnarchoChristian", bits[8]);
			}

			default:
				throw new UnsupportedOperationException(strategy.toString());
		}

		//return withoutPathOrFileExt;
	}

	private
	String removeSpacesAddPodcastPrefix(String englishy)
	{
		final String reduced = englishy.replace(" ", "");
		return "Podcast:"+reduced;
	}

	private
	int firstMatching(String[] bits, String token)
	{
		for (int i=0; i<bits.length; i++)
		{
			if (token.equalsIgnoreCase(bits[i]))
			{
				return i;
			}
		}

		return -1;
	}

	private static final
	Collection<String> HAZARDS=Arrays.asList("", "\t");

	private
	List<String> removeHazards(String[] bits)
	{
		final
		List<String> retval=new ArrayList<>(Arrays.asList(bits));
		{
			retval.removeAll(HAZARDS);
		}

		return retval;
	}

	private
	String getCswdcEpisodeNumber(String baseName)
	{
		//"cswdcd12.mp3"
		// 012345678
		char hundredsCode=baseName.charAt(5);
		int hundreds=(hundredsCode-'a');
		String subHundreds=baseName.substring(6,8);
		return hundreds+subHundreds;
	}

	private
	String firstNumeric(String[] bits)
	{
		for (String bit : bits)
		{
			if (isNumeric(bit))
			{
				return bit;
			}
		}

		return null;
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

				if (bit.isEmpty())
				{
					continue;
				}

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
		log.debug("refine('{}', '{}')", showName, episode);

		return String.format(
			":%s:%s",
			refineShowName(showName),
			refineEpisodeNumber(episode)
		);
	}

    private static final
    Map<String,String> SHOW_NAME_MAP;

    static
    {
        final
        Map<String,String> a=new HashMap<>();
        {
            a.put("lv", "LinuxVoice");
            a.put("glp", "GoingLinux");
            a.put("tri", "Triangulation");
            a.put("ttg", "TechGuy");
            a.put("techsnap", "TechSNAP");
            a.put("tllts", "LinuxLink");
			a.put("lan", "LinuxAction:News");
			a.put("asknoah", "AskNoah");
        }

        SHOW_NAME_MAP=Collections.unmodifiableMap(a);
    }

	private
	String refineShowName(String showName)
	{
        //Do we have a pre-set name?
        {
            final
            String preset=SHOW_NAME_MAP.get(showName);

            if (preset!=null)
            {
                log.debug("preset: '{}' -> '{}'", showName, preset);
                return preset;
            }
        }

		if (showName.length()<=4)
		{
			return showName.toUpperCase();
		}
		else
		if (showName.startsWith("linuxaction"))
		{
			return "LAS";
		}

		if (showName.endsWith("podcast"))
		{
			showName=showName.substring(0, showName.length()-7)+"Podcast";
		}

		return collapseSeparators(upperFirst(removeExtension(showName)));
	}

	private
	String collapseSeparators(String s)
	{
		final
		StringBuilder sb=new StringBuilder(s);

		int i=0;

		while (i<sb.length())
		{
			if (isSeparator(sb.charAt(i)))
			{
				sb.deleteCharAt(i);
			}
			else
			{
				i++;
			}
		}

		if (sb.length()>1)
		{
			return sb.toString();
		}
		else
		{
			return s;
		}
	}

	private
	boolean isSeparator(char c)
	{
		return c=='_' || c=='-';
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

	//clean-up, nicify...
	private
	String refineEpisodeNumber(String episode)
	{
		log.debug("refine episode '{}'", episode);

		if (episode.isEmpty())
		{
			return "Other";
		}

		final
		StringBuilder sb=new StringBuilder(episode);

		char firstChar;//='0';

		//TRIM FROM THE FRONT
		do
		{
			firstChar=sb.charAt(0);

			if (badNumericPrefix(firstChar))
			{
				//Trim off leading zeros, or the like...
				sb.deleteCharAt(0);
			}
			else
			{
				break;
			}
		}
		while (sb.length()>0);

		//TRUNCATE OTHER NUMBERS & IDENTIFIERS
		{
			for (int i=1; i<sb.length(); i++)
			{
				final
				char c = sb.charAt(i);

				if (isSeparator(c))
				{
					sb.delete(i, sb.length());
					break;
				}
			}
		}

		episode=sb.toString();

		log.debug("... to: '{}'", episode);

		if (episode.isEmpty())
		{
			//Opps... I never suspected an episode zero before... :LNL:0 !?!?
			return String.valueOf(firstChar);
		}

		if (episode.length()==8 && episode.startsWith("20"))
		{
			//Break the year into it's own 'folder'...
			return episode.substring(0,4)+":"+episode.substring(4);
		}
		else
		{
			return episode;
		}
	}

	private
	boolean badNumericPrefix(char c)
	{
		return c=='-' || c=='0' || c=='_' || c=='.';
	}

	private
	ZimPageNameStrategy getStrategy(String url, String s, String[] bits)
	{
		log.debug("getStrategy('{}', '{}', ...)", url, s);

		if (looksLikeUUID(s))
		{
			return UUID;
		}

		//-----needs deep context... can't extract from basic filename---------

		if (url.contains("/AnarchoChristian"))
		{
			return ANARCHOCHRISTIAN;
		}

		if (url.contains("/Podcasts/PRay"))
		{
			return MartinHash;
		}

		if (url.contains("Libert"))
		{
			//NB: 'weekly' is more distinct/rare than 'christian' so it comes first.
			if (url.contains("Weekly"))
			{
				return LIBERTY_WEEKLY;
			}

			if (url.contains("Christian"))
			{
				return LIBERTARIAN_CHRISTIAN;
			}
		}

		if (url.contains("Peterson Podcast"))
		{
			return JORDAN_PETERSON;
		}

		//-----------------------------------------

		if (s.startsWith("FTL"))
		{
			return FTL;
		}

		if (s.startsWith("T3-"))
		{
			return TTT;
		}

		if (s.startsWith("Agenda31"))
		{
			return AGENDA31;
		}

		if (s.startsWith("gnuWorldOrder"))
		{
			return GWO;
		}

		if (s.startsWith("ue-Diamond-Collection"))
		{
			return USER_ERROR_DIAMOND_COLLECTION;
		}

		if (s.startsWith("cswdc"))
		{
			return CSWDC;
		}

		if (s.startsWith("TAMMP") || s.startsWith("MMPC"))
		{
			return BillBurr;
		}

		//----------- begin generic (wide-net) reasoning -------------

		if (isNumeric(bits[1]) || bits.length<2)
		{
			return BIT_ONE_IS_EPISODE_NUMBER;
		}

		if (bits.length>4 && isNumeric(bits[3]))
		{
			return BIT_THREE_IS_EPISODE_NUMBER;
		}

		if (bits[1].equals("_s"))
		{
			return SEASON_2_EPISODE_4;
		}

		if (fuseMarker(bits[2]))
		{
			return FUSE_TWO;
		}

		if (isNumeric(bits[2]))
		{
			return BIT_TWO_IS_EPISODE_NUMBER;
		}

		if (isNumeric(bits[3]))
		{
			return BIT_THREE_IS_EPISODE_NUMBER;
		}

		log.warn("no obvious strategy for: '{}'", s);
		return BEST_EFFORT;
	}

	private
	boolean looksLikeUUID(String s)
	{
		if (s.length()!=36)
		{
			log.debug("not a UUID, length={}: '{}'", s.length(), s);
			return false;
		}

		return s.charAt(8)=='-' && s.charAt(13)=='-' && s.charAt(18)=='-';
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
			if (!isNumericOrSeparator(c))
			{
				return false;
			}
		}

		return bit.length()>0;
	}

	private
	boolean isNumericOrSeparator(char c)
	{
		return Character.isDigit(c) || c=='-' || c=='_';
	}

	private
	ZimPageNameStrategy lastStrategy;

}
