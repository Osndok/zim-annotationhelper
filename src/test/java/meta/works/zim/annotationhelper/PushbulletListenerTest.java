package meta.works.zim.annotationhelper;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by robert on 2018-12-24 09:02.
 */
public
class PushbulletListenerTest
{
	@Test
	public
	void testSanitizeForZimPageName()
	{
		assertEquals(a(" * John ///Doe /+- "), ":JohnDoe:TextMessage");
		assertEquals(a("5555"), ":Phone:Number:5555");
		assertEquals(a("987-555-1234"), ":Phone:Number:987:5551234");
	}

	private
	String a(String input)
	{
		return PushbulletListener.sanitizeForZimPageName(input);
	}
}