package meta.works.zim.annotationhelper;

import java.io.File;
import java.io.IOException;

public
interface ZimPageAppender
{
    void journalNote(String memo) throws IOException, InterruptedException;

    void journalNoteStruckOut(String memo) throws IOException, InterruptedException;

    void pageNote(String pageName, String memo) throws IOException, InterruptedException;

    void nowPlaying(StateSnapshot state);

    // TODO: Find a way OTHER than direct reference of the file to query zim for the page contents.
    File getPageFile(String pageName);

    void newActionItem(String memo) throws IOException, InterruptedException;
}
