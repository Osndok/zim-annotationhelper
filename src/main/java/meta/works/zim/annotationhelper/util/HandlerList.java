package meta.works.zim.annotationhelper.util;

import java.util.ArrayList;
import java.util.Arrays;

public
class HandlerList<T> extends ArrayList<Handler<T>> implements Handler<T>
{
    @Override
    public
    HandlerResult handle(final T t)
    {
        for (Handler<T> handler : this)
        {
            HandlerResult result = handler.handle(t);
            if (result == null) {
                throw new NullPointerException(handler.getClass()+".handle(t) returned null");
            }
            if (result == HandlerResult.HANDLED) {
                return HandlerResult.HANDLED;
            }
        }

        return HandlerResult.NOT_HANDLED;
    }

    public static <T2>
    HandlerList<T2> of(Handler<T2>... handlers)
    {
        var list = new HandlerList<T2>();
        list.addAll(Arrays.asList(handlers));
        return list;
    }
}
