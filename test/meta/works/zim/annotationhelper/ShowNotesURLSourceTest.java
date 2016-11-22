package meta.works.zim.annotationhelper;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by robert on 2016-11-22 11:41.
 */
public
class ShowNotesURLSourceTest
{
	private
	ShowNotesURLSource showNotesURLSource;

	@BeforeMethod
	public
	void setUp() throws Exception
	{
		showNotesURLSource=new ShowNotesURLSource();
	}

	@Test
	public
	void testGetShowNotesURL() throws Exception
	{
		t(":DWW:20161121", "https://distrowatch.com/weekly.php?issue=20161121");

		//NB: NA-582 is (experimentally) the first one that fits this pattern.
		t(":NA:581", null);
		t(":NA:582", "http://582.nashownotes.com");
		t(":NA:582b", "http://582.nashownotes.com");
		t(":NA:1234", "http://1234.nashownotes.com");
	}

	private
	void t(String zimPageName, String expectedShowNotesUrl)
	{
		final
		String actual=showNotesURLSource.getShowNotesURL(zimPageName);

		assertEquals(actual, expectedShowNotesUrl);
	}

}