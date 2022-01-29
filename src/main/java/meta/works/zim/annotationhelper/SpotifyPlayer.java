package meta.works.zim.annotationhelper;

import meta.works.zim.annotationhelper.podcasts.JoeRogan;
import org.freedesktop.dbus.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public
class SpotifyPlayer extends AbstractDBusMediaPlayer
{
    private static final
    Logger log = LoggerFactory.getLogger(SpotifyPlayer.class);

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

        if (album.equals(JoeRogan.ALBUM))
        {
            return JoeRogan.getZimPage(getString(metadata, "xesam:title"));
        }

        log.trace("Unknown spotify album: {}", album);
        return null;
    }

    @Override
    StateChangeReturn onStateChange(final StateSnapshot was, final StateSnapshot now, final long age) throws
                                                                                                      IOException,
                                                                                                      InterruptedException
    {
        return null;
    }

    @Override
    void onPeriodicInterval(final StateSnapshot state) throws IOException, InterruptedException
    {

    }
}
