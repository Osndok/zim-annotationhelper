package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by robert on 2017-01-06 11:48.
 */
public abstract
class AbstractShowNotesSource implements ShowNotesSource
{
	private static final
	Logger log = LoggerFactory.getLogger(AbstractShowNotesSource.class);

	@Override
	public final
	String getShowNotesURL(String zimPageName)
	{
		if (zimPageName==null)
		{
			log.debug("no zim page");
			return null;
		}
		else
		{
			final
			String[] bits = zimPageName.split(":");

			if (bits.length<2)
			{
				log.debug("not enough segments '{}'", zimPageName);
				return null;
			}
			else
			{
				final
				String showName=bits[bits.length - 2];

				final
				String episodeNumber=bits[bits.length - 1];

				log.debug("extracting '{}' -> '{}' / '{}'", zimPageName, showName, episodeNumber);
				return getShowNotesURL(showName, episodeNumber);
			}
		}
	}
}
