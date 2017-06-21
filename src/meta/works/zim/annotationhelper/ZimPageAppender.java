package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

	public
	void journalNote(String memo) throws IOException, InterruptedException
	{
		log.debug("journalNote('{}')", memo);

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
			//if (DEBUG) processBuilder.inheritIO();
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
	void maybeNoteFirstPlay(String url)
	{
		final
		StashFile stashFile = StashFile.getInstance();

		final
		Long lastPlayTime = stashFile.getLastPlayTime();

		if (lastPlayTime == null || differentDayNumber(lastPlayTime))
		{
			try
			{
				journalNote("First music/podcast of the day: "+basename(url));
			}
			catch (Exception e)
			{
				log.error("caught", e);
			}

			stashFile.setLastPlayTime(System.currentTimeMillis());
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
}
