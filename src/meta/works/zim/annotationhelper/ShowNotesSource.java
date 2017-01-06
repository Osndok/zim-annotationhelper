package meta.works.zim.annotationhelper;

/**
 * Created by robert on 2017-01-06 11:37.
 */
public
interface ShowNotesSource
{
	String getShowNotesURL(String zimPageName);
	String getShowNotesURL(String enclosingZimPage, String episodeIdentifier);
}
