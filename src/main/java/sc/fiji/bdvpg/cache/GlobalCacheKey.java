
package sc.fiji.bdvpg.cache;

import java.lang.ref.WeakReference;

public class GlobalCacheKey {

	private final WeakReference<Object> source;

	private final int timepoint;

	private final int level;

	public final WeakReference<Object> key;

	public GlobalCacheKey(final Object source, final int timepoint,
		final int level, final Object key)
	{
		this.source = new WeakReference<>(source);
		this.timepoint = timepoint;
		this.level = level;
		this.key = new WeakReference<>(key);

		int value = source.hashCode();
		value = 31 * value + level;
		value = 31 * value + key.hashCode();
		value = 31 * value + timepoint;
		hashcode = value;
	}

	public boolean partialEquals(final Object source, final int timepoint,
		final int level)
	{
		if (this.source.get() == null) return false;
		if (key.get() == null) return false;

		return (this.source.get() == source) && (this.timepoint == timepoint) &&
			(this.level == level);
	}

	@Override
	public boolean equals(final Object other) {
		if (source.get() == null) return false;
		if (key.get() == null) return false;

		if (this == other) return true;
		if (!(other instanceof GlobalCacheKey)) return false;
		final GlobalCacheKey that = (GlobalCacheKey) other;

		return (this.source.get() == that.source.get()) &&
			(this.timepoint == that.timepoint) && (this.level == that.level) &&
			(this.key.get().equals(that.key.get()));
	}

	final int hashcode;

	@Override
	public int hashCode() {
		return hashcode;
	}
}
