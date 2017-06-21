package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Created by robert on 2017-06-21 00:27.
 */
public
class StashFile
{
	public static
	StashFile getInstance()
	{
		return INSTANCE;
	}

	private static final
	StashFile INSTANCE=new StashFile(new File("/home/robert/.local/zim-annotation-helper.stash"));

	static
	StashFile getTestingStash() throws IOException
	{
		return new StashFile(File.createTempFile("/tmp/zim-annotation-helper-test", ".stash"));
	}

	private static final
	Logger log=LoggerFactory.getLogger(StashFile.class);

	private
	StashFile(File file)
	{
		this.file=file;
	}

	private final
	File file;

	//e.g. what each line means (APPEND, BUT DO NOT REORDER)
	private static
	enum Field
	{
		LAST_PLAY_TIME
	}

	public
	Long getLastPlayTime()
	{
		return getLong(Field.LAST_PLAY_TIME);
	}

	private
	Long getLong(Field field)
	{
		final
		int fieldNumber=field.ordinal();

		final
		String[] fields=readFields();

		final
		String stringValue=fields[fieldNumber];
		{
			if (stringValue==null || stringValue.isEmpty())
			{
				log.debug("{} is empty/null", field);
				return null;
			}
		}

		return Long.parseLong(stringValue);
	}

	private
	String[] readFields()
	{
		final
		ArrayList<String> fieldValues=new ArrayList<>();

		try
		{
			final
			BufferedReader br = new BufferedReader(new FileReader(file));

			String line;

			while ((line=br.readLine())!=null)
			{
				fieldValues.add(line);
			}

			br.close();
		}
		catch (IOException e)
		{
			log.info("unable to read stash file values: "+e);
		}

		final
		int numNeeded=Field.values().length;

		while (fieldValues.size() < numNeeded)
		{
			fieldValues.add(null);
		}

		return fieldValues.toArray(new String[fieldValues.size()]);
	}

	public
	void setLastPlayTime(long time)
	{
		setLong(Field.LAST_PLAY_TIME, time);
	}

	private
	void setLong(Field field, long l)
	{
		final
		int fieldNumber=field.ordinal();

		final
		String[] fields=readFields();
		{
			fields[fieldNumber] = String.valueOf(l);
		}

		writeFields(fields);
	}

	private
	void writeFields(String[] fieldValues)
	{
		try
		{
			final
			PrintStream out = new PrintStream(file);

			for (String fieldValue : fieldValues)
			{
				if (fieldValue.indexOf('\n')>=0)
				{
					throw new IllegalArgumentException("stash value cannot contain a newline character");
				}

				out.println(fieldValue);
			}

			out.close();
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

}
