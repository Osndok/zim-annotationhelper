package meta.works.zim.annotationhelper.podcasts;

public
interface SpotifyPodcast
{
    boolean albumNameMatches(String s);
    ParsedTitle parseTitle(String title);
}
