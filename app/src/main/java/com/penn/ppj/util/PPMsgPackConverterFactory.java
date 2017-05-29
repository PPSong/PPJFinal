package com.penn.ppj.util;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by penn on 06/04/2017.
 */

public class PPMsgPackConverterFactory extends Converter.Factory {
    private static final MediaType MEDIA_TYPE = MediaType.parse("text/plain");

    private MessagePack messagePack = null;


    public static PPMsgPackConverterFactory create() {
        return new PPMsgPackConverterFactory();
    }

    private PPMsgPackConverterFactory() {
    }

    public static PPMsgPackConverterFactory create(MessagePack messagePack) {
        return new PPMsgPackConverterFactory(messagePack);
    }

    private PPMsgPackConverterFactory(MessagePack messagePack) {
        if (messagePack == null) throw new NullPointerException("messagePack == null");
        this.messagePack = messagePack;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        if (String.class.equals(type)) {
            return new Converter<ResponseBody, String>() {
                @Override public String convert(ResponseBody value) throws IOException {
                    final InputStream in = value.byteStream();
                    MessageUnpacker unPacker= MessagePack.newDefaultUnpacker(in);

                    return unPacker.unpackValue().toString();
                }
            };
        }
        return null;
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,
                                                          Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        if (String.class.equals(type)) {
            return new Converter<String, RequestBody>() {
                @Override public RequestBody convert(String value) throws IOException {
                    return RequestBody.create(MEDIA_TYPE, value);
                }
            };
        }
        return null;
    }
}