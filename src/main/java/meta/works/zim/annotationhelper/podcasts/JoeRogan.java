package meta.works.zim.annotationhelper.podcasts;

public
class JoeRogan implements SpotifyPodcast
{
    public static final String ALBUM = "The Joe Rogan Experience";

    @Override
    public
    ParsedTitle parseTitle(String title)
    {
        if (title == null)
        {
            return null;
        }

        int firstHash = title.indexOf('#');

        final
        String subShow;

        if (firstHash == 0)
        {
            title = title.substring(1).replace("- ", "").trim();
            subShow = null;
        }
        else
        {
            subShow = title.substring(0, firstHash).replace("JRE ", "").replace("Show ", "").trim();
            title = title.substring(firstHash+1).replace("with ", "").trim();
        }

        int firstSpace = title.indexOf(' ');

        final
        String number = title.substring(0, firstSpace);

        final
        String blurb = title.substring(firstSpace+1);

        final
        String zimPage;
        {
            if (subShow == null)
            {
                zimPage = String.format(":JRE:%s", number);
            }
            else
            if (subShow.indexOf(' ') >= 0)
            {
                // Unknown subshow, need an example
                return null;
            }
            else
            {
                zimPage = String.format(":JRE:%s:%s", subShow, number);
            }
        }

        return new ParsedTitle(number, zimPage, blurb);
    }

    @Override
    public
    boolean albumNameMatches(final String s)
    {
        return s.equals(ALBUM);
    }
}
