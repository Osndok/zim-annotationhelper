package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by robert on 2016-10-06 11:36.
 */
public
class ZimPageAppender
{
	private static final
	Logger log = LoggerFactory.getLogger(ZimPageAppender.class);

	private static final boolean LOG_FIRST_PLAY_ONLY = Boolean.getBoolean("log.firstPlayOnly");

	private
	String lastJournalNote;

	public
	void journalNote(String memo) throws IOException, InterruptedException
	{
		log.debug("journalNote('{}')", memo);

		if (memo.equals(lastJournalNote))
		{
			return;
		}
		lastJournalNote=memo;

		final
		String[] command = new String[]
			{
				"zim", "--plugin", "append",
				"--journal",
				"--time",
				"--literal", memo
			};

		final
		ProcessBuilder processBuilder = new ProcessBuilder(command);

		final
		Process process = processBuilder.start();

		final
		int statusCode = process.waitFor();

		if (statusCode == 0)
		{
			log.trace("zim-plugin-append exit success");
		}
		else
		{
			log.error("zim-plugin-append exit status {}", statusCode);
		}
	}

	public
	void pageNote(String pageName, String memo) throws IOException, InterruptedException
	{
		log.debug("pageNote('{}', '{}')", pageName, memo);

		if (pageName==null)
		{
			log.warn("ignoring pageNote() with null page");
			return;
		}

		//NB: the same pageNote might be applied to different pages, which would marginally complicate duplicate suppression.

		final
		String[] command = new String[]
			{
				"zim", "--plugin", "append",
				"--page", pageName,
				"--literal", memo
			};

		final
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		{
			processBuilder.inheritIO();
		}

		final
		Process process = processBuilder.start();

		final
		int statusCode = process.waitFor();

		if (statusCode == 0)
		{
			log.trace("zim-plugin-append exit success");
		}
		else
		{
			log.error("zim-plugin-append exit status {}", statusCode);
		}
	}

	static final
	Calendar calendar=Calendar.getInstance();

	static
	{
		calendar.setTimeZone(TimeZone.getTimeZone("US/Hawaii"));
	}

	public
	void nowPlaying(String url, String title)
	{
		final
		StashFile stashFile = StashFile.getInstance();

		final
		Long lastPlayTime = stashFile.getLastPlayTime();

		final
		boolean first;

		if (lastPlayTime == null || differentDayNumber(lastPlayTime))
		{
			first = true;
		}
		else
		{
			first = false;

			if (LOG_FIRST_PLAY_ONLY) {
				return;
			}
		}

		appendNowPlayingNotationToZimJournal(stashFile, url, title);
	}

	private
	void appendNowPlayingNotationToZimJournal(StashFile stashFile, String url, String title)
	{
		if (title == null || title.isEmpty())
		{
			title = basename(maybeDecode(url));
		}
		else
		{
			title = maybeDecode(title);
		}

		try
		{
			journalNote(title);
		}
		catch (Exception e)
		{
			log.error("caught", e);
		}

		stashFile.setLastPlayTime(System.currentTimeMillis());
	}

	private
	String maybeDecode(final String s)
	{
		try
		{
			return URLDecoder.decode(s, StandardCharsets.UTF_8);
		}
		catch (Throwable e)
		{
			// NB: If we log anything here it will likely spam the logs horribly...
			return s;
		}
	}

	private
	String basename(String url)
	{
		return new File(url).getName();
	}

	private
	boolean differentDayNumber(Long time)
	{
		final
		int thatDay;
		{
			calendar.setTime(new Date(time));
			thatDay=calendar.get(Calendar.DATE);
		}

		final
		int thisDay;
		{
			calendar.setTime(new Date());
			thisDay=calendar.get(Calendar.DATE);
		}

		log.debug("day match? {} =?= {}", thatDay, thisDay);
		return thatDay != thisDay;
	}

	// TODO: Find a way OTHER than direct reference of the file to query zim for the page contents.
	public
	File getPageFile(final String pageName)
	{
		final
		StringBuilder sb=new StringBuilder(getNotebookDirectory());

		for (String bit : pageName.split(":"))
		{
			if (!bit.isEmpty())
			{
				sb.append(File.separator);
				sb.append(bit);
			}
		}

		sb.append(".txt");

		final
		String retval=sb.toString();

		log.debug("getPageFile('{}') -> '{}'", pageName, retval);

		return new File(retval);
	}

	// TODO: Find a way OTHER than direct reference of the file to query zim for the page contents.
	private
	String getNotebookDirectory()
	{
		final
		String override=System.getProperty("zim.notebook.path");

		if (override==null)
		{
			final
			String userHome=System.getProperty("user.home");

			final
			String notebooksDir=System.getProperty("zim.notebooks.dirname", "Notebooks");

			final
			String notebookName=System.getProperty("zim.notebook.name", "Primary");

			return String.format("%s%s%s%s%s", userHome, File.separator, notebooksDir, File.separator, notebookName);
		}
		else
		{
			return override;
		}
	}

	private
	String lastActionItem;

	private static final
	String actionItemPageName = System.getProperty("zim.annotationhelper.todopage", ":Todo");

	public
	void newActionItem(String memo) throws IOException, InterruptedException
	{
		log.debug("newActionItem('{}')", memo);

		if (memo.equals(lastActionItem))
		{
			return;
		}
		lastActionItem=memo;

		final
		String[] command;
		{
			if (actionItemPageName.isEmpty())
			{
				command = new String[]
				{
					"zim", "--plugin", "append",
					"--journal",
					"--literal", "[ ] "+memo
				};
			}
			else
			{
				command = new String[]
				{
					"zim", "--plugin", "append",
					"--page", actionItemPageName,
					"--literal", "[ ] "+memo
				};
			}
		}

		final
		ProcessBuilder processBuilder = new ProcessBuilder(command);

		final
		Process process = processBuilder.start();

		final
		int statusCode = process.waitFor();

		if (statusCode == 0)
		{
			log.trace("zim-plugin-append exit success");
		}
		else
		{
			log.error("zim-plugin-append exit status {}", statusCode);
		}
	}
}
