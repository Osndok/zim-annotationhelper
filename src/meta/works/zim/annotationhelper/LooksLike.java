package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by robert on 2016-10-06 15:18.
 */
public
class LooksLike
{
	private static final
	Logger log = LoggerFactory.getLogger(LooksLike.class);

	public static
	boolean videoFile(String url)
	{
		if (url==null || url.length()<5)
		{
			return false;
		}

		final
		boolean retval=url.endsWith(".mp4");

		final
		char lastChar=url.charAt(url.length()-1);

		log.debug("videoFile('{}')? {} (lastChar='{}')", url, retval, lastChar);

		return retval;
	}
}
