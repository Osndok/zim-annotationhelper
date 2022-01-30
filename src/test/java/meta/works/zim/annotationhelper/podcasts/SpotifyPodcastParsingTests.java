package meta.works.zim.annotationhelper.podcasts;

import meta.works.zim.annotationhelper.SpotifyPlayer;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public
class SpotifyPodcastParsingTests
{
    @Test
    public void jre()
    {
        var r = testCase("The Joe Rogan Experience", "#1749 - Shane Dorian");
        assertThat(r.number).isEqualTo("1749");
        assertThat(r.zimPage).isEqualTo(":JRE:1749");
        assertThat(r.blurb).isEqualTo("Shane Dorian");
    }

    @Test
    public void jreMma()
    {
        var r = testCase("The Joe Rogan Experience", "JRE MMA Show #118 with Julianna Pena");
        assertThat(r.number).isEqualTo("118");
        assertThat(r.zimPage).isEqualTo(":JRE:MMA:118");
        assertThat(r.blurb).isEqualTo("Julianna Pena");
    }

    @Test
    public void askNoah()
    {
        var r = testCase("Ask Noah Show", "Episode 270: Soft Skills & Cupcakes");
        assertThat(r.number).isEqualTo("270");
        assertThat(r.zimPage).isEqualTo(":NoahChelliah:Ask:270");
        assertThat(r.blurb).isEqualTo("Soft Skills & Cupcakes");
    }

    @Test
    public void twoPointFiveAdmins()
    {
        var r = testCase("2.5 Admins", "2.5 Admins 75: Burning Fiber");
        assertThat(r.number).isEqualTo("75");
        assertThat(r.zimPage).isEqualTo(":Two:PointFiveAdmins:75");
        assertThat(r.blurb).isEqualTo("Burning Fiber");
    }

    @Test
    public void coderRadio()
    {
        var r = testCase("Coder Radio", "449: Monetized Misery");
        assertThat(r.number).isEqualTo("449");
        assertThat(r.zimPage).isEqualTo(":CR:449");
        assertThat(r.blurb).isEqualTo("Monetized Misery");
    }

    @Test
    public void hardwareAddicts()
    {
        var r = testCase("Hardware Addicts", "52: The Ultimate Monitor Shopping Guide");
        assertThat(r.number).isEqualTo("52");
        assertThat(r.zimPage).isEqualTo(":Hardware:Addicts:52");
        assertThat(r.blurb).isEqualTo("The Ultimate Monitor Shopping Guide");
    }

    @Test
    public void linuxUnplugged()
    {
        var r = testCase("LINUX Unplugged", "442: Liberty Leaks and Lies");
        assertThat(r.number).isEqualTo("442");
        assertThat(r.zimPage).isEqualTo(":LUP:442");
        assertThat(r.blurb).isEqualTo("Liberty Leaks and Lies");
    }

    @Test
    public void linuxActionNews()
    {
        var r = testCase("Linux Action News", "Linux Action News 225");
        assertThat(r.number).isEqualTo("225");
        assertThat(r.zimPage).isEqualTo(":LinuxAction:News:225");
        //assertThat(r.blurb).isEqualTo("Linux Action News 225");
    }

    @Test
    public void selfHosted()
    {
        var r = testCase("Self-Hosted", "63: Pulling the Rug Out");
        assertThat(r.number).isEqualTo("63");
        assertThat(r.zimPage).isEqualTo(":Self:Hosted:Podcast:63");
        assertThat(r.blurb).isEqualTo("Pulling the Rug Out");
    }

    @Test
    public void thisWeekInLinux()
    {
        var r = testCase("This Week in Linux", "182: Exciting FlatHub, Wine 7.0, SUSE Liberty Linux, Linux Mint, AppIndicators | This Week in Linux");
        assertThat(r.number).isEqualTo("182");
        assertThat(r.zimPage).isEqualTo(":TWIL:182");
        assertThat(r.blurb).isEqualTo("Exciting FlatHub, Wine 7.0, SUSE Liberty Linux, Linux Mint, AppIndicators");
    }

    private
    ParsedTitle testCase(final String album, final String title)
    {
        var podcast = SpotifyPlayer.getPodcastGivenAlbum(album);

        assertThat(podcast)
                .as("No podcast class claimed album: "+album)
                .isNotNull();

        var parsed = podcast.parseTitle(title);

        assertThat(parsed)
                .as(podcast+" did not parse title: "+title)
                .isNotNull();

        return parsed;
    }
}
