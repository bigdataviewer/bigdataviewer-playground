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

package sc.fiji.bdvpg.scijava.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.scijava.cache.CacheService;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** {@link CacheService} implementation wrapping a guava {@link Cache}.
 *
 * Modifications by Nicolas Chiaruttini :
 * - using weak keys
 * - logs content
 * - cache can be retrieved
 * @author Mark Hiner, Nicolas Chiaruttini
 * This service is used to link the SpimData Object to its BdvHandle
 */

@Plugin(type = Service.class)
public class GuavaWeakCacheService extends AbstractService implements CacheService {

    private Cache<Object, Object> cache;

    public Cache<Object, Object> getCache() {
        return cache;
    }

    public void logCache(Consumer<String> logger) {
        cache.asMap().forEach((key, value) -> {
            logger.accept(key.getClass().getSimpleName() + ":" + key);
            logger.accept("\t" + value.getClass().getSimpleName() + ":" + value);

        });
    }

    @Override
    public void initialize() {
        cache = CacheBuilder.newBuilder().weakKeys().build();
    }

    @Override
    public void put(final Object key, final Object value) {
        cache.put(key, value);
    }

    @Override
    public Object get(final Object key) {
        return cache.getIfPresent(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V get(final Object key, final Callable<V> valueLoader)
            throws ExecutionException
    {
        return (V) cache.get(key, valueLoader);
    }
}
