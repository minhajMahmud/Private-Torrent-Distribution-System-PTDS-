package com.ptds.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FavoriteId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "file_id")
    private UUID fileId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FavoriteId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(fileId, that.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, fileId);
    }
}
