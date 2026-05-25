package com.ruoyi.common.core.deserializer;

import java.io.IOException;
import java.util.Set;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

/**
 * 全局字符串反序列化器：自动去除 JSON 请求体（@RequestBody）中所有 String 字段的首尾空格。
 *
 * <p>实现 {@link ContextualDeserializer}，以便在反序列化前拿到字段名，
 * 对密码类等敏感字段跳过 trim（避免误伤含首尾空格的密码）。</p>
 *
 * @author ruoyi
 */
public class TrimStringJsonDeserializer extends JsonDeserializer<String> implements ContextualDeserializer
{
    /** 跳过 trim 的字段名（密码类，首尾空格可能是有效内容） */
    private static final Set<String> SKIP_FIELDS = Set.of(
            "password", "oldPassword", "newPassword", "confirmPassword");

    /** 当前字段是否跳过 trim */
    private final boolean skip;

    public TrimStringJsonDeserializer()
    {
        this(false);
    }

    private TrimStringJsonDeserializer(boolean skip)
    {
        this.skip = skip;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        String value = p.getValueAsString();
        if (value == null || skip)
        {
            return value;
        }
        return value.trim();
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
    {
        if (property != null && SKIP_FIELDS.contains(property.getName()))
        {
            return new TrimStringJsonDeserializer(true);
        }
        return this;
    }
}
