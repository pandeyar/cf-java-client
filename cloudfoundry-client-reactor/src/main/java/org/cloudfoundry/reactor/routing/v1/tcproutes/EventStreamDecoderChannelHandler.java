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

package org.cloudfoundry.reactor.routing.v1.tcproutes;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpContent;
import reactor.util.function.Tuples;

import java.nio.charset.Charset;

final class EventStreamDecoderChannelHandler extends ChannelInboundHandlerAdapter {

    static final String DELIMITER = "DELIMITER";

    private static final char[] COLON = new char[]{':', ' '};

    private static final char[] CRLF = new char[]{'\r', '\n'};

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private String characters;

    private int colonPosition;

    private int crlfPosition;

    private int nameEndPosition;

    private int nameStartPosition;

    private int position;

    private Stage stage;

    private int valueEndPosition;

    private int valueStartPosition;

    EventStreamDecoderChannelHandler() {
        reset();
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (!(message instanceof DefaultHttpContent)) {
            super.channelRead(context, message);
            return;
        }

        ByteBuf byteBuf = ((DefaultHttpContent) message).content();
        this.characters = this.characters != null ? this.characters + byteBuf.toString(UTF8) : byteBuf.toString(UTF8);

        while (this.position < this.characters.length()) {
            char c = this.characters.charAt(this.position);

            switch (this.stage) {
                case COLON:
                    colon(c);
                    break;
                case CRLF:
                    crlf(context, c);
                    break;
                case NAME:
                    name(c);
                    break;
                case VALUE:
                    value(c);
                    break;
            }
        }

        if (Stage.NAME == this.stage) {
            reset();
        }
    }

    private void colon(char c) {
        if (this.colonPosition < COLON.length) {
            if (COLON[this.colonPosition] == c) {
                this.colonPosition++;
                this.position++;
            } else {
                this.valueStartPosition = this.position;
                this.stage = Stage.VALUE;
                this.position++;
            }
        } else {
            this.valueStartPosition = this.position;
            this.stage = Stage.VALUE;
            this.position++;
        }
    }

    private void crlf(ChannelHandlerContext context, char c) {
        if (this.crlfPosition < CRLF.length) {
            if (CRLF[this.crlfPosition] == c) {
                this.crlfPosition++;
                this.position++;
            } else {
                send(context);
            }
        } else {
            send(context);
        }
    }

    private void name(char c) {
        if (this.nameStartPosition == this.position) {
            if (CRLF[0] == c) {
                this.nameEndPosition = this.position;
                this.valueStartPosition = this.position;
                this.valueEndPosition = this.position;
                this.stage = Stage.CRLF;
                this.crlfPosition = 1;
                this.position++;
            } else if (CRLF[1] == c) {
                this.nameEndPosition = this.position;
                this.valueStartPosition = this.position;
                this.valueEndPosition = this.position;
                this.stage = Stage.CRLF;
                this.crlfPosition = 2;
                this.position++;
            } else {
                this.position++;
            }
        } else if (COLON[0] == c) {
            this.nameEndPosition = this.position;
            this.stage = Stage.COLON;
            this.colonPosition = 1;
            this.position++;
        } else {
            this.position++;
        }
    }

    private void reset() {
        this.characters = null;
        this.nameStartPosition = 0;
        this.position = 0;
        this.stage = Stage.NAME;
    }

    private void send(ChannelHandlerContext context) {
        if (this.nameStartPosition == this.valueEndPosition) {
            context.fireChannelRead(DELIMITER);
        } else {
            String name = this.characters.substring(this.nameStartPosition, this.nameEndPosition);
            String value = this.characters.substring(this.valueStartPosition, this.valueEndPosition);
            context.fireChannelRead(Tuples.of(name, value));
        }

        this.nameStartPosition = this.position;
        this.valueEndPosition = this.position;
        this.stage = Stage.NAME;
    }

    private void value(char c) {
        if (CRLF[0] == c) {
            this.valueEndPosition = this.position;
            this.stage = Stage.CRLF;
            this.crlfPosition = 1;
            this.position++;
        } else if (CRLF[1] == c) {
            this.valueEndPosition = this.position;
            this.stage = Stage.CRLF;
            this.crlfPosition = 2;
            this.position++;
        } else {
            this.position++;
        }
    }

    private enum Stage {

        COLON,

        CRLF,

        NAME,

        VALUE

    }

}
