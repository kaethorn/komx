package de.wasenweg.komix.comics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Publisher {

    private String id;

    private String publisher;
    private List<Series> series;
}
