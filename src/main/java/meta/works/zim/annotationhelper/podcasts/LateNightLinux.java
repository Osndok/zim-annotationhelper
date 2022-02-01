package meta.works.zim.annotationhelper.podcasts;

public
class LateNightLinux extends NumericTitleSuffix
{
    public
    LateNightLinux()
    {
        super("Late Night Linux", ' ', ":LNL:%s");
    }

    @Override
    public
    boolean albumNameMatches(final String album)
    {
        return album.startsWith(this.album);
    }

    @Override
    public
    ParsedTitle parseTitle(final String title)
    {
        if (title.startsWith("Late Night Linux Extra"))
        {
            return parseTitle(title, ":LNL:Extra:%s");
        }

        if (title.startsWith("Late Night Linux"))
        {
            return parseTitle(title, ":LNL:%s");
        }

        if (title.startsWith("Linux Downtime"))
        {
            return parseTitle(title, ":Linux:Downtime:Podcast:%s");
        }

        if (title.startsWith("Linux After Dark"))
        {
            return parseTitle(title, ":Linux:AfterDark:%s");
        }

        return null;
    }
}
