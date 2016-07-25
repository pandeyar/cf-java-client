/*
 * Copyright 2013-2016 the original author or authors.
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

package org.cloudfoundry.util;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utilities for dealing with IO
 */
public final class IoUtils {

    /**
     * Pump data from a reader to a write
     *
     * @param bufferSize the size of the buffer to use
     * @param read       a {@link Function} that receives a buffer and returns the number of bytes read into the buffer
     * @param write      {@link Consumer} that receives a {@link Tuple2} with the populated a buffer and the number of bytes in the buffer
     */
    public static void pump(int bufferSize, Function<byte[], Integer> read, Consumer<Tuple2<byte[], Integer>> write) {
        byte[] buffer = new byte[bufferSize];

        int length;
        while ((length = read.apply(buffer)) != -1) {
            write.accept(Tuples.of(buffer, length));
        }
    }

}
