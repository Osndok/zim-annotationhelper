package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by robert on 2017-01-06 11:48.
 */
public abstract
class AbstractShowNotesSource implements ShowNotesSource
{
	private static final
	Logger log = LoggerFactory.getLogger(AbstractShowNotesSource.class);

	protected
	boolean acceptUnparsedZimPageName(String zimPageName)
	{
		return true;
	}

	int showNotesUrlNumCalls=0;

	@Override
	public final
	int getShowNotesUrlNumCalls()
	{
		return showNotesUrlNumCalls;
	}

	int unsafeShowNotesUrlNumCalls=0;

	@Override
	public final
	int getUnsafeShowNotesUrlNumCalls()
	{
		return unsafeShowNotesUrlNumCalls;
	}

	@Override
	public final
	String getShowNotesURL(String zimPageName)
	{
		showNotesUrlNumCalls++;

		if (zimPageName==null)
		{
			log.debug("no zim page");
			return null;
		}
		else
		if (acceptUnparsedZimPageName(zimPageName))
		{
			log.debug("{} WILL attempt: '{}'", this, zimPageName);

			try
			{
				final
				String[] bits = zimPageName.split(":");
				{
					log.debug("'{}' split into {} segments", zimPageName, bits.length);
				}

				if (bits.length < 2)
				{
					return null;
				}

				final
				int year=findYear(bits);

				if (year>0)
				{
					//NB: If it looks wrong, that's because it is... the 1 is to avoid the ':' prefix (a shortcut?)
					final
					String showName = String.join(":", Arrays.copyOfRange(bits, 1, year));

					final
					String episodeNumber = String.join(":", Arrays.copyOfRange(bits, year, bits.length));

					log.debug("year-split '{}' -> '{}' / '{}'", zimPageName, showName, episodeNumber);

					return unsafe_getShowNotesURL(showName, episodeNumber);
				}
				else
				{
					final
					String showName;
					{
						//NB: the first bit is usually empty; ":A:B" -> {"", "A", "B"}
						if (bits.length==3)
						{
							showName=bits[1];
						}
						else
						if (bits.length==4)
						{
							showName=bits[1]+":"+bits[2];
						}
						else
						{
							showName=bits[bits.length - 2];
						}
					}

					final
					String episodeNumber = bits[bits.length - 1];

					log.debug("extracting '{}' -> '{}' / '{}'", zimPageName, showName, episodeNumber);
					return unsafe_getShowNotesURL(showName, episodeNumber);
				}
			}
			catch (Exception e)
			{
				log.error("caught", e);
				return null;
			}
		}
		else
		{
			log.debug("{} refuses to attempt: '{}'", this, zimPageName);
			return null;
		}
	}

	private
	int findYear(String[] bits)
	{
		for(int i=0; i<bits.length; i++)
		{
			final
			String bit=bits[i];

			if (bit.length()==4)
			{
				if (bit.startsWith("19") || bit.startsWith("20"))
				{
					final
					char c2=bit.charAt(2);

					final
					char c3=bit.charAt(3);

					if (Character.isDigit(c2) && Character.isDigit(c3))
					{
						return i;
					}
				}
			}
		}

		return 0;
	}

	protected
	String fetchUrlContent(String urlString)
	{
		try
		{
			final
			URL url=new URL(urlString);

			final
			URLConnection urlConnection = url.openConnection();
			{
				//NB: This is not vanity... the ubuntu podcast wiki refuses the default user-agent.
				urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
			}

			final
			Scanner scanner = new Scanner(urlConnection.getInputStream(), "UTF-8");

			try
			{
				scanner.useDelimiter("\\A");
				return scanner.next();
			}
			finally
			{
				scanner.close();
			}
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
