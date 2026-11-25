package edu.university.equipmentlending.shared.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public final class StrictTextDeserializer extends StdDeserializer<String> {
    public StrictTextDeserializer() { super(String.class); }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_STRING)) return parser.getText();
        return (String) context.handleUnexpectedToken(String.class, parser);
    }
}
