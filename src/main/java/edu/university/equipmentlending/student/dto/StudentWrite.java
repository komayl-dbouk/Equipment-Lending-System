package edu.university.equipmentlending.student.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;

/** Raw limits run before the service normalizes and checks email syntax. */
public record StudentWrite(
        @JsonDeserialize(using = TextDeserializer.class) @NotBlank @Size(max = 120) String name,
        @JsonDeserialize(using = TextDeserializer.class)
        @NotBlank @Size(max = 254) @Email(groups = Normalized.class) String email) {
    public interface Normalized { }

    /** Keep the published string types without changing Jackson behavior for other features. */
    public static final class TextDeserializer extends StdDeserializer<String> {
        public TextDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.hasToken(JsonToken.VALUE_STRING)) return parser.getText();
            return (String) context.handleUnexpectedToken(String.class, parser);
        }
    }
}
