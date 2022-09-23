package meta.works.zim.annotationhelper;

public
class PhoneNumberLinker
{
    public
    String linkifyPhoneNumber(final String s)
    {
        if (s == null || !s.startsWith("+1 ") || s.length() != 15) {
            return s;
        }

        var retval = new StringBuilder("[[:Phone:Number:");
        var displayText = new StringBuilder("(");

        for (int i = 3; i < 15; i ++) {
            char c = s.charAt(i);

            if (i == 6) {
                if (c == '-') {
                    retval.append(':');
                    displayText.append(") ");
                    continue;
                } else {
                    return s;
                }
            }

            if (i == 10) {
                if (c == '-') {
                    retval.append(':');
                    displayText.append('-');
                    continue;
                } else {
                    return s;
                }
            }

            if (!Character.isDigit(c)) {
                return s;
            }

            retval.append(c);
            displayText.append(c);
        }

        retval.append('|');
        retval.append(displayText);
        retval.append("]]");

        return retval.toString();
    }
}
