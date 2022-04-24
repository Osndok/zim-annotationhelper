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
        zimPageAppender.journalNote("Task: "+title);
    }

    public
    void OnNotificationDismissed(final String id, final String summary) throws IOException, InterruptedException
    {
        zimPageAppender.journalNote("dismissed: Task: "+summary);
    }
}
