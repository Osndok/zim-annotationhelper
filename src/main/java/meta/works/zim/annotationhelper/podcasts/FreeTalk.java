package meta.works.zim.annotationhelper.podcasts;

public
class FreeTalk extends DateTitleSuffix
{
    public
    FreeTalk()
    {
        super("Free Talk Live", ' ', '-', "DYNAMIC :(");
    }

    @Override
    public
    ParsedTitle parseTitle(final String title)
    {
        if (title.startsWith("Free Talk Live "))
        {
            zimPageFormatString = ":FreeTalk:Live:%s";
            return super.parseTitle(title);
        }

        if (title.startsWith("FTL Digest "))
        {
            zimPageFormatString = ":FreeTalk:Digest:%s";
            return super.parseTitle(title);
        }

        return null;
    }
}
