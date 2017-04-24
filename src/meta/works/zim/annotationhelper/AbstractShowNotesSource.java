package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLConnection;
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

				if (bits.length < 2)
				{
					log.debug("not enough segments '{}'", zimPageName);
					return null;
				}
				else
				{
					final
					String showName = bits[bits.length - 2];

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
