package top.mothership.cb3.pojo.old;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;

public class LocalDateCustomSerializer extends JsonSerializer<LocalDate> {
    @Override
    public void serialize(LocalDate date, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("year", date.getYear());
        gen.writeNumberField("month", date.getMonthValue());
        gen.writeNumberField("day", date.getDayOfMonth());
        gen.writeEndObject();
    }
}