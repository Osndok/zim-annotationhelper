package meta.works.zim.annotationhelper;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by robert on 2019-03-30 10:54.
 */
public
class CallInRecordTest
{
	@Test
	public
	void testBasicNoName()
	{
		final
		CallInRecord cir=new CallInRecord("1553887817;+14328971283;+14328971283");

		assertEquals(cir.getDate().getTime(), 1553887817000l);
		assertEquals(cir.getRawNumber(), "+14328971283");
		assertNull(cir.getContactName());
		assertFalse(cir.hasContactName());
	}

	@Test
	public
	void testBasicWithName()
	{
		final
		CallInRecord cir=new CallInRecord("1553887817;+14328971283;Bob Jones");

		assertEquals(cir.getDate().getTime(), 1553887817000l);
		assertEquals(cir.getRawNumber(), "+14328971283");
		assertEquals(cir.getContactName(), "Bob Jones");
		assertTrue(cir.hasContactName());
	}

	@Test
	public
	void testStripPlusOnePrefix()
	{
		assertEquals(CallInRecord.stripPlusOnePrefix("411"), "411");
		assertEquals(CallInRecord.stripPlusOnePrefix("5551234"), "5551234");
		assertEquals(CallInRecord.stripPlusOnePrefix("+15551234"), "5551234");
	}

	@Test
	public
	void testGetZimPageName()
	{
		CallInRecord cir=new CallInRecord("1553887817;+14328971283;Bob Jones");
		assertEquals(cir.getZimPageName(), ":Phone:Number:432:897:1283");

		cir=new CallInRecord("1553887817;411;Information");
		assertEquals(cir.getZimPageName(), ":Phone:Number:411");

		cir=new CallInRecord("1553887817;2863;Short Code");
		assertEquals(cir.getZimPageName(), ":Phone:Number:2863");
	}

	@Test
	public
	void testGetZimPageLink()
	{
		CallInRecord cir=new CallInRecord("1553887817;+14328971283;Bob Jones");
		assertEquals(cir.getZimPageLink("Blather"), "[[:Phone:Number:432:897:1283:Blather|Bob Jones]]");

		cir=new CallInRecord("1553887817;+14328971283;+14328971283");
		assertEquals(cir.getZimPageLink("Text"), "[[:Phone:Number:432:897:1283:Text|432-897-1283]]");
	}
}