package meta.works.zim.annotationhelper;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static meta.works.zim.annotationhelper.AbstractDBusMediaPlayer.zimPageNameExtractor;
import static org.testng.Assert.*;

/**
 * Created by robert on 2017-01-06 12:17.
 */
public
class ShowNotesJupiterBroadcastingTest
{
	private
	ShowNotesSource showNotesSource;

	@BeforeMethod
	public
	void setUp() throws Exception
	{
		showNotesSource = new ShowNotesJupiterBroadcasting();
	}

	@DataProvider(name = "zimPageShowNotesPairs")
	public
	Object[][] createData1()
	{
		//NB: These will soon SUNSET behind the pagination boundary; but for our use-case,
		return new Object[][]
			{
				{":TechSNAP:300", "http://www.jupiterbroadcasting.com/106026/2089-days-uptime-techsnap-300/"},
				{":CR:238:", "http://www.jupiterbroadcasting.com/106011/undockered-cr-238/"},
				{":BSD:175", "http://www.jupiterbroadcasting.com/105921/how-the-dtrace-saved-christmas-bsd-now-175/"},
				{":Unfilter:220", "http://www.jupiterbroadcasting.com/105906/reheating-cold-wars-unfilter-220/"},
				{":LUP:178", "http://www.jupiterbroadcasting.com/105886/big-sister-is-watching-lup-178/"},
				{":LAS:450", "http://www.jupiterbroadcasting.com/105836/winter-solus-review-las-450/"},
				{":UE:8", "http://www.jupiterbroadcasting.com/104351/snorting-apple-cider-user-error-8/"},
			};
	}

	@Test(dataProvider = "zimPageShowNotesPairs")
	public
	void testGetUrlFor(String zimPageName, String expectedUrl) throws Exception
	{
		assertEquals(showNotesSource.getShowNotesURL(zimPageName), expectedUrl);
	}

	@Test
	public
	void testHistoricPaginationRequired() throws Exception
	{
		final
		String showNotesURL = showNotesSource.getShowNotesURL(":Unfilter:184");

		assertEquals(showNotesURL, "http://www.jupiterbroadcasting.com/98841/clearly-rigged-unfilter-184/");
	}

	@Test
	public
	void testUnknownShowNameDoesNotThrow() throws Exception
	{
		final
		String showNotesURL = showNotesSource.getShowNotesURL("Blather", "1234");

		assertNull(showNotesURL);
	}
}
