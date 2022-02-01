package meta.works.zim.annotationhelper;

import meta.works.zim.annotationhelper.podcasts.*;
import org.freedesktop.dbus.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * TODO: ask spotify to expose the current playhead position via mpris/dbus
 * TODO: wire-in the shownotes URL
 */
public
class SpotifyPlayer extends AbstractDBusMediaPlayer
{
    private static final
    Logger log = LoggerFactory.getLogger(SpotifyPlayer.class);

    public static final
    List<SpotifyPodcast> PODCASTS = List.of(
            new JoeRogan(),
            new LateNightLinux(),
            new NumericTitlePrefix("Ask Noah Show", ':', ":NoahChelliah:Ask:%s"),
            new NumericTitlePrefix("2.5 Admins", ':', ":Two:PointFiveAdmins:%s"),
            new NumericTitlePrefix("Coder Radio", ':', ":CR:%s"),
            new NumericTitlePrefix("Hardware Addicts", ':', ":Hardware:Addicts:%s"),
            new NumericTitlePrefix("LINUX Unplugged", ':', ":LUP:%s"),
            new NumericTitleSuffix("Linux Action News", ' ', ":LinuxAction:News:%s"),
            new NumericTitlePrefix("Self-Hosted", ':', ":Self:Hosted:Podcast:%s"),
            new NumericTitlePrefix("This Week in Linux", ':', ":TWIL:%s")
    );

    public
    SpotifyPlayer()
    {
        super("spotify");
    }

    @Override
    String getDBusSenderSuffix()
    {
        return "spotify";
    }

    @Override
    protected
    String getZimPage(final Map<String, Variant> metadata)
    {
        final
        String album = getString(metadata, "xesam:album");

        if (album == null)
        {
            return null;
        }

        SpotifyPodcast podcast = getPodcastGivenAlbum(album);

        if (podcast != null)
        {
            String title = getString(metadata, "xesam:title");

            var parsed = podcast.parseTitle(title);

            if (parsed == null)
            {
                log.debug("{} podcast parser did not parse title: {}", album, title);
                return null;
            }

            return parsed.zimPage;
        }

        log.trace("Unknown spotify album: {}", album);
        return null;
    }

    public static
    SpotifyPodcast getPodcastGivenAlbum(final String album)
    {
        for (SpotifyPodcast podcast : PODCASTS)
        {
            if (podcast.albumNameMatches(album))
            {
                return podcast;
            }
        }

        return null;
    }

    /**
     * When moving from one ad to another, we would lose track of which show we are playing except for this.
     */
    String zimPageHint;

    boolean inAdvert;

    @Override
    StateChangeReturn onStateChange(final StateSnapshot was, final StateSnapshot now, final long age) throws
                                                                                                      IOException,
                                                                                                      InterruptedException
    {
        if (now.getZimPage()!=null)
        {
            zimPageHint = now.getZimPage();
        }

        if (isAd(was))
        {
            if (isAd(now))
            {
                if (!was.getTrackId().equals(now.getTrackId()))
                {
                    log.debug("moving from one ad to another");
                    noteAdvert(now);
                }
            }
            else
            {
                inAdvert = false;
                log.debug("moving from an advert back to main content");
                noteAdvertBlockEnded(now);
            }
        }
        else
        {
            if (isAd(now))
            {
                inAdvert = true;
                log.debug("moving from main content to an advert");
                noteAdvertBlockStarts(was);
                noteAdvert(now);
            }
            else
            {
                log.info("was {}, but now {}", was, now);
            }
        }

        return null;
    }

    private
    void noteAdvertBlockStarts(final StateSnapshot preAdState) throws IOException, InterruptedException
    {
        zimPageAppender.pageNote(preAdState.getZimPage(), "/");
    }

    private
    void noteAdvert(final StateSnapshot advert) throws IOException, InterruptedException
    {
        // NB: adverts do not have a correct zim page reference.
        String zimPage = zimPageHint;
        String advertBlurb = reduceAdvertBlurb(advert.getTitle());
        String advertId = advert.getUrl();

        String notable = String.format("[[%s|Ad]]: %s", advertId, advertBlurb);
        zimPageAppender.pageNote(zimPage, notable);
    }

    private
    void noteAdvertBlockEnded(final StateSnapshot state) throws IOException, InterruptedException
    {
        zimPageAppender.pageNote(state.getZimPage(), "/");
    }

    private
    boolean isAd(final StateSnapshot state)
    {
        String trackId = state.getTrackId();
        return trackId != null && trackId.startsWith("spotify:ad:");
    }

    private static final String AD_PREFIX_1 = "Advertisement from ";

    public static
    String reduceAdvertBlurb(String s)
    {
        if (s.startsWith(AD_PREFIX_1))
        {
            s = s.substring(AD_PREFIX_1.length());
        }

        if (s.charAt(0) == '"')
        {
            s = s.substring(1);
        }

        if (s.endsWith("\""))
        {
            s = s.substring(0, s.length()-1);
        }

        return s;
    }

    @Override
    void onPeriodicInterval(final StateSnapshot state) throws IOException, InterruptedException
    {
        if (state.zimPage==null)
        {
            log.debug("onPeriodicInterval: {}", state.roughTimeCode);
        }
        else
        {
            log.trace("onPeriodicInterval: {} -> {}", state.roughTimeCode, state.zimPage);
            zimPageAppender.pageNote(state.zimPage, state.roughTimeCode);
        }
    }
}
