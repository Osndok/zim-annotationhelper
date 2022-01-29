package meta.works.zim.annotationhelper;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public
class SpotifyPlayerTests
{
    @Test
    public void reduceAdvertBlurbExamples()
    {
        assertThat(SpotifyPlayer.reduceAdvertBlurb("Basic Thing")).isEqualTo("Basic Thing");
        assertThat(SpotifyPlayer.reduceAdvertBlurb("Advertisement from \"Company, Inc.\"")).isEqualTo("Company, Inc.");
    }
}
