package meta.works.zim.annotationhelper.podcasts;

public
class NumericTitlePrefix implements SpotifyPodcast
{
    private final String album;
    private final char criticalCharacter;
    private final String zimPageFormatString;

    public
    NumericTitlePrefix(final String album, final char criticalCharacter, final String zimPageFormatString)
    {
        this.album = album;
        this.criticalCharacter = criticalCharacter;
        this.zimPageFormatString = zimPageFormatString;
    }

    public static final String EPISODE_PREFIX_1 = "Episode ";

    @Override
    public
    boolean albumNameMatches(final String s)
    {
        return s.equals(album);
    }

    @Override
    public
    String getZimPageFromTitle(String title)
    {
        title = stripNoise(title);

        int firstOccurrence = title.indexOf(criticalCharacter);

        if (firstOccurrence <= 0)
        {
            return null;
        }

        final
        String number = title.substring(0, firstOccurrence);

        return String.format(zimPageFormatString, number);
    }

    private
    String stripNoise(String title)
    {
        if (title.startsWith(album))
        {
            title = title.substring(album.length());
        }

        if (title.startsWith(EPISODE_PREFIX_1))
        {
            title = title.substring(EPISODE_PREFIX_1.length());
        }

        return title.trim();
    }
}
