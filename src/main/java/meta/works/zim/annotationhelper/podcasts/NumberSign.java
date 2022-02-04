package meta.works.zim.annotationhelper.podcasts;

public
class NumberSign implements SpotifyPodcast
{
    private final String album;
    private final String zimPageFormat;

    public
    NumberSign(final String album, final String zimPageFormat)
    {
        this.album = album;
        this.zimPageFormat = zimPageFormat;
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
        int hash = title.lastIndexOf('#');

        if (hash < 0)
        {
            return null;
        }

        var sb = new StringBuilder();
        int l = title.length();
        int noise = 0;

        for (int i=hash+1; i<l; i++)
        {
            char c = title.charAt(i);
            if (Character.isDigit(c))
            {
                sb.append(c);
            }
            else
            if (Character.isAlphabetic(c) || isStopCharacter(c))
            {
                break;
            }
            else
            {
                noise++;
                if (noise > 3)
                {
                    break;
                }
            }
        }

        if (sb.length() == 0)
        {
            return null;
        }

        var number = sb.toString();
        var zimPage = String.format(zimPageFormat, number);
        var parsed = new ParsedTitle(number, zimPage, title);
        return parsed;
    }

    private
    boolean isStopCharacter(final char c)
    {
        return (c==':' || c=='-' || c=='|' || c == '.');
    }
}
