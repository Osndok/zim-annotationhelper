package meta.works.zim.annotationhelper.util;

public
interface Handler<T>
{
    HandlerResult handle(T t);
}
