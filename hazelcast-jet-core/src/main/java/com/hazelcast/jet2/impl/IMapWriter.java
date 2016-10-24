/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet2.impl;

import com.hazelcast.core.IMap;
import com.hazelcast.jet2.Inbox;
import com.hazelcast.jet2.Outbox;
import com.hazelcast.jet2.Processor;
import com.hazelcast.jet2.ProcessorContext;
import com.hazelcast.jet2.ProcessorSupplier;

import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IMapWriter extends AbstractProcessor {

    private final IMap map;
    private final ArrayMap buffer = new ArrayMap();

    public IMapWriter(IMap map) {
        this.map = map;
    }

    @Override
    public void init(@Nonnull Outbox outbox) {
        super.init(outbox);
    }

    @Override
    public void process(int ordinal, Inbox inbox) {
        inbox.drainTo(buffer.entries);
        flush();
    }

    @Override
    public boolean complete() {
        flush();
        return true;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    private void flush() {
        //noinspection unchecked
        map.putAll(buffer);
        buffer.clear();
    }

    public static ProcessorSupplier supplier(String mapName) {
        return new Supplier(mapName);
    }

    private static class Supplier implements ProcessorSupplier {

        static final long serialVersionUID = 1L;

        private final String name;
        private transient IMap map;

        public Supplier(String name) {
            this.name = name;
        }

        @Override
        public void init(ProcessorContext context) {
            map = context.getHazelcastInstance().getMap(name);
        }

        @Override
        public Processor get() {
            return new IMapWriter(map);
        }
    }

    private static class ArrayMap extends AbstractMap {

        private final List<Entry> entries;
        private final ArraySet set = new ArraySet();

        public ArrayMap() {
            entries = new ArrayList<>();
        }

        @Override
        @Nonnull
        public Set<Entry> entrySet() {
            return set;
        }

        public void add(Map.Entry entry) {
            entries.add(entry);
        }

        private class ArraySet extends AbstractSet<Map.Entry> {
            @Override
            @Nonnull
            public Iterator<Entry> iterator() {
                return entries.iterator();
            }

            @Override
            public int size() {
                return entries.size();
            }
        }
    }
}
