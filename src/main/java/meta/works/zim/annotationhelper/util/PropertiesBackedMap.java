package meta.works.zim.annotationhelper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A {@code Map<String,String>} whose contents are persisted to a properties file on disk.
 *
 * <p>Every mutation (put, remove, clear, putAll, and mutations via the collection views)
 * immediately rewrites the backing file. If the file does not exist at construction time,
 * the map starts empty and a warning is logged; the file will be created on first mutation.
 *
 * <p>On-disk format is standard {@link Properties} {@code key=value}, one entry per line,
 * sorted by key for stable diffs. The timestamp comment normally emitted by
 * {@link Properties#store} is stripped.
 *
 * <p>Not thread-safe.
 */
public
class PropertiesBackedMap implements Map<String, String>
{
	private static final
	Logger log = LoggerFactory.getLogger(PropertiesBackedMap.class);

	private final
	Path file;

	private final
	Map<String, String> delegate = new LinkedHashMap<>();

	public
	PropertiesBackedMap(String filename)
	{
		this(new File(filename).toPath());
	}

	public
	PropertiesBackedMap(File file)
	{
		this(file.toPath());
	}

	public
	PropertiesBackedMap(Path file)
	{
		this.file = file;
		load();
	}

	private
	void load()
	{
		if (!Files.exists(file))
		{
			log.warn("backing file does not exist, starting empty: {}", file);
			return;
		}

		final
		Properties props = new Properties();

		try (final var in = Files.newBufferedReader(file, StandardCharsets.UTF_8))
		{
			props.load(in);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException("failed to read backing file: " + file, e);
		}

		for (final String name : props.stringPropertyNames())
		{
			delegate.put(name, props.getProperty(name));
		}
	}

	private
	void save()
	{
		final
		Properties props = new Properties();

		for (final var e : delegate.entrySet())
		{
			props.setProperty(e.getKey(), e.getValue());
		}

		final
		StringWriter buffer = new StringWriter();

		try
		{
			props.store(buffer, null);
		}
		catch (IOException e)
		{
			// StringWriter never throws; keep for the API contract.
			throw new UncheckedIOException(e);
		}

		final
		String cleaned = stripCommentsAndSort(buffer.toString());

		try
		{
			final var parent = file.getParent();
			if (parent != null && !Files.exists(parent))
			{
				Files.createDirectories(parent);
			}
			Files.writeString(file, cleaned, StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException("failed to write backing file: " + file, e);
		}
	}

	/**
	 * Properties.store() writes a "#<date>" comment header and iterates entries in
	 * Hashtable (unordered) order. Drop comments and sort remaining lines so the file
	 * is deterministic and diff-friendly.
	 *
	 * <p>Caveat (accepted by design): values containing literal newlines would break
	 * this line-based sort. Properties escaping produces "\n" (two chars) for embedded
	 * newlines, so this is only a concern for hand-edited files.
	 */
	private static
	String stripCommentsAndSort(String stored)
	{
		final
		List<String> lines = new ArrayList<>();

		try (final var reader = new BufferedReader(new StringReader(stored)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("#") || line.startsWith("!"))
				{
					continue;
				}
				if (line.isEmpty())
				{
					continue;
				}
				lines.add(line);
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}

		Collections.sort(lines);

		final
		StringBuilder out = new StringBuilder(stored.length());

		for (final String line : lines)
		{
			out.append(line).append('\n');
		}

		return out.toString();
	}

	// -------- Map<String,String> implementation (delegating + persisting) --------

	@Override
	public
	int size()
	{
		return delegate.size();
	}

	@Override
	public
	boolean isEmpty()
	{
		return delegate.isEmpty();
	}

	@Override
	public
	boolean containsKey(Object key)
	{
		return delegate.containsKey(key);
	}

	@Override
	public
	boolean containsValue(Object value)
	{
		return delegate.containsValue(value);
	}

	@Override
	public
	String get(Object key)
	{
		return delegate.get(key);
	}

	@Override
	public
	String put(String key, String value)
	{
		if (key == null || value == null)
		{
			throw new NullPointerException("PropertiesBackedMap does not permit null keys or values");
		}

		final
		String prior = delegate.put(key, value);

		if (prior == null || !prior.equals(value))
		{
			save();
		}

		return prior;
	}

	@Override
	public
	String remove(Object key)
	{
		if (!delegate.containsKey(key))
		{
			return null;
		}

		final
		String prior = delegate.remove(key);

		save();
		return prior;
	}

	@Override
	public
	void putAll(Map<? extends String, ? extends String> m)
	{
		boolean changed = false;

		for (final var e : m.entrySet())
		{
			if (e.getKey() == null || e.getValue() == null)
			{
				throw new NullPointerException("PropertiesBackedMap does not permit null keys or values");
			}
			final
			String prior = delegate.put(e.getKey(), e.getValue());
			if (prior == null || !prior.equals(e.getValue()))
			{
				changed = true;
			}
		}

		if (changed)
		{
			save();
		}
	}

	@Override
	public
	void clear()
	{
		if (delegate.isEmpty())
		{
			return;
		}
		delegate.clear();
		save();
	}

	@Override
	public
	Set<String> keySet()
	{
		// Return an unmodifiable view; use remove(Object) on the map itself for mutations.
		return java.util.Collections.unmodifiableSet(delegate.keySet());
	}

	@Override
	public
	Collection<String> values()
	{
		return java.util.Collections.unmodifiableCollection(delegate.values());
	}

	@Override
	public
	Set<Entry<String, String>> entrySet()
	{
		return java.util.Collections.unmodifiableSet(delegate.entrySet());
	}

	@Override
	public
	boolean equals(Object o)
	{
		return delegate.equals(o);
	}

	@Override
	public
	int hashCode()
	{
		return delegate.hashCode();
	}

	@Override
	public
	String toString()
	{
		return getClass().getSimpleName() + "(" + file + ")=" + delegate;
	}
}
