package meta.works.zim.annotationhelper.podcasts;

import java.util.List;

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

    private static final List<String> EPISODE_PREFIX_NOISE = List.of(
        "Episode ",
        "Ep ",
        "#"
    );

    @Override
    public
    boolean albumNameMatches(final String s)
    {
        return s.equals(album);
    }

    @Override
    public
    ParsedTitle parseTitle(String title)
    {
        title = stripNoise(title);

        int firstOccurrence = title.indexOf(criticalCharacter);

        if (firstOccurrence <= 0)
        {
            return null;
        }

        final
        String number = title.substring(0, firstOccurrence);

        final
        String blurb = title.substring(firstOccurrence+1).trim();

        final
        String zimPage = String.format(zimPageFormatString, number);

        return new ParsedTitle(number, zimPage, blurb);
    }

    private
    String stripNoise(String title)
    {
        if (title.startsWith(album))
        {
            title = title.substring(album.length()).trim();
        }

        if (title.endsWith(album))
        {
            title = title.substring(0, title.length()-album.length()).trim();
        }

        for (String prefix : EPISODE_PREFIX_NOISE)
        {
            if (title.startsWith(prefix))
            {
                title = title.substring(prefix.length());
            }
        }

        // This could also work as a stripped suffix.
        title = title.replace("|", "");

        return title.trim();
    }
}
