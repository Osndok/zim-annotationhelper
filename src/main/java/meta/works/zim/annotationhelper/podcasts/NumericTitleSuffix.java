package meta.works.zim.annotationhelper.podcasts;

public
class NumericTitleSuffix implements SpotifyPodcast
{
    protected final String album;
    protected final char criticalCharacter;
    protected final String zimPageFormatString;

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
        return parseTitle(title, zimPageFormatString);
    }

    protected
    ParsedTitle parseTitle(final String title, final String zimPageFormatString)
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
