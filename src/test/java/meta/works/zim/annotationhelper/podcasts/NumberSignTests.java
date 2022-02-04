package meta.works.zim.annotationhelper.podcasts;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public
class NumberSignTests
{
    private final
    NumberSign underTest = new NumberSign("album", "PRE:%s:POST");

    @Test
    public
    void testVectors()
    {
        _case("", null);
        _case("Alphabet", null);
        _case("12345", null);
        _case("#", null);
        _case("    #", null);
        _case("#    ", null);

        _case("#1", "1");
        _case("Show #12345678901234567890 reference", "12345678901234567890");
        _case("Episode # 123!", "123");
        _case("#1 - 2020 Wrapup", "1");
        _case("#1: 12 things to avoid", "1");
    }

    private
    void _case(final String input, final String expectedNumber)
    {
        var parsed = underTest.parseTitle(input);

        if (expectedNumber == null)
        {
            assertThat(parsed).isNull();
            return;
        }

        assertThat(parsed.number).isEqualTo(expectedNumber);
    }

}
