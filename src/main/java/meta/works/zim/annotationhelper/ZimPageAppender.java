package meta.works.zim.annotationhelper;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public
interface ZimPageAppender
{
    DateTimeFormatter JOURNAL_MONTH_PAGE_FORMATTER = DateTimeFormatter.ofPattern(":'Journal':yyyy:MM");

    default
    void journalNote(String memo) throws IOException, InterruptedException
    {
        journalNote(memo, new Date());
    }

    void journalNote(String memo, Date effectiveTime) throws IOException, InterruptedException;

    void journalNoteStruckOut(String memo) throws IOException, InterruptedException;

    void pageNote(String pageName, String memo) throws IOException, InterruptedException;

    void nowPlaying(StateSnapshot state);

    // TODO: Find a way OTHER than direct reference of the file to query zim for the page contents.
    File getPageFile(String pageName);

    void newActionItem(String memo) throws IOException, InterruptedException;

    default
    void journalMonthNote(String memo, Date effectiveTime) throws IOException, InterruptedException
    {
        var localDateTime = effectiveTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        var page = JOURNAL_MONTH_PAGE_FORMATTER.format(localDateTime);

        pageNote(page, memo);
    }
}
