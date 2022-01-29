package meta.works.zim.annotationhelper;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public
class RhythmBoxMediaPlayerTest
{
	private
	RhythmBoxMediaPlayer rbmp;

	@BeforeMethod
	public
	void setUp() throws Exception
	{
		rbmp=new RhythmBoxMediaPlayer();
	}

	@Test
	public
	void testReduceTimeCodeLine() throws Exception
	{
		assertEquals(rbmp.reduceTimeCodeLine("+ (00:24:02) - This is a blurb."),  "24:02 - This is a blurb.");
		assertEquals(rbmp.reduceTimeCodeLine("+ (01:24:02) - This is a blurb."),  "1:24:02 - This is a blurb.");
		assertEquals(rbmp.reduceTimeCodeLine("+ (10:24:02) - This is a blurb."),  "10:24:02 - This is a blurb.");
	}

}