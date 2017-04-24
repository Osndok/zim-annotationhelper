package meta.works.zim.annotationhelper;

/**
 * Created by robert on 2017-01-06 11:37.
 */
public
interface ShowNotesSource
{
	int getShowNotesUrlNumCalls();
	String getShowNotesURL(String zimPageName);

	int getUnsafeShowNotesUrlNumCalls();

	/*
	NOT PROTECTED from the accept filter & try/catch wrapper
	USE ONLY FOR TESTING SPECIFIC IMPLEMENTATIONS...
	TODO: remove this from the interface altogether?!?
	 */
	@Deprecated
	String unsafe_getShowNotesURL(String enclosingZimPage, String episodeIdentifier);
}
