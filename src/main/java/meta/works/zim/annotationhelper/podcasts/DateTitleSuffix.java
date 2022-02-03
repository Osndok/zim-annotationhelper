package meta.works.zim.annotationhelper.podcasts;

public
class DateTitleSuffix implements SpotifyPodcast
{
    protected final String album;
    protected final char preDateCharacter;
    protected final char dateSegmentSeparator;
    protected String zimPageFormatString;

    public
    DateTitleSuffix(
            final String album,
            final char preDateCharacter,
            final char dateSegmentSeparator,
            final String zimPageFormatString
    )
    {
        this.album = album;
        this.preDateCharacter = preDateCharacter;
        this.dateSegmentSeparator = dateSegmentSeparator;
        this.zimPageFormatString = zimPageFormatString;
    }

    @Override
    public
    boolean albumNameMatches(final String s)
    {
        return album.equals(s);
    }

    @Override
    public
    ParsedTitle parseTitle(final String title)
    {
        int datePositition = title.lastIndexOf(preDateCharacter);

        if (datePositition < 0)
        {
            return null;
        }

        final
        String blurb = title.substring(0, datePositition);

        final
        String date = title.substring(datePositition+1).replace(dateSegmentSeparator, ':');

        final
        String zimPage = String.format(zimPageFormatString, date);

        return new ParsedTitle(date, zimPage, blurb);
    }
}
