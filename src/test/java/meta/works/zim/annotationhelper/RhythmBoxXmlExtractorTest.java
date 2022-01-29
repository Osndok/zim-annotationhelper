package meta.works.zim.annotationhelper;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

import static org.testng.Assert.*;

public
class RhythmBoxXmlExtractorTest
{
	private
	RhythmBoxXmlExtractor rhythmBoxXmlExtractor;

	@BeforeMethod
	public
	void setUp() throws Exception
	{
		this.rhythmBoxXmlExtractor=new RhythmBoxXmlExtractor(new File("test/rhythmdb.xml"));
	}

	@Test
	public
	void testGetDescriptionFor() throws Exception
	{
		d("relative/path/for/AllJupiterVideos", "Jupiter Broadcasting Videos");
		d(
			"/absolute/An_Americans_Guide_to_Brazilian_Libertarianism_Ep._39.mp3",
			"What if I told you that there is one country in the world where \"Mises\" garners more google searches than \"Marx?\" Which country would you guess? The United States? Switzerland? New Zealand? WRONG. It's BRAZIL. In this episode, Liberty Weekly listener (and native Brazilian) Dallas Ferraz primes us on the Brazilian Liberty Movement."
		);
	}

	@Test
	public
	void testEntities() throws Exception
	{
		final
		String description = this.rhythmBoxXmlExtractor.getDescriptionFor(new File("LNL27.ogg"));

		// NB: Smart quote!
		assertTrue(description.startsWith("It’s been a year of Late Night Linux!"));
		assertTrue(description.endsWith("February Arch and Tails announced that they... Read More"));
	}

	private
	void d(String fileName, String expectedDescription) throws IOException, XMLStreamException
	{
		final
		String description = this.rhythmBoxXmlExtractor.getDescriptionFor(new File(fileName));

		assertEquals(description, expectedDescription);
	}

}