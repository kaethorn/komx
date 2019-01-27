package de.wasenweg.komix.preferences;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Document
public class Preference {

    @Id
    private String id;

    @NonNull
    private String key;
    @NonNull
    private String name;
    @NonNull
    private String value;
    @NonNull
    private String comment;

    @Override
    public String toString() {
        return String.format(
                "Preference[id=%s, key='%s', value='%s']",
                id, key, value);
    }
}
