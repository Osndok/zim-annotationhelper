package meta.works.zim.annotationhelper;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by robert on 2017-06-21 01:02.
 */
public
class StashFileTest
{
	@Test
	public
	void testBasicStorage() throws Exception
	{
		final
		StashFile stash = StashFile.getTestingStash();

		assertNull(stash.getLastPlayTime());

		stash.setLastPlayTime(1234);

		assertEquals(stash.getLastPlayTime(), new Long(1234));
	}
}