package meta.works.zim.annotationhelper;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public
class TasksNotificationsModificator
{
    private final ZimPageAppender zimPageAppender;
    private final Timer timer = new Timer();
    private final Map<String, DeferLoggingDismissalMessage> deferredDismissalsByTitle = new ConcurrentHashMap<>();

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

        var deferred = deferredDismissalsByTitle.get(title);

        // If we are reinstating a task title that was just recently dismissed, then suppress both this activation and the original/deferred dismissal message.
        if (deferred != null && deferred.stillValid)
        {
            deferred.cancel();
        }
        else
        {
            // Otherwise, just log it as a task.
            zimPageAppender.journalNote("Task: " + title);
        }
    }

    public
    void OnNotificationDismissed(final String id, final String title) throws IOException, InterruptedException
    {
        if (title.endsWith(" tasks") && OneSpace(title))
        {
            return;
        }

        var deferred = new DeferLoggingDismissalMessage(title);
        deferredDismissalsByTitle.put(title, deferred);
        timer.schedule(deferred, 2000);
    }

    private
    boolean OneSpace(final String s)
    {
        // COPYPASTA WARNING: We rely on the fact that we already know there is one space
        return s.indexOf(' ') == s.lastIndexOf(' ');
    }

    private
    class DeferLoggingDismissalMessage
            extends TimerTask
    {
        final String title;
        volatile boolean stillValid = true;

        private
        DeferLoggingDismissalMessage(final String title)
        {
            this.title = title;
        }

        @Override
        public
        void run()
        {
            neutralize();
            try
            {
                zimPageAppender.journalNoteStruckOut("dismissed: Task: "+title);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        private
        void neutralize()
        {
            stillValid = false;
            deferredDismissalsByTitle.remove(title, this);
        }

        @Override
        public
        boolean cancel()
        {
            neutralize();
            return super.cancel();
        }
    }
}
