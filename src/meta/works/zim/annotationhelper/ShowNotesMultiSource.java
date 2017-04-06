package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by robert on 2017-01-06 12:43.
 */
public
class ShowNotesMultiSource extends AbstractShowNotesSource
{
	private final
	List<AbstractShowNotesSource> implementations= Arrays.asList(
		new ShowNotesBasicURLSource(),
		new ShowNotesJupiterBroadcasting(),
		new ShowNotesUbuntuPodcast()
	);

	private static final
	Logger log = LoggerFactory.getLogger(ShowNotesMultiSource.class);

	@Override
	public
	String getShowNotesURL(String enclosingZimPage, String episodeIdentifier)
	{
		for (AbstractShowNotesSource implementation : this.implementations)
		{
			final
			String answer = implementation.getShowNotesURL(enclosingZimPage, episodeIdentifier);

			if (answer!=null)
			{
				return answer;
			}
		}

		log.debug("no answer for: '{}' / '{}'", enclosingZimPage, episodeIdentifier);
		return null;
	}
}
