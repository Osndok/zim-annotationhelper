package meta.works.zim.annotationhelper.util;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public
class PropertiesBackedMapTest
{
	private
	Path tempDir;

	private
	Path file;

	@BeforeMethod
	public
	void setUp() throws IOException
	{
		tempDir = Files.createTempDirectory("pbm-test-");
		file = tempDir.resolve("data.properties");
	}

	@AfterMethod
	public
	void tearDown() throws IOException
	{
		if (Files.exists(tempDir))
		{
			try (Stream<Path> walk = Files.walk(tempDir))
			{
				walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
					.forEach(p ->
					{
						try
						{
							Files.deleteIfExists(p);
						}
						catch (IOException ignored)
						{
						}
					});
			}
		}
	}

	@Test
	public
	void missingFileYieldsEmptyMapAndNoFileCreated()
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);

		assertTrue(map.isEmpty());
		assertEquals(map.size(), 0);
		assertFalse(Files.exists(file), "no file should be created until a mutation occurs");
	}

	@Test
	public
	void putPersistsToDisk() throws IOException
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);

		map.put("alpha", "one");
		map.put("beta", "two");

		assertTrue(Files.exists(file));

		final
		String contents = Files.readString(file, StandardCharsets.UTF_8);

		assertEquals(contents, "alpha=one\nbeta=two\n",
			"file should be deterministic: sorted, no comments, no date header");
	}

	@Test
	public
	void putReturnsPriorValue()
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);

		assertNull(map.put("k", "v1"));
		assertEquals(map.put("k", "v2"), "v1");
	}

	@Test
	public
	void reloadReadsPersistedContents()
	{
		final
		PropertiesBackedMap first = new PropertiesBackedMap(file);
		first.put("alpha", "one");
		first.put("beta", "two");

		final
		PropertiesBackedMap second = new PropertiesBackedMap(file);

		assertEquals(second.size(), 2);
		assertEquals(second.get("alpha"), "one");
		assertEquals(second.get("beta"), "two");
	}

	@Test
	public
	void removePersistsAndReturnsPriorValue() throws IOException
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);
		map.put("a", "1");
		map.put("b", "2");

		assertEquals(map.remove("a"), "1");
		assertNull(map.remove("does-not-exist"));

		final
		String contents = Files.readString(file, StandardCharsets.UTF_8);

		assertEquals(contents, "b=2\n");
	}

	@Test
	public
	void clearEmptiesFile() throws IOException
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);
		map.put("a", "1");
		map.put("b", "2");
		map.clear();

		assertTrue(map.isEmpty());
		assertEquals(Files.readString(file, StandardCharsets.UTF_8), "");
	}

	@Test
	public
	void putAllPersistsOnce() throws IOException
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);

		final
		Map<String, String> incoming = new HashMap<>();
		incoming.put("z", "zed");
		incoming.put("a", "ay");
		incoming.put("m", "em");

		map.putAll(incoming);

		final
		String contents = Files.readString(file, StandardCharsets.UTF_8);

		assertEquals(contents, "a=ay\nm=em\nz=zed\n");
	}

	@Test
	public
	void redundantPutDoesNotRewriteFile() throws IOException, InterruptedException
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);
		map.put("k", "v");

		final
		long mtimeBefore = Files.getLastModifiedTime(file).toMillis();

		Thread.sleep(15);

		map.put("k", "v");

		final
		long mtimeAfter = Files.getLastModifiedTime(file).toMillis();

		assertEquals(mtimeAfter, mtimeBefore,
			"putting an identical value should not rewrite the backing file");
	}

	@Test
	public
	void keySetValuesEntrySetAreReadOnlyViews()
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);
		map.put("k", "v");

		assertThrows(UnsupportedOperationException.class, () -> map.keySet().remove("k"));
		assertThrows(UnsupportedOperationException.class, () -> map.values().remove("v"));
		assertThrows(UnsupportedOperationException.class, () -> map.entrySet().clear());
	}

	@Test
	public
	void containsSemantics()
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);
		map.put("k", "v");

		assertTrue(map.containsKey("k"));
		assertTrue(map.containsValue("v"));
		assertFalse(map.containsKey("nope"));
		assertFalse(map.containsValue("nope"));
	}

	@Test
	public
	void nullKeyOrValueRejected()
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);

		assertThrows(NullPointerException.class, () -> map.put(null, "v"));
		assertThrows(NullPointerException.class, () -> map.put("k", null));
	}

	@Test
	public
	void specialCharactersRoundTrip()
	{
		final
		PropertiesBackedMap map = new PropertiesBackedMap(file);

		map.put("with=equals", "value with spaces");
		map.put("unicode", "café \u2603");
		map.put("colons:here", "a:b:c");

		final
		PropertiesBackedMap reloaded = new PropertiesBackedMap(file);

		assertEquals(reloaded.get("with=equals"), "value with spaces");
		assertEquals(reloaded.get("unicode"), "café \u2603");
		assertEquals(reloaded.get("colons:here"), "a:b:c");
	}

	@Test
	public
	void constructorAcceptsStringFilename() throws IOException
	{
		final
		String path = file.toString();

		final
		PropertiesBackedMap map = new PropertiesBackedMap(path);
		map.put("k", "v");

		assertTrue(Files.exists(file));
		assertEquals(Files.readString(file, StandardCharsets.UTF_8), "k=v\n");
	}

	@Test
	public
	void constructorCreatesParentDirectoriesOnFirstWrite() throws IOException
	{
		final
		Path nested = tempDir.resolve("a/b/c/data.properties");

		final
		PropertiesBackedMap map = new PropertiesBackedMap(nested);
		map.put("k", "v");

		assertTrue(Files.exists(nested));
	}
}
