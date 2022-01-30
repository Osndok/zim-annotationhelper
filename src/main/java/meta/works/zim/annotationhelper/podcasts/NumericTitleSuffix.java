package meta.works.zim.annotationhelper.podcasts;

public
class NumericTitleSuffix implements SpotifyPodcast
{
    private final String album;
    private final char criticalCharacter;
    private final String zimPageFormatString;

    public
    NumericTitleSuffix(final String album, final char criticalCharacter, final String zimPageFormatString)
    {
        this.album = album;
        this.criticalCharacter = criticalCharacter;
        this.zimPageFormatString = zimPageFormatString;
    }

    @Override
    public
    boolean albumNameMatches(final String s)
    {
        return s.equals(album);
    }

    @Override
    public
    ParsedTitle parseTitle(final String title)
    {
        int lastOccurance = title.lastIndexOf(criticalCharacter);

        if (lastOccurance < 0)
        {
            return null;
        }

        final
        String blurb = title.substring(0, lastOccurance);

        final
        String number = title.substring(lastOccurance+1);

        final
        String zimPage = String.format(zimPageFormatString, number);

        return new ParsedTitle(number, zimPage, blurb);
    }
}
