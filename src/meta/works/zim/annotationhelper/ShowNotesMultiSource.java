package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by robert on 2017-01-06 12:43.
 */
public
class ShowNotesMultiSource implements ShowNotesSource
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
	String getShowNotesURL(String zimPageName)
	{
		for (AbstractShowNotesSource implementation : implementations)
		{
			final
			String answer=implementation.getShowNotesURL(zimPageName);

			if (answer==null)
			{
				log.debug("{} confounds {}", zimPageName, implementation);
			}
			else
			{
				log.info("{} is answered by {}", zimPageName, implementation);
				return answer;
			}
		}

		log.info("{} has no acceptable ShowNotesSource", zimPageName);
		return null;
	}

	@Override
	public
	String unsafe_getShowNotesURL(String enclosingZimPage, String episodeIdentifier)
	{
		throw new AssertionError();
	}

	@Override
	public
	int getShowNotesUrlNumCalls()
	{
		int i=0;

		for (AbstractShowNotesSource implementation : implementations)
		{
			i+=implementation.getShowNotesUrlNumCalls();
		}

		return i;
	}

	@Override
	public
	int getUnsafeShowNotesUrlNumCalls()
	{
		int i = 0;

		for (AbstractShowNotesSource implementation : implementations)
		{
			i += implementation.getUnsafeShowNotesUrlNumCalls();
		}

		return i;
	}

	public
	ShowNotesSource getImplementation(Class<? extends ShowNotesSource> c)
	{
		for (AbstractShowNotesSource implementation : implementations)
		{
			if (c.isAssignableFrom(implementation.getClass()))
			{
				return implementation;
			}
		}

		return null;
	}
}
