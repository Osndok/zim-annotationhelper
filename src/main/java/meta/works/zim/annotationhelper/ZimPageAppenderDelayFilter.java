package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;

// WIP
public
class ZimPageAppenderDelayFilter
       implements ZimPageAppender
{
    private static final
    Logger log = LoggerFactory.getLogger(ZimPageAppenderDelayFilter.class);

    private final ZimPageAppender impl;

    public
    ZimPageAppenderDelayFilter(final ZimPageAppender impl)
    {
        this.impl = impl;
    }

    @Override
    public
    void journalNote(final String memo, final Date effectiveTime) throws IOException, InterruptedException
    {
        if (shouldDelayMemo(memo))
        {
            var delayMe = new DeferredMemo(memo, effectiveTime);

            // TODO: implement "delay" instead of current implementation of... "drop on the floor"

            log.warn("unimplemented: dropping: {}", memo);
        }
        else if (indicatesDumpDelayed(memo))
        {
            // TODO: delayed = null ? timer.cancel ?
            impl.journalNote(memo, effectiveTime);
        }
        else
        {
            impl.journalNote(memo, effectiveTime);
        }
    }

    private
    boolean shouldDelayMemo(final String memo)
    {
        return memo.endsWith(": Incoming call");
    }

    private
    boolean indicatesDumpDelayed(final String memo)
    {
        return memo.endsWith(": Incoming suspected spam call");
    }

    @Override
    public
    void journalNoteStruckOut(final String memo) throws IOException, InterruptedException
    {
        impl.journalNoteStruckOut(memo);
    }

    @Override
    public
    void pageNote(final String pageName, final String memo) throws IOException, InterruptedException
    {
        impl.pageNote(pageName, memo);
    }

    @Override
    public
    void nowPlaying(final StateSnapshot state)
    {
        impl.nowPlaying(state);
    }

    @Override
    public
    File getPageFile(final String pageName)
    {
        return impl.getPageFile(pageName);
    }

    @Override
    public
    void newActionItem(final String memo) throws IOException, InterruptedException
    {
        impl.newActionItem(memo);
    }

    private record DeferredMemo(String memo, Date effectiveDate)
    {
    }
}
