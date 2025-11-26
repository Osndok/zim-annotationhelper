package meta.works.zim.annotationhelper;

import meta.works.zim.annotationhelper.util.Handler;
import meta.works.zim.annotationhelper.util.HandlerResult;

import java.io.IOException;

public
class CommandExecutorStringHandler
        implements Handler<String>
{
    @Override
    public
    HandlerResult handle(final String s)
    {
        try
        {
            // TODO: Any exceptions thrown by this (and maybe even stderr output?) should be returned via pushbullet.
            Runtime.getRuntime().exec(s);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return HandlerResult.HANDLED;
    }
}
