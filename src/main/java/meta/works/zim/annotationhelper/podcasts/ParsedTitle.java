package meta.works.zim.annotationhelper.podcasts;

public
class ParsedTitle
{
    public final String number;
    public final String zimPage;
    public final String blurb;

    public
    ParsedTitle(final String number, final String zimPage, final String blurb)
    {
        this.number = number;
        this.zimPage = zimPage;
        this.blurb = blurb;
    }

    @Override
    public
    boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final ParsedTitle that = (ParsedTitle) o;

        if (number != null ? !number.equals(that.number) : that.number != null)
        {
            return false;
        }
        if (zimPage != null ? !zimPage.equals(that.zimPage) : that.zimPage != null)
        {
            return false;
        }
        return blurb != null ? blurb.equals(that.blurb) : that.blurb == null;
    }

    @Override
    public
    int hashCode()
    {
        int result = number != null ? number.hashCode() : 0;
        result = 31 * result + (zimPage != null ? zimPage.hashCode() : 0);
        result = 31 * result + (blurb != null ? blurb.hashCode() : 0);
        return result;
    }
}
