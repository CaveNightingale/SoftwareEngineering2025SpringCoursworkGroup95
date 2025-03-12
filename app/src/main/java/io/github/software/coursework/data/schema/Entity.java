package io.github.software.coursework.data.schema;

import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;

public record Entity(
        String name,
        String telephone,
        String email,
        String address,
        String website,
        Type type
) implements Item<Entity> {
    public enum Type {
        UNKNOWN,
        INDIVIDUAL,
        EDUCATION,
        GOVERNMENT,
        COMMERCIAL,
        NONPROFIT,
    }


    @Override
    public void serialize(Document.Writer writer) {
        writer.writeString("name", name);
        writer.writeString("telephone", telephone);
        writer.writeString("email", email);
        writer.writeString("address", address);
        writer.writeString("website", website);
        writer.writeString("type", type.name().toLowerCase());
        writer.writeEnd();
    }

    public static Entity deserialize(Document.Reader reader) {
        return new Entity(
                reader.readString("name"),
                reader.readString("telephone"),
                reader.readString("email"),
                reader.readString("address"),
                reader.readString("website"),
                Type.valueOf(reader.readString("type").toUpperCase())
        );
    }

    public Entity withName(String name) {
        return new Entity(name, telephone, email, address, website, type);
    }

    public Entity withTelephone(String telephone) {
        return new Entity(name, telephone, email, address, website, type);
    }

    public Entity withEmail(String email) {
        return new Entity(name, telephone, email, address, website, type);
    }

    public Entity withAddress(String address) {
        return new Entity(name, telephone, email, address, website, type);
    }

    public Entity withWebsite(String website) {
        return new Entity(name, telephone, email, address, website, type);
    }

    public Entity withType(Type type) {
        return new Entity(name, telephone, email, address, website, type);
    }
}
