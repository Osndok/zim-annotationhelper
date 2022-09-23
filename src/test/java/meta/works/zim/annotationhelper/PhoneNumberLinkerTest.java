package meta.works.zim.annotationhelper;

import junit.framework.TestCase;

import static org.assertj.core.api.Assertions.assertThat;

public
class PhoneNumberLinkerTest
        extends TestCase
{
    private final
    PhoneNumberLinker underTest = new PhoneNumberLinker();

    public
    void testLinkifyPhoneNumber()
    {
        _doesNotModify(null);
        _doesNotModify("");
        _doesNotModify("+1 123-456-789");
        _doesNotModify("+1 123-456-78901");
        _doesNotModify("+2 123-456-7890");
        _doesNotModify("+1 1238456-7890");
        _doesNotModify("+1 123-45697890");
        _doesNotModify("+1 123-456-7x90");

        _case("+1 123-456-7890", "[[:Phone:Number:123:456:7890|(123) 456-7890]]");
    }

    private
    void _doesNotModify(final String input)
    {
        _case(input, input);
    }

    private
    void _case(final String input, final String expectedOutput)
    {
        var result = underTest.linkifyPhoneNumber(input);
        assertThat(result).isEqualTo(expectedOutput);
    }
}
