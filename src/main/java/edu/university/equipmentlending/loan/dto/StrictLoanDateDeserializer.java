package edu.university.equipmentlending.loan.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Accept only ISO calendar-date strings, including valid calendar days. */
public final class StrictLoanDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(parser, "Expected an ISO calendar date");
        }
        try {
            return LocalDate.parse(parser.getText());
        } catch (DateTimeParseException exception) {
            throw JsonMappingException.from(parser, "Invalid ISO calendar date", exception);
        }
    }
}
