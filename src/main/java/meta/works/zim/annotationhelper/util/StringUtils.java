package meta.works.zim.annotationhelper.util;

public
class StringUtils
{
    public static
    String stripPrefix(String str, String prefix) {
        if (str.startsWith(prefix)) {
            return str.substring(prefix.length());
        }
        return str;
    }

    public static void main(String[] args) {
        String str = "HelloWorld";
        String prefix = "Hello";

        String result = stripPrefix(str, prefix);
        System.out.println("Result: " + result); // Output: "World"
    }
}
