/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.scala;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Simple ClassLoader cache with values kept as a soft references.
 * Closeable classloaders are correctly closed after eviction (may happen in undetermined time, gc dependent).
 */
public class SoftClassLoaderCache<K> {
    private static class WrappedLoader extends URLClassLoader {
        public WrappedLoader(ClassLoader wrapped) {
            super(new URL[0], wrapped);
        }
    }

    private static class ClassLoaderReference extends SoftReference<ClassLoader> {
        private final ClassLoader strongReference;

        public ClassLoader getStrongReference() {
            return strongReference;
        }

        public ClassLoaderReference(ClassLoader strongReference, WrappedLoader loader, ReferenceQueue<ClassLoader> referenceQueue) {
            super(loader, referenceQueue);
            this.strongReference = strongReference;
        }
    }

    private final ReferenceQueue<ClassLoader> referenceQueue;
    private final Cache<K, ClassLoaderReference> cache;


    public SoftClassLoaderCache(int maxSize) {
        referenceQueue = new ReferenceQueue<>();
        cache = CacheBuilder.newBuilder().maximumSize(maxSize).build();
    }

    private void drainQueue() throws Exception {
        ClassLoaderReference poll = (ClassLoaderReference) referenceQueue.poll();
        while (poll != null) {
            ClassLoader strong = poll.getStrongReference();
            if (strong instanceof AutoCloseable) {
                ((AutoCloseable) strong).close();
            }
            poll = (ClassLoaderReference) referenceQueue.poll();
        }
    }

    private void clearEmpty() {
        for (Map.Entry<K, ClassLoaderReference> e: cache.asMap().entrySet()) {
            if (e.getValue().get() == null) {
                cache.invalidate(e.getKey());
            }
        }
    }

    public ClassLoader get(K key, Callable<ClassLoader> loader) throws Exception {
        drainQueue();
        ClassLoaderReference ifPresent = cache.getIfPresent(key);
        ClassLoader cl = ifPresent == null ? null : ifPresent.get();
        if (cl == null) {
            clearEmpty(); // make some space in cache
            ClassLoader strong = loader.call();
            WrappedLoader softening = new WrappedLoader(strong);
            ClassLoaderReference clr = new ClassLoaderReference(strong, softening, referenceQueue);
            cache.put(key, clr);
            return softening;
        } else return cl;
    }

    public void clear() throws Exception {
        for (Map.Entry<K, ClassLoaderReference> e: cache.asMap().entrySet()) {
            e.getValue().clear();
            e.getValue().enqueue();
            drainQueue(); // clear queue
        }
    }
}
