package meta.works.zim.annotationhelper.podcasts;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public
class JoeRoganTests
{
    @Test
    public void numericPageNameExtraction()
    {
        String page = JoeRogan.getZimPage("#1234 - Guest Name");
        assertThat(page).isEqualTo(":JRE:1234");
    }
}
