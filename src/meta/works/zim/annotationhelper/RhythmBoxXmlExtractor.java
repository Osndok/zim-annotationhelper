package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.*;

public
class RhythmBoxXmlExtractor
{
	private static final
	Logger log = LoggerFactory.getLogger(RhythmBoxXmlExtractor.class);

	private final
	File file;

	private final
	XMLInputFactory xmlInputFactory=XMLInputFactory.newInstance();

	public
	RhythmBoxXmlExtractor(File file)
	{
		this.file = file;
		//dnw: xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
	}

	public
	String getDescriptionFor(File musicOrPodcastFile) throws IOException, XMLStreamException
	{
		final
		String requestedBaseName=musicOrPodcastFile.getName();

		final
		XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new FileReader(this.file));

		try
		{
			String currentLocation=null;
			StringBuilder currentDescription=null;
			boolean inDescriptionTag=false;

			while(true)
			{
				final
				int type = xmlStreamReader.next();
				{
					log.debug("type: {}", type);
				}

				if (type==CHARACTERS)
				{
					if (inDescriptionTag)
					{
						String stringValue=xmlStreamReader.getText();
						log.debug("characters: '{}'", stringValue);
						currentDescription.append(stringValue);
					}
				}
				else
				if (type==ENTITY_REFERENCE)
				{
					if (inDescriptionTag)
					{
						String stringValue=xmlStreamReader.getText();
						log.debug("entity: '{}'", stringValue);
						currentDescription.append(stringValue);
					}
				}
				else
				if (type==START_ELEMENT)
				{
					final
					String tagName=xmlStreamReader.getLocalName();
					{
						log.debug("start: {}", tagName);
					}

					inDescriptionTag=false;

					if (tagName.equals("entry"))
					{
						currentDescription=null;
						currentLocation=null;
					}
					else
					if (tagName.equals("description"))
					{
						inDescriptionTag=true;
						currentDescription = new StringBuilder();
					}
					else
					if (tagName.equals("location"))
					{
						// BUG? Expecting no entities.
						if (xmlStreamReader.next()==CHARACTERS)
						{
							currentLocation = xmlStreamReader.getText();
						}
					}
				}
				else
				if (type==END_ELEMENT)
				{
					inDescriptionTag=false;

					final
					String tagName=xmlStreamReader.getLocalName();
					{
						log.trace("end: {}", tagName);
					}

					if (tagName.equals("entry"))
					{
						log.trace("{} -> {}", currentLocation, currentDescription);

						if (locationMatch(requestedBaseName, currentLocation))
						{
							log.debug("match: {}", currentLocation);
							return currentDescription.toString();
						}
					}
				}
				else
				if (type==END_DOCUMENT)
				{
					break;
				}
			}
		}
		catch (NoSuchElementException e)
		{
			log.debug("end", e);
		}
		finally
		{
			xmlStreamReader.close();
		}

		return null;
	}

	private
	boolean locationMatch(String requestedBaseName, String currentLocation)
	{
		final
		String thisBaseName=new File(currentLocation).getName();

		return thisBaseName.equals(requestedBaseName);
	}
}
