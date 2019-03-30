package meta.works.zim.annotationhelper;

import java.util.Date;

/**
 * Created by robert on 2019-03-30 10:48.
 */
public
class CallInRecord
{
	final
	Date date;

	final
	String rawNumber;

	final
	String contactName;

	public
	CallInRecord(String s)
	{
		final
		int semiColonOne=s.indexOf(';');

		final
		int semiColonTwo=s.indexOf(';', semiColonOne+1);

		this.date=new Date(1000*Long.parseLong(s.substring(0, semiColonOne)));
		this.rawNumber=s.substring(semiColonOne+1, semiColonTwo);

		final
		String numberOrContactName=s.substring(semiColonTwo+1);

		if (numberOrContactName.equals(rawNumber))
		{
			this.contactName=null;
		}
		else
		{
			this.contactName=numberOrContactName;
		}
	}

	public
	Date getDate()
	{
		return date;
	}

	public
	String getRawNumber()
	{
		return rawNumber;
	}

	public
	String getContactName()
	{
		return contactName;
	}

	public
	boolean hasContactName()
	{
		return contactName!=null;
	}

	static
	String stripPlusOnePrefix(String s)
	{
		final
		StringBuilder sb=new StringBuilder(s);

		if (sb.charAt(0)=='+')
		{
			sb.delete(0,1);
		}

		if (sb.charAt(0)=='1')
		{
			sb.delete(0,1);
		}

		return sb.toString();
	}

	public
	String getZimPageName()
	{
		String s=stripPlusOnePrefix(rawNumber);

		if (s.length()==10)
		{
			String one=s.substring(0, 3);
			String two=s.substring(3, 6);
			String three=s.substring(6);
			return String.format(":Phone:Number:%s:%s:%s", one, two, three);
		}
		else
		{
			return String.format(":Phone:Number:%s", s);
		}
	}

	public
	String getZimPageLink(String subPage)
	{
		if (hasContactName())
		{
			return String.format("[[%s:%s|%s]]", getZimPageName(), subPage, contactName);
		}
		else
		{
			return String.format("[[%s:%s]]", getZimPageName(), subPage);
		}
	}
}
