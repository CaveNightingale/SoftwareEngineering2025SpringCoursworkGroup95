package io.github.software.coursework.data.schema;

import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;

import java.io.IOException;

/**
 * An entity is a party that participate in trading. It may be a person, a college, a government, a company, or a non-profit organization.
 * @param name The name of the entity
 * @param telephone The telephone number of the entity
 * @param email The email address of the entity
 * @param address The address of the entity
 * @param website The website of the entity
 * @param type The type of the entity
 */
public record Entity(
        String name,
        String telephone,
        String email,
        String address,
        String website,
        Type type
) implements Item {
    /**
     * The type of entity.
     */
    public enum Type {
        /**
         * The type of entity is unknown. (i.e. user did not specify)
         */
        UNKNOWN,
        /**
         * The type of entity is an individual.
         */
        INDIVIDUAL,
        /**
         * The type of entity is a college or university, typically seen in scholarship transactions or tuition payments.
         */
        EDUCATION,
        /**
         * The type of entity is a government, typically seen in tax payments and fines.
         */
        GOVERNMENT,
        /**
         * The type of entity is a company, typically seen in salary payments and item purchases.
         */
        COMMERCIAL,
        /**
         * The type of entity is a non-profit organization, typically seen this in donation transactions.
         */
        NONPROFIT,
    }


    @Override
    public void serialize(Document.Writer writer) throws IOException {
        writer.writeInteger("schema", 1);
        writer.writeString("name", name);
        writer.writeString("telephone", telephone);
        writer.writeString("email", email);
        writer.writeString("address", address);
        writer.writeString("website", website);
        writer.writeString("type", type.name().toLowerCase());
        writer.writeEnd();
    }

    public static Entity deserialize(Document.Reader reader) throws IOException {
        long schema = reader.readInteger("schema");
        if (schema != 1) {
            throw new IOException("Unsupported schema version: " + schema);
        }
        Entity rval = new Entity(
                reader.readString("name"),
                reader.readString("telephone"),
                reader.readString("email"),
                reader.readString("address"),
                reader.readString("website"),
                Type.valueOf(reader.readString("type").toUpperCase())
        );
        reader.readEnd();
        return rval;
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
