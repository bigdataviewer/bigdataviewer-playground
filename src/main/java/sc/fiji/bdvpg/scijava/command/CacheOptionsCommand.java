/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.bdvpg.scijava.command;

import com.google.gson.Gson;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.widget.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.cache.GlobalCacheBuilder;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu +
	"Set cache options",
	description = "Sets Bdv Playground cache options (needs a restart)",
	initializer = "initialize")
public class CacheOptionsCommand implements BdvPlaygroundActionCommand {

	final public static Logger logger = LoggerFactory.getLogger(
		CacheOptionsCommand.class);

	@Parameter
	PrefService prefs;

	@Parameter(label = "Cache type", choices = { "Caffeine", "LinkedHashMap" },
		persist = false)
	String cache_type;

	@Parameter(label = "Log cache (ms between log), negative to avoid logging",
		persist = false)
	int log_ms;

	@Parameter(label = "Rule: use a ratio of all memory available (%)",
		callback = "useRatio", persist = false)
	int mem_ratio_pc;

	@Parameter(label = "Rule: set a size for cache (Mb)",
		callback = "useMbForCache", persist = false)
	int mem_for_cache_mb;

	@Parameter(label = "Rule: set a size for the rest of the application (Mb)",
		callback = "useMbForElse", persist = false)
	int mem_for_everything_else_mb;

	@Parameter(label = "Reset to default", callback = "reset")
	Button button;

	@Override
	public void run() {
		GlobalCacheBuilder builder = GlobalCacheBuilder.builder();
		switch (cache_type) {
			case GlobalCacheBuilder.CAFFEINE:
				builder.caffeine();
				break;
			case GlobalCacheBuilder.LINKED_HASH_MAP:
				builder.linkedHashMap();
				break;
		}
		if (log_ms > 0) builder.log(log_ms);
		if (mem_for_cache_mb > 0) builder.memoryForCache((long) mem_for_cache_mb *
			1024L * 1024L);
		if (mem_for_everything_else_mb > 0) builder.memoryForEverythingElse(
			(long) mem_for_everything_else_mb * 1024L * 1024L);
		if (mem_ratio_pc > 0) builder.memoryRatioForCache(((double) mem_ratio_pc) /
			100);

		String serializedCacheBuilder = new Gson().toJson(builder,
			GlobalCacheBuilder.class);

		logger.info("Cache builder : " + serializedCacheBuilder);

		prefs.put(SourceAndConverterService.class, "cache.builder",
			serializedCacheBuilder);
	}

	void initialize() {
		Gson gson = new Gson();
		String defaultCacheBuilder = gson.toJson(GlobalCacheBuilder.builder(),
			GlobalCacheBuilder.class);
		String cacheBuilderJson = prefs.get(SourceAndConverterService.class,
			"cache.builder", defaultCacheBuilder);
		try {
			GlobalCacheBuilder builder = gson.fromJson(cacheBuilderJson,
				GlobalCacheBuilder.class);
			setParametersFromBuilder(builder);
		}
		catch (Exception e) {
			reset();
		}
	}

	void setParametersFromBuilder(GlobalCacheBuilder builder) {
		cache_type = builder.getCacheType();
		mem_ratio_pc = builder.getMemoryRatioForCache() > 0 ? (int) (builder
			.getMemoryRatioForCache() * 100) : -1;
		mem_for_cache_mb = builder.getMemoryInBytesForCache() > 0 ? (int) (builder
			.getMemoryInBytesForCache() / (1024 * 1024)) : -1;
		mem_for_everything_else_mb = builder.getMemoryInBytesForEverythingElse() > 0
			? (int) (builder.getMemoryInBytesForEverythingElse() / (1024 * 1024))
			: -1;
		if (builder.getLog()) {
			log_ms = builder.getMsBetweenLog();
		}
		else {
			log_ms = -1;
		}
	}

	void reset() {
		setParametersFromBuilder(GlobalCacheBuilder.builder());
	}

	void useRatio() {
		mem_for_cache_mb = -1;
		mem_for_everything_else_mb = -1;
	}

	void useMbForCache() {
		mem_ratio_pc = -1;
		mem_for_everything_else_mb = -1;
	}

	void useMbForElse() {
		mem_ratio_pc = -1;
		mem_for_cache_mb = -1;
	}
}
