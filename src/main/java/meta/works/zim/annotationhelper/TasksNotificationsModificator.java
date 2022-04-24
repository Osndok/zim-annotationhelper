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
        if (title.endsWith(" tasks"))
        {
            return;
        }

        // TODO: If this was just recently dismissed, then suppress both this and the original dismissal message.
        zimPageAppender.journalNote("Task: "+title);
    }

    public
    void OnNotificationDismissed(final String id, final String title) throws IOException, InterruptedException
    {
        if (title.endsWith(" tasks"))
        {
            return;
        }

        // TODO: defer this, a few seconds, to see if it is instantly put back on the screen.
        zimPageAppender.journalNote("dismissed: Task: "+title);
    }
}
