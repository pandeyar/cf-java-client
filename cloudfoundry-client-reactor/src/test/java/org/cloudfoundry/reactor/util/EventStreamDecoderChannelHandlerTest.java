/*
 * Copyright 2013-2017 the original author or authors.
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

package org.cloudfoundry.reactor.util;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import reactor.util.function.Tuples;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public final class EventStreamDecoderChannelHandlerTest {

    private final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

    private final ChannelHandlerContext context = mock(ChannelHandlerContext.class, RETURNS_SMART_NULLS);

    private final EventStreamDecoderChannelHandler handler = new EventStreamDecoderChannelHandler();

    @Test
    public void threeMessagesAllData() throws Exception {
        String message = "data: This is the first message.\n" +
            "\n" +
            "data: This is the second message, it\n" +
            "data: has two lines.\n" +
            "\n" +
            "data: This is the third message.\n" +
            "\n";

        this.handler.channelRead(this.context, getMessage(message));

        verify(this.context, times(6)).fireChannelRead(this.captor.capture());
        assertThat(this.captor.getAllValues()).containsExactly(
            Tuples.of("data", "This is the first message."),
            EventStreamDecoderChannelHandler.DELIMITER,
            Tuples.of("data", "This is the second message, it"),
            Tuples.of("data", "has two lines."),
            EventStreamDecoderChannelHandler.DELIMITER,
            Tuples.of("data", "This is the third message.")
        );
    }

    private static DefaultHttpContent getMessage(String message) {
        return new DefaultHttpContent(Unpooled.copiedBuffer(message.toCharArray(), Charset.defaultCharset()));
    }

}
