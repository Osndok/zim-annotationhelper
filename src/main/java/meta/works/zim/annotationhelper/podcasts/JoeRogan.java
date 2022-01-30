package meta.works.zim.annotationhelper.podcasts;

public
class JoeRogan implements SpotifyPodcast
{
    public static final String ALBUM = "The Joe Rogan Experience";

    public static
    String getZimPage(String title)
    {
        if (title == null)
        {
            return null;
        }

        if (title.charAt(0) == '#')
        {
            title = title.substring(1);
        }

        int firstSpace = title.indexOf(' ');

        if (firstSpace > 0)
        {
            String number = title.substring(0, firstSpace);

            try
            {
                int numericConfirmation = Integer.parseInt(number);
                return String.format(":JRE:%s", number);
            }
            catch (Exception e)
            {
                // Number format exception?
            }
        }

        return null;
    }

    @Override
    public
    boolean albumNameMatches(final String s)
    {
        return s.equals(ALBUM);
    }

    @Override
    public
    String getZimPageFromTitle(final String title)
    {
        return getZimPage(title);
    }
}
