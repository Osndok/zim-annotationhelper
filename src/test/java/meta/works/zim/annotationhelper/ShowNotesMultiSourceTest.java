package meta.works.zim.annotationhelper;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by robert on 2017-04-24 11:05.
 */
public
class ShowNotesMultiSourceTest
{
	private
	ShowNotesMultiSource multiSource;

	@BeforeMethod
	public
	void setUp() throws Exception
	{
		this.multiSource=new ShowNotesMultiSource();
	}

	@Test
	public
	void testGetImplementation() throws Exception
	{
		final
		ShowNotesSource implementation = multiSource.getImplementation(ShowNotesJupiterBroadcasting.class);

		assertNotNull(implementation);
		assertEquals(implementation.getClass(), ShowNotesJupiterBroadcasting.class);
	}

	@Test(enabled = false)
	public
	void testShouldNotCall() throws Exception
	{
		final
		ShowNotesSource ubuntu=multiSource.getImplementation(ShowNotesUbuntuPodcast.class);

		int starting=ubuntu.getUnsafeShowNotesUrlNumCalls();

		final
		String showNotesURL = multiSource.getShowNotesURL(":SMLR:232");

		int ending=ubuntu.getUnsafeShowNotesUrlNumCalls();

		assertEquals(starting, ending);
	}

	@Test
	public
	void testShouldNotCall2() throws Exception
	{
		final
		ShowNotesSource ubuntu=multiSource.getImplementation(ShowNotesUbuntuPodcast.class);

		int starting=ubuntu.getUnsafeShowNotesUrlNumCalls();

		final
		String showNotesURL = multiSource.getShowNotesURL(":FreeTalk:Digest:2017:04:10");

		int ending=ubuntu.getUnsafeShowNotesUrlNumCalls();

		assertEquals(starting, ending);
	}
}