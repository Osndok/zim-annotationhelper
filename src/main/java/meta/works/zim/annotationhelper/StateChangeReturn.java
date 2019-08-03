package meta.works.zim.annotationhelper;

/**
 * Created by robert on 2017-02-01 14:57.
 */
public
class StateChangeReturn
{
	private
	boolean initialTimeCodeSuppressed;

	public
	boolean isInitialTimeCodeSuppressed()
	{
		return initialTimeCodeSuppressed;
	}

	public
	StateChangeReturn withInitialTimeCodeSuppressed()
	{
		this.initialTimeCodeSuppressed = true;
		return this;
	}
}
