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

    @Test
    public void libertarianChristian()
    {
        var r = testCase("Libertarian Christian Podcast", "Ep 259: Rediscovering Republicanism, with John Nantz");
        assertThat(r.number).isEqualTo("259");
        assertThat(r.zimPage).isEqualTo(":Libertarian:Christian:Podcast:259");
        assertThat(r.blurb).isEqualTo("Rediscovering Republicanism, with John Nantz");
    }

    @Test
    public void lateNightLinux()
    {
        var r = testCase("Late Night Linux All Episodes", "Late Night Linux X Episode 161");
        assertThat(r.number).isEqualTo("161");
        assertThat(r.zimPage).isEqualTo(":LNL:161");
    }

    @Test
    public void lateNightLinuxExtras()
    {
        var r = testCase("Late Night Linux All Episodes", "Late Night Linux Extra X Episode 38");
        assertThat(r.number).isEqualTo("38");
        assertThat(r.zimPage).isEqualTo(":LNL:Extra:38");
    }

    @Test
    public void linuxDowntime()
    {
        var r = testCase("Late Night Linux All Episodes", "Linux Downtime X Episode 39");
        assertThat(r.number).isEqualTo("39");
        assertThat(r.zimPage).isEqualTo(":Linux:Downtime:Podcast:39");
    }

    @Test
    public void linuxAfterDark()
    {
        var r = testCase("Late Night Linux All Episodes", "Linux After Dark X Episode 09");
        assertThat(r.number).isEqualTo("09");
        assertThat(r.zimPage).isEqualTo(":Linux:AfterDark:09");
    }

    @Test
    public void freeTalkLive()
    {
        var r = testCase("Free Talk Live", "Free Talk Live 2022-01-03");
        assertThat(r.number).isEqualTo("2022:01:03");
        assertThat(r.zimPage).isEqualTo(":FreeTalk:Live:2022:01:03");
    }

    @Test
    public void freeTalkLive2()
    {
        var r = testCase("Free Talk Live", "Free Talk Live 2022-05-31 - Ron Paul Interview");
        assertThat(r.number).isEqualTo("2022:05:31");
        assertThat(r.zimPage).isEqualTo(":FreeTalk:Live:2022:05:31");
    }

    @Test
    public void freeTalkDigest()
    {
        var r = testCase("Free Talk Live", "FTL Digest 2022-01-03");
        assertThat(r.number).isEqualTo("2022:01:03");
        assertThat(r.zimPage).isEqualTo(":FreeTalk:Digest:2022:01:03");
    }

    @Test
    public void libertyLockdown()
    {
        var r = testCase("Liberty Lockdown", "Ep 123 Ian Crossland and the Nature of the Universe");
        assertThat(r.number).isEqualTo("123");
        assertThat(r.zimPage).isEqualTo(":Liberty:Lockdown:123");
    }

    @Test
    public void moneroTalk()
    {
        var r = testCase("Monero Talk", "SUBSHOW EPI #123: Description goes here");
        assertThat(r.number).isEqualTo("123");
        assertThat(r.zimPage).isEqualTo(":Monero:Talk:123");
    }

    @Test
    public void orangePill()
    {
        var r = testCase("Orange Pill Podcast", "Orange Pill [OP57] - Remembering");
        assertThat(r.number).isEqualTo("57");
        assertThat(r.zimPage).isEqualTo(":Orange:Pill:57");
    }

    @Test
    public void lexFridman()
    {
        var r = testCase("Lex Fridman Podcast", "#123 - Summary");
        assertThat(r.number).isEqualTo("123");
        assertThat(r.zimPage).isEqualTo(":LexFridman:Podcast:123");
    }

    @Test
    public void timcastIrl()
    {
        var r = testCase("Timcast IRL", "Timcast IRL #123 - Summary");
        assertThat(r.number).isEqualTo("123");
        assertThat(r.zimPage).isEqualTo(":Timcast:IRL:123");
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
