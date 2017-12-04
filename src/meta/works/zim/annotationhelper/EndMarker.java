package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public
class EndMarker
{
	public static final String STRING=".end";

	private static final
	Logger log = LoggerFactory.getLogger(EndMarker.class);

	public static
	boolean isPresentIn(File file)
	{
		try
		{
			final
			BufferedReader br = new BufferedReader(new FileReader(file));

			try
			{
				String line = br.readLine();

				while (line != null)
				{
					if (line.equals(STRING))
					{
						return true;
					}

					line=br.readLine();
				}
			}
			finally
			{
				br.close();
			}
		}
		catch (FileNotFoundException e)
		{
			log.debug("fnf");
		}
		catch (IOException e)
		{
			log.info("caught: {}", e.toString(), e);
		}

		return false;
	}
}
