/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.cache;

import bdv.cache.SharedQueue;

/**
 * Holder for a single, process-wide {@link SharedQueue} shared by all lazily
 * computed volatile sources created by BigDataViewer-Playground (and, ideally,
 * by downstream libraries building lazy {@link bdv.viewer.Source}s on top of
 * it).
 * <p>
 * <b>Why a single queue?</b> A {@link SharedQueue} owns a fixed pool of daemon
 * fetcher threads that are started immediately in its constructor and never
 * reclaimed unless {@link SharedQueue#shutdown()} is called. Creating one queue
 * per source (the historical behaviour of {@code SourceResampler} and
 * {@code WrapVolatileSource}) therefore spawned {@code availableProcessors-1}
 * threads per source and leaked them. Sharing one queue keeps the fetcher
 * thread count bounded regardless of how many sources are created, which is
 * exactly what the class name ("shared") implies.
 * <p>
 * <b>Is a single queue safe for nested lazy pipelines?</b> Yes. A fetcher
 * thread that picks up a fetch task loads it <em>synchronously</em>
 * (BLOCKING strategy) and recurses through the BLOCKING reads of all upstream
 * sources on that same thread; it never re-enqueues-and-waits on the queue. The
 * only way to provoke a thread-starvation deadlock is to write a loader that,
 * while running on the queue, blocks waiting for another result that itself
 * needs a free fetcher thread of the same queue. Don't do that: inside a cell
 * loader always read upstream data through the blocking (non-volatile)
 * accessor. If a stage genuinely must wait on asynchronous results, give it its
 * own dedicated {@link SharedQueue} instead of this shared one.
 * <p>
 * <b>numPriorities.</b> The queue keeps one FIFO/LIFO deque per priority level
 * (0 = highest); BigDataViewer's renderer requests data with a priority equal
 * to the mipmap/resolution level being drawn, so coarse levels load before fine
 * ones. The default here is deliberately generous so that realistic multi
 * resolution pyramids are not collapsed onto a single priority (the old
 * {@code new SharedQueue(n)} used a single priority). Priorities above the
 * configured maximum are clamped, so over-provisioning costs only a few empty
 * deques.
 * <p>
 * The instance is created lazily on first {@link #getInstance()}. Applications
 * that want a different configuration should call {@link #setInstance} or
 * {@link #set} <em>once, at startup, before any lazy source is created</em>;
 * replacing the queue shuts the previous one down, which would break sources
 * already fetching through it.
 *
 * @author Nicolas Chiaruttini
 */
public class GlobalSharedQueue {

	private GlobalSharedQueue() {}

	/** Default number of fetcher threads of the shared queue. */
	public static int DEFAULT_NUM_FETCHER_THREADS = Math.max(Runtime.getRuntime()
		.availableProcessors() - 1, 1);

	/**
	 * Default number of priority levels of the shared queue. Generous on
	 * purpose: it only needs to cover the deepest mipmap pyramid in use, and
	 * extra levels are cheap (one empty deque each).
	 */
	public static int DEFAULT_NUM_PRIORITIES = 16;

	private static volatile SharedQueue instance;

	/**
	 * @return the shared queue, creating a default one on first access.
	 */
	public static SharedQueue getInstance() {
		SharedQueue local = instance;
		if (local == null) {
			synchronized (GlobalSharedQueue.class) {
				local = instance;
				if (local == null) {
					local = new SharedQueue(DEFAULT_NUM_FETCHER_THREADS,
						DEFAULT_NUM_PRIORITIES);
					instance = local;
				}
			}
		}
		return local;
	}

	/**
	 * Installs a pre-configured shared queue, shutting down the previously held
	 * one (if any). Call once at startup, before any lazy source is created.
	 *
	 * @param queue the queue to share globally
	 */
	public static synchronized void setInstance(SharedQueue queue) {
		if (queue == null) {
			throw new IllegalArgumentException(
				"The global shared queue cannot be null.");
		}
		SharedQueue old = instance;
		instance = queue;
		if (old != null && old != queue) {
			old.shutdown();
		}
	}

	/**
	 * Convenience for {@link #setInstance(SharedQueue)} that builds the queue
	 * from a thread and priority count.
	 *
	 * @param numFetcherThreads number of fetcher (daemon) threads
	 * @param numPriorities number of priority levels (0 = highest)
	 */
	public static synchronized void set(int numFetcherThreads, int numPriorities) {
		setInstance(new SharedQueue(numFetcherThreads, numPriorities));
	}
}
