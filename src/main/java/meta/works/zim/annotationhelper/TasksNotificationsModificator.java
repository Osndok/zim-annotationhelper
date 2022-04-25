package meta.works.zim.annotationhelper;

import java.io.IOException;

public
class TasksNotificationsModificator
{
    private final ZimPageAppender zimPageAppender;

    public
    TasksNotificationsModificator(final ZimPageAppender zimPageAppender)
    {

        this.zimPageAppender = zimPageAppender;
    }

    public
    void OnNotificationDisplayed(final String id, final String title) throws IOException, InterruptedException
    {
        if (title.endsWith(" tasks") && OneSpace(title))
        {
            return;
        }

        // TODO: If this was just recently dismissed, then suppress both this and the original dismissal message.
        zimPageAppender.journalNote("Task: "+title);
    }

    public
    void OnNotificationDismissed(final String id, final String title) throws IOException, InterruptedException
    {
        if (title.endsWith(" tasks") && OneSpace(title))
        {
            return;
        }

        // TODO: defer this, a few seconds, to see if it is instantly put back on the screen.
        zimPageAppender.journalNote("dismissed: Task: "+title);
    }

    private
    boolean OneSpace(final String s)
    {
        // COPYPASTA WARNING: We rely on the fact that we already know there is one space
        return s.indexOf(' ') == s.lastIndexOf(' ');
    }
}
