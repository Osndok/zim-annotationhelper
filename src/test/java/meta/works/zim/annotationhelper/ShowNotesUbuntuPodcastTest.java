package meta.works.zim.annotationhelper;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by robert on 2017-04-06 17:06.
 */
public
class ShowNotesUbuntuPodcastTest
{
	private
	ShowNotesSource showNotesSource;

	@BeforeMethod
	public
	void setUp() throws Exception
	{
		showNotesSource = new ShowNotesUbuntuPodcast();
	}

	@DataProvider(name = "zimPageShowNotesPairs")
	public
	Object[][] createData1()
	{
		//NB: These will soon SUNSET behind the pagination boundary; but for our use-case,
		return new Object[][]
			{
				{":UbuntuPodcast:s10:e05", "http://ubuntupodcast.org/2017/04/06/s10e05-supreme-luxuriant-gun/"},
			};
	}

	@Test(dataProvider = "zimPageShowNotesPairs", enabled = false, description = "rot")
	public
	void testGetUrlFor(String zimPageName, String expectedUrl) throws Exception
	{
		assertEquals(showNotesSource.getShowNotesURL(zimPageName), expectedUrl);
	}

	@Test(enabled = false, description = "rot")
	public
	void testHistoricPaginationRequired() throws Exception
	{
		final
		String showNotesURL = showNotesSource.getShowNotesURL(":UbuntuPodcast:s08:e37");

		assertEquals(showNotesURL, "http://ubuntupodcast.org/2015/11/19/s08e37-code-name-k-o-z/");
	}

	@Test
	public
	void testUnknownShowNameDoesNotThrow() throws Exception
	{
		final
		String showNotesURL = showNotesSource.getShowNotesURL(":Blather:1234");

		assertNull(showNotesURL);
	}
}
