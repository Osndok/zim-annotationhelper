package meta.works.zim.annotationhelper.podcasts;

import meta.works.zim.annotationhelper.StateSnapshot;

public
interface SpotifyPodcast
{
    boolean albumNameMatches(String s);
    String getZimPageFromTitle(String title);
}
